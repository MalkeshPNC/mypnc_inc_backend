package com.pnc.masters.quote.api;

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
        QuoteLockResponse lock,
        int familySize
) {
}
