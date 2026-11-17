package com.pnc.masters.quote.api;

import java.time.LocalDateTime;

public record QuoteLockResponse(
        Long qid,
        Long lockedByUserId,
        String lockedByName,
        boolean heldByCurrentUser,
        LocalDateTime acquiredAt,
        LocalDateTime expiresAt
) {
}
