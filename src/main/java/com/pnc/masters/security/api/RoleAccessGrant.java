package com.pnc.masters.security.api;

import com.pnc.masters.security.ModuleAccessLevel;
import jakarta.validation.constraints.NotNull;

public record RoleAccessGrant(
        @NotNull Long moduleId,
        @NotNull ModuleAccessLevel access
) {
}
