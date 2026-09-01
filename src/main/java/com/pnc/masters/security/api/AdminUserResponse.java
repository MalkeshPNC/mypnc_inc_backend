package com.pnc.masters.security.api;

import java.util.List;

public record AdminUserResponse(
        Long userId,
        String email,
        String displayName,
        boolean enabled,
        List<String> roles
) {
}
