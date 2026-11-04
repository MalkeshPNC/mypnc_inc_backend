package com.pnc.masters.quote.api;

import java.util.List;

public record QuoteEmailLogPageResponse(
        List<QuoteEmailLogResponse> items,
        long total,
        int page,
        int size,
        List<QuoteEmailSenderResponse> senders
) {
}
