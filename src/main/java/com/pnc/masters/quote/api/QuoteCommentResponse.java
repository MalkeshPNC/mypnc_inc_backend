package com.pnc.masters.quote.api;

import java.time.LocalDateTime;
import java.util.List;

public record QuoteCommentResponse(
        Long commentId,
        Long qid,
        Long authorId,
        String authorName,
        String body,
        Long replyToCommentId,
        LocalDateTime createdAt,
        List<Long> mentionUserIds,
        List<QuoteCommentImageResponse> images
) {
}
