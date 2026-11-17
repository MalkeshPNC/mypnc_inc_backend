package com.pnc.masters.chat;

import com.pnc.masters.chat.api.ChatRateLimitedException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatSendRateLimiter {

    static final int MAX_PER_MINUTE = 30;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ConcurrentHashMap<Long, Deque<Instant>> sentAt = new ConcurrentHashMap<>();

    public void check(Long userId) {
        Instant cutoff = Instant.now().minus(WINDOW);
        Deque<Instant> times = sentAt.computeIfAbsent(userId, id -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(cutoff)) {
                times.removeFirst();
            }
            if (times.size() >= MAX_PER_MINUTE) {
                throw new ChatRateLimitedException();
            }
            times.addLast(Instant.now());
        }
    }
}
