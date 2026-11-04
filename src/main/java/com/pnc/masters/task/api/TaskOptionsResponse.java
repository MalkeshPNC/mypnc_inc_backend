package com.pnc.masters.task.api;

import java.util.List;

public record TaskOptionsResponse(
        List<String> types,
        List<TaskPersonResponse> people,
        List<TaskPersonResponse> reviewers,
        List<TaskGroupOption> groups
) {
}
