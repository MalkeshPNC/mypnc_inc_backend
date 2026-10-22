package com.pnc.masters.quote.api;

import java.util.List;

public record QuotePageResponse(
        List<QuoteSummaryResponse> items,
        long total,
        int page,
        int size
) {
}
