package com.pnc.masters.quote.api;

import java.math.BigDecimal;

public record CustomerQuoteYearResponse(
        int year,
        long quotes,
        long received,
        int winRatio,
        BigDecimal smallest,
        BigDecimal largest,
        BigDecimal won
) {
}
