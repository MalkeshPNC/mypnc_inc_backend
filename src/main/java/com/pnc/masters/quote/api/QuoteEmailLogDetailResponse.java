package com.pnc.masters.quote.api;

import java.time.LocalDateTime;

public record QuoteEmailLogDetailResponse(
        Long id,
        Long quoteId,
        String quoteNumber,
        String fromAddress,
        String to,
        String cc,
        String subject,
        String attachmentName,
        String htmlBody,
        String status,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime sentAt,
        Long sentByUserId,
        String sentByName,
        String sentByEmail
) {
}
