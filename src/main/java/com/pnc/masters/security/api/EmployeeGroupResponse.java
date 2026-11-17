package com.pnc.masters.security.api;

import java.time.LocalDateTime;
import java.util.List;

public record EmployeeGroupResponse(
        Long groupId,
        String name,
        String type,
        Long createdByUserId,
        String createdByName,
        LocalDateTime createdAt,
        List<EmployeeGroupMemberResponse> members
) {
}
