package com.pnc.masters.security.api;

import java.util.List;

public record EmployeeGroupRequest(String name, String type, List<Long> userIds) {
}
