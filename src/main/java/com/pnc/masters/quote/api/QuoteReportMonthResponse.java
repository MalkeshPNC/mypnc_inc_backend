package com.pnc.masters.quote.api;

import java.math.BigDecimal;

public record QuoteReportMonthResponse(
        int month,
        long quotes,
        long orders,
        int winRatio,
        long newCustomers,
        BigDecimal allSmallest,
        BigDecimal allLargest,
        BigDecimal allWon,
        BigDecimal newSmallest,
        BigDecimal newLargest,
        BigDecimal newWon,
        BigDecimal pcbRevenue
) {
}
