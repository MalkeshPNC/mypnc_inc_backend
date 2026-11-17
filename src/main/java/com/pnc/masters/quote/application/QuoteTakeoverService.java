package com.pnc.masters.quote.application;

import com.pnc.masters.quote.api.QuoteLockNotice;
import com.pnc.masters.quote.api.QuoteLockResponse;
import com.pnc.masters.quote.api.QuoteValidationException;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Asks the person editing a quote before an admin takes the lock.
 * One request per quote, kept in memory because it only lives for the answer
 * and the short save window.
 */
@Service
public class QuoteTakeoverService {

    static final Duration SAVE_WINDOW = Duration.ofSeconds(15);
    static final Duration FLUSH_GRACE = Duration.ofSeconds(5);

    private final QuoteLockService lockService;
    private final AppUserRepository users;
    private final QuoteLockNotifier notifier;
    private final Clock clock;
    private final ConcurrentHashMap<Long, Request> pending = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "quote-takeover");
        thread.setDaemon(true);
        return thread;
    });

    public QuoteTakeoverService(QuoteLockService lockService,
                                AppUserRepository users,
                                QuoteLockNotifier notifier,
                                Clock clock) {
        this.lockService = lockService;
        this.users = users;
        this.notifier = notifier;
        this.clock = clock;
    }

    @PostConstruct
    void start() {
        scheduler.scheduleWithFixedDelay(this::tick, 500, 500, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void stop() {
        scheduler.shutdownNow();
    }

    public void request(Long qid, Long adminId) {
        QuoteLockResponse lock = lockService.find(qid, adminId);
        if (lock == null) {
            throw new QuoteValidationException("Nobody is editing this quote.");
        }
        if (lock.heldByCurrentUser()) {
            throw new QuoteValidationException("You already have this quote.");
        }
        if (pending.containsKey(qid)) {
            throw new QuoteValidationException("A takeover is already in progress.");
        }
        if (!notifier.isConnected(lock.lockedByUserId())) {
            throw new QuoteValidationException(
                    "That person is not connected, so the takeover request could not be delivered.");
        }
        Request request = new Request(
                qid,
                lock.lockedByUserId(),
                lock.lockedByName(),
                adminId,
                nameOf(adminId),
                Phase.REQUESTED,
                null,
                null
        );
        pending.put(qid, request);
        send(request.holderId, notice(request, "requested", null));
    }

    public void respond(Long qid, Long userId, boolean allow) {
        Request request = pending.get(qid);
        if (request == null || request.phase != Phase.REQUESTED || !request.holderId.equals(userId)) {
            throw new QuoteValidationException("There is no takeover request to answer.");
        }
        if (!allow) {
            if (pending.remove(qid, request)) {
                send(request.adminId, notice(request, "denied", null));
            }
            return;
        }
        request.phase = Phase.GRANTED;
        request.transferAt = Instant.now(clock).plus(SAVE_WINDOW);
        QuoteLockNotice granted = notice(request, "granted", request.transferAt);
        send(request.holderId, granted);
        send(request.adminId, granted);
    }

    /** The editor finished the save that runs when the countdown ends. */
    public void saved(Long qid, Long userId) {
        Request request = pending.get(qid);
        if (request == null || request.phase != Phase.FLUSHING || !request.holderId.equals(userId)) {
            throw new QuoteValidationException("There is no takeover save to confirm.");
        }
        complete(request);
    }

    void tick() {
        Instant now = Instant.now(clock);
        for (Request request : List.copyOf(pending.values())) {
            if (!pending.containsKey(request.qid)) {
                continue;
            }
            if (!holderStillHasLock(request)) {
                if (pending.remove(request.qid, request)) {
                    send(request.adminId, notice(request, "released", null));
                }
                continue;
            }
            if (request.phase == Phase.GRANTED && request.transferAt != null && !now.isBefore(request.transferAt)) {
                request.phase = Phase.FLUSHING;
                request.flushDeadline = now.plus(FLUSH_GRACE);
                send(request.holderId, notice(request, "flush", request.transferAt));
            }
            if (request.phase == Phase.FLUSHING
                    && request.flushDeadline != null
                    && !now.isBefore(request.flushDeadline)) {
                complete(request);
            }
        }
    }

    private void complete(Request request) {
        if (!pending.remove(request.qid, request)) {
            return;
        }
        lockService.reassign(request.qid, request.adminId);
        QuoteLockNotice completed = notice(request, "completed", request.transferAt);
        send(request.holderId, completed);
        send(request.adminId, completed);
    }

    private boolean holderStillHasLock(Request request) {
        QuoteLockResponse lock = lockService.find(request.qid, request.holderId);
        return lock != null && request.holderId.equals(lock.lockedByUserId());
    }

    private void send(Long userId, QuoteLockNotice notice) {
        notifier.send(userId, notice);
    }

    private QuoteLockNotice notice(Request request, String type, Instant transferAt) {
        return new QuoteLockNotice(
                type,
                request.qid,
                request.holderName,
                request.adminName,
                request.adminId,
                transferAt
        );
    }

    private String nameOf(Long userId) {
        return users.findById(userId)
                .map(AppUser::getDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .orElse("Unknown");
    }

    private enum Phase {
        REQUESTED,
        GRANTED,
        FLUSHING
    }

    private static final class Request {
        private final Long qid;
        private final Long holderId;
        private final String holderName;
        private final Long adminId;
        private final String adminName;
        private Phase phase;
        private Instant transferAt;
        private Instant flushDeadline;

        private Request(Long qid,
                        Long holderId,
                        String holderName,
                        Long adminId,
                        String adminName,
                        Phase phase,
                        Instant transferAt,
                        Instant flushDeadline) {
            this.qid = qid;
            this.holderId = holderId;
            this.holderName = holderName;
            this.adminId = adminId;
            this.adminName = adminName;
            this.phase = phase;
            this.transferAt = transferAt;
            this.flushDeadline = flushDeadline;
        }
    }
}
