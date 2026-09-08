package com.pnc.masters.quote.application;

import com.pnc.masters.quote.QuoteLock;
import com.pnc.masters.quote.QuoteLockProperties;
import com.pnc.masters.quote.QuoteLockRepository;
import com.pnc.masters.quote.api.QuoteLockResponse;
import com.pnc.masters.quote.api.QuoteLockedException;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Single-editor lock for quotes. Expiry is evaluated lazily on every read and
 * write, the way password reset tokens are checked at use time, so no scheduled
 * cleanup job is needed and a crashed browser can never wedge a quote.
 */
@Service
@Transactional
public class QuoteLockService {

    private final QuoteLockRepository lockRepository;
    private final AppUserRepository userRepository;
    private final QuoteLockProperties properties;

    public QuoteLockService(QuoteLockRepository lockRepository,
                            AppUserRepository userRepository,
                            QuoteLockProperties properties) {
        this.lockRepository = lockRepository;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    /**
     * Claims the lock, or renews it when the caller already holds it. Doubles as
     * the heartbeat endpoint. Returns the current holder either way, so a caller
     * who lost the race still learns who has it.
     */
    public QuoteLockResponse acquireOrRenew(Long qid, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        QuoteLock lock = lockRepository.findByQid(qid).orElse(null);

        if (lock != null && !isExpired(lock, now) && !lock.getLockedByUserId().equals(userId)) {
            return toResponse(lock, userId);
        }
        if (lock == null) {
            lock = new QuoteLock();
            lock.setQid(qid);
        }
        if (!userId.equals(lock.getLockedByUserId())) {
            lock.setLockedByUserId(userId);
            lock.setAcquiredAt(now);
        }
        lock.setLastSeenAt(now);
        return toResponse(lockRepository.save(lock), userId);
    }

    /**
     * Guards a save. Throws when the live lock belongs to somebody else, or when
     * the caller's own lock has gone idle and expired.
     */
    public void requireHeldBy(Long qid, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        QuoteLock lock = lockRepository.findByQid(qid).orElse(null);
        if (lock == null || isExpired(lock, now)) {
            throw new QuoteLockedException(qid, null, null);
        }
        if (!lock.getLockedByUserId().equals(userId)) {
            throw new QuoteLockedException(qid, lock.getLockedByUserId(), displayName(lock.getLockedByUserId()));
        }
        lock.setLastSeenAt(now);
        lockRepository.save(lock);
    }

    /** Releases the lock only if the caller holds it. */
    public void release(Long qid, Long userId) {
        lockRepository.findByQid(qid)
                .filter(lock -> lock.getLockedByUserId().equals(userId))
                .ifPresent(lockRepository::delete);
    }

    /** Drops the lock whoever holds it. Admin only, and used on soft delete. */
    public void forceRelease(Long qid) {
        lockRepository.findByQid(qid).ifPresent(lockRepository::delete);
    }

    @Transactional(readOnly = true)
    public QuoteLockResponse find(Long qid, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return lockRepository.findByQid(qid)
                .filter(lock -> !isExpired(lock, now))
                .map(lock -> toResponse(lock, userId))
                .orElse(null);
    }

    /** One query for the whole list page rather than one per row. */
    @Transactional(readOnly = true)
    public Map<Long, QuoteLockResponse> findAll(Collection<Long> qids, Long userId) {
        if (qids.isEmpty()) {
            return Map.of();
        }
        LocalDateTime now = LocalDateTime.now();
        List<QuoteLock> locks = lockRepository.findAllByQidIn(qids).stream()
                .filter(lock -> !isExpired(lock, now))
                .toList();
        Map<Long, QuoteLockResponse> byQid = new HashMap<>();
        for (QuoteLock lock : locks) {
            byQid.put(lock.getQid(), toResponse(lock, userId));
        }
        return byQid;
    }

    private boolean isExpired(QuoteLock lock, LocalDateTime now) {
        return lock.getLastSeenAt().plusNanos(properties.getIdleTimeoutMs() * 1_000_000L).isBefore(now);
    }

    private QuoteLockResponse toResponse(QuoteLock lock, Long currentUserId) {
        return new QuoteLockResponse(
                lock.getQid(),
                lock.getLockedByUserId(),
                displayName(lock.getLockedByUserId()),
                lock.getLockedByUserId().equals(currentUserId),
                lock.getAcquiredAt(),
                lock.getLastSeenAt().plusNanos(properties.getIdleTimeoutMs() * 1_000_000L)
        );
    }

    private String displayName(Long userId) {
        return Optional.ofNullable(userId)
                .flatMap(userRepository::findById)
                .map(AppUser::getDisplayName)
                .orElse("Unknown");
    }
}
