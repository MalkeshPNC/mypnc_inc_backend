package com.pnc.masters.devtrack.api;

import java.time.LocalDateTime;

public record DevTrackCommentResponse(
        Long commentId,
        Long userId,
        String userName,
        String comment,
        LocalDateTime createdAt
) {
}
