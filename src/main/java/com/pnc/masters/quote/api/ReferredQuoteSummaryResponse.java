package com.pnc.masters.quote.api;

import java.util.List;

public record ReferredQuoteSummaryResponse(
        String referredBy,
        List<ReferredQuoteYearCount> years
) {
}
