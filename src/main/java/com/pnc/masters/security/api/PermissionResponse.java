package com.pnc.masters.security.api;

import java.util.List;

public record PermissionResponse(
        boolean superAdmin,
        List<ModulePermissionResponse> modules
) {
}
