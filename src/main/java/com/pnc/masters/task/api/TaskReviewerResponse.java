package com.pnc.masters.task.api;

import java.time.LocalDateTime;

public record TaskReviewerResponse(
        Long userId,
        String displayName,
        String comment,
        LocalDateTime commentAt
) {
}
