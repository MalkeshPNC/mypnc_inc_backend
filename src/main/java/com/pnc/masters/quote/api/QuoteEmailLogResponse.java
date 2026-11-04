package com.pnc.masters.quote.api;

import java.time.LocalDateTime;

public record QuoteEmailLogResponse(
        Long id,
        Long quoteId,
        String quoteNumber,
        String to,
        String cc,
        String subject,
        String attachmentName,
        String status,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime sentAt,
        Long sentByUserId,
        String sentByName,
        String sentByEmail
) {
}
