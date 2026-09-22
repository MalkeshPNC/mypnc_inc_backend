package com.pnc.masters.chat.api;

import java.time.Instant;

public record ChatPresenceEvent(
        Long userId,
        String status,
        Instant lastSeenAt
) {
}
