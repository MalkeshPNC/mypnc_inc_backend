package com.pnc.masters.chat.api;

import java.time.Instant;

public record ChatPersonResponse(
        Long userId,
        String displayName,
        String status,
        Instant lastSeenAt
) {
}
