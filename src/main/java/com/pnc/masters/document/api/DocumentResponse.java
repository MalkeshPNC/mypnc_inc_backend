package com.pnc.masters.document.api;

import java.time.LocalDateTime;

public record DocumentResponse(
        String documentId,
        Long customerId,
        String category,
        String originalFilename,
        String storedFilename,
        String contentType,
        long fileSize,
        String checksumSha256,
        LocalDateTime createdAt
) { }
