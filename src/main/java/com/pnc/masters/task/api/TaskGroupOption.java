package com.pnc.masters.task.api;

import java.util.List;

public record TaskGroupOption(Long groupId, String name, List<Long> userIds) {
}
