package com.pnc.masters.task.api;

import java.util.List;

public record TaskUpdateRequest(
        String title,
        String description,
        String taskType,
        String priority,
        String jobNumber,
        String ncNumber,
        String status,
        List<Long> assigneeIds,
        boolean assigneesSpecified,
        List<Long> groupIds,
        boolean groupsSpecified,
        String comment,
        boolean commentSpecified,
        String personalStatus,
        String reminderDate,
        List<Long> reviewerIds,
        boolean reviewerSpecified
) {
}
