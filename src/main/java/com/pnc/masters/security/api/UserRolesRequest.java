package com.pnc.masters.security.api;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UserRolesRequest(
        @NotEmpty(message = "at least one role is required") List<String> roles
) {
}
