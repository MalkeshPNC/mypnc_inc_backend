package com.pnc.masters.quote.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record QuoteSummaryResponse(
        Long qid,
        String quoteNumber,
        String quoteType,
        String projectNumber,
        LocalDate createDate,
        LocalDate submitDate,
        LocalDate receivedDate,
        String customerName,
        String ncNumber,
        String assyNumber,
        String pcbNumber,
        String assyQuoteStatus,
        String pcbQuoteStatus,
        String status,
        boolean itarc,
        boolean berryc,
        boolean samsReview,
        QuoteLockResponse lock,
        int familySize,
        Long custId,
        BigDecimal receivedTotal
) {
}
