package com.pnc.masters.quote.api;

import java.time.LocalDateTime;

public record QuoteHistoryResponse(
        Long qhId,
        Long qid,
        String action,
        String status,
        String assyQuoteStatus,
        String pcbQuoteStatus,
        Long changedByUserId,
        String changedByName,
        LocalDateTime changedAt
) {
}
