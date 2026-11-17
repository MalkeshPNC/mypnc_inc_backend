package com.pnc.masters.security.api;

import com.pnc.masters.security.ModuleAccessLevel;

public record ModulePermissionResponse(
        String moduleCode,
        String moduleName,
        ModuleAccessLevel access
) {
}
