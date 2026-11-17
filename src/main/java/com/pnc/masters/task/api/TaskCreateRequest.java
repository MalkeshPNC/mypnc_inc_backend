package com.pnc.masters.task.api;

import java.util.List;

public record TaskCreateRequest(
        String title,
        String description,
        String taskType,
        String priority,
        String jobNumber,
        String ncNumber,
        String reminderDate,
        List<Long> assigneeIds,
        List<Long> groupIds,
        List<Long> reviewerIds,
        Long qid
) {
}
