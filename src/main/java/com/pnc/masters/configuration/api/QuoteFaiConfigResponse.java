package com.pnc.masters.configuration.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuoteFaiConfigResponse(
        String headingNotes,
        String importantNotes,
        BigDecimal pncFaiPcb,
        BigDecimal pncFaiPcba,
        BigDecimal as9102FaiPcb,
        BigDecimal as9102FaiPcba,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
}
