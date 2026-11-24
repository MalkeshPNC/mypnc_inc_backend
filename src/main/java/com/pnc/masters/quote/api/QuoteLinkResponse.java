package com.pnc.masters.quote.api;

import java.time.LocalDate;

public record QuoteLinkResponse(
        Long qid,
        String quoteNumber,
        String status,
        String customerName,
        LocalDate createDate
) {
}
