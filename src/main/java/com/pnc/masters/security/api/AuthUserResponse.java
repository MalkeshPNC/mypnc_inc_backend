package com.pnc.masters.security.api;

import java.util.List;

public record AuthUserResponse(
        Long userId,
        String email,
        String displayName,
        List<String> roles
) {
}
