package com.pnc.masters.security.api;

import java.time.LocalDate;
import java.util.List;

public record AdminUserResponse(
        Long userId,
        String email,
        String displayName,
        boolean enabled,
        List<String> roles,
        LocalDate dateOfJoining,
        String department,
        String branch,
        String homeAddress,
        LocalDate dateOfBirth,
        String designation,
        String regularTiming,
        String contactNumber
) {
}
