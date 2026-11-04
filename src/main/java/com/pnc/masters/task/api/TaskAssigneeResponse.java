package com.pnc.masters.task.api;

import java.time.LocalDateTime;

public record TaskAssigneeResponse(
        Long userId,
        String displayName,
        String status,
        String comment,
        LocalDateTime commentAt,
        Long assignedByUserId,
        String assignedByName
) {
}
