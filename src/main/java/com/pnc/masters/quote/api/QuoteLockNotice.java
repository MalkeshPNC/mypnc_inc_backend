package com.pnc.masters.quote.api;

import java.time.Instant;

/** Pushed to the editor and the admin on the existing user socket. */
public record QuoteLockNotice(
        String type,
        Long qid,
        String holderName,
        String adminName,
        Long adminUserId,
        Instant transferAt
) {
}
