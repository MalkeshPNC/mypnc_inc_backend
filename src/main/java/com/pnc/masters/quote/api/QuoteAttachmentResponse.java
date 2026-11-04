package com.pnc.masters.quote.api;

import java.time.LocalDateTime;

public record QuoteAttachmentResponse(
        Long attachmentId,
        Long qid,
        String category,
        String originalFilename,
        String contentType,
        long fileSize,
        boolean generated,
        LocalDateTime createdAt
) {
}
