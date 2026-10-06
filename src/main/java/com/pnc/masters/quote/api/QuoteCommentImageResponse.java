package com.pnc.masters.quote.api;

public record QuoteCommentImageResponse(
        Long imageId,
        String filename,
        String contentType
) {
}
