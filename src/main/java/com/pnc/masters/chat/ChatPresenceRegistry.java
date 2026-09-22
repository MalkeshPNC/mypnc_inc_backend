package com.pnc.masters.chat;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory connection counts plus availability. A user stays connected while
 * at least one tab is open; a connected user can be online or idle.
 */
@Component
public class ChatPresenceRegistry {

    public static final String ONLINE = "online";
    public static final String IDLE = "idle";
    public static final String OFFLINE = "offline";

    private final ConcurrentHashMap<Long, Set<String>> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> availability = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Instant> lastSeen = new ConcurrentHashMap<>();

    public boolean connect(Long userId, String sessionId) {
        lastSeen.put(userId, Instant.now());
        Set<String> open = sessions.computeIfAbsent(userId, id -> ConcurrentHashMap.newKeySet());
        boolean becameOnline = open.isEmpty();
        open.add(sessionId);
        if (becameOnline) {
            availability.put(userId, ONLINE);
        }
        return becameOnline;
    }

    public boolean disconnect(Long userId, String sessionId) {
        lastSeen.put(userId, Instant.now());
        Set<String> open = sessions.get(userId);
        if (open == null) {
            return false;
        }
        open.remove(sessionId);
        if (!open.isEmpty()) {
            return false;
        }
        sessions.remove(userId);
        availability.remove(userId);
        return true;
    }

    public boolean setAvailability(Long userId, String status) {
        if (!isOnline(userId)) {
            return false;
        }
        String next = normalizeAvailability(status);
        if (next == null) {
            return false;
        }
        lastSeen.put(userId, Instant.now());
        String previous = availability.put(userId, next);
        return !next.equals(previous);
    }

    public boolean isOnline(Long userId) {
        Set<String> open = sessions.get(userId);
        return open != null && !open.isEmpty();
    }

    public String status(Long userId) {
        if (!isOnline(userId)) {
            return OFFLINE;
        }
        return availability.getOrDefault(userId, ONLINE);
    }

    public Instant lastSeenAt(Long userId) {
        return lastSeen.get(userId);
    }

    public int connectionCount(Long userId) {
        Set<String> open = sessions.get(userId);
        return open == null ? 0 : open.size();
    }

    private static String normalizeAvailability(String status) {
        if (status == null) {
            return null;
        }
        String value = status.trim().toLowerCase();
        if (ONLINE.equals(value) || IDLE.equals(value)) {
            return value;
        }
        return null;
    }
}
