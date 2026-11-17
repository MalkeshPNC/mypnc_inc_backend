package com.pnc.masters.security.api;

import java.time.LocalDate;

public record ProfileResponse(
        Long userId,
        String email,
        String displayName,
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
