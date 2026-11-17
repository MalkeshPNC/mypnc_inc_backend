package com.pnc.masters.task.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TaskResponse(
        Long taskId,
        String title,
        String description,
        String taskType,
        String priority,
        String jobNumber,
        String ncNumber,
        Long qid,
        LocalDate reminderDate,
        String status,
        Long createdByUserId,
        String createdByName,
        List<TaskReviewerResponse> reviewers,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TaskAssigneeResponse> assignees,
        List<TaskGroupResponse> groups,
        List<TaskFileResponse> files
) {
}
