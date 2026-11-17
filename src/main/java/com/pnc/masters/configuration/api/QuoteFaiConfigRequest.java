package com.pnc.masters.configuration.api;

import java.math.BigDecimal;

public record QuoteFaiConfigRequest(
        String headingNotes,
        String importantNotes,
        BigDecimal pncFaiPcb,
        BigDecimal pncFaiPcba,
        BigDecimal as9102FaiPcb,
        BigDecimal as9102FaiPcba
) {
}
