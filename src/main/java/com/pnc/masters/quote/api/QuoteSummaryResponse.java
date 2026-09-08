package com.pnc.masters.quote.api;

import java.time.LocalDate;

public record QuoteSummaryResponse(
        Long qid,
        String quoteNumber,
        String quoteType,
        String customerName,
        String ncNumber,
        String status,
        LocalDate createDate,
        QuoteLockResponse lock
) {
}
