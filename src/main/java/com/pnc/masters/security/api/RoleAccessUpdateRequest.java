package com.pnc.masters.security.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RoleAccessUpdateRequest(
        @NotEmpty List<@Valid RoleAccessGrant> grants
) {
}
