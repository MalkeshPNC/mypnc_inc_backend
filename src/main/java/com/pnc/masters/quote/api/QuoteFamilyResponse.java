package com.pnc.masters.quote.api;

import java.util.List;

public record QuoteFamilyResponse(String family, List<QuoteResponse> quotes) {
}
