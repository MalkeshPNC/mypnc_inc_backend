package com.pnc.masters.task.api;

import java.time.LocalDateTime;

public record TaskLogResponse(
        Long logId,
        String fieldName,
        String oldValue,
        String newValue,
        Long userId,
        String userName,
        LocalDateTime createdAt
) {
}
