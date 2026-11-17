package com.pnc.masters.security.api;

import com.pnc.masters.security.ModuleAccessLevel;

public record RoleAccessResponse(
        Long moduleId,
        String moduleCode,
        String moduleName,
        ModuleAccessLevel access
) {
}
