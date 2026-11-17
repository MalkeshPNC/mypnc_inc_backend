package com.pnc.masters.security.api;

public record RoleResponse(
        Long roleId,
        String roleCode,
        String roleName,
        boolean system
) {
}
