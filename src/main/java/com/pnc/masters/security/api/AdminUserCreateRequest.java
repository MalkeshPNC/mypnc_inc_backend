package com.pnc.masters.security.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record AdminUserCreateRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters") String password,
        @NotBlank @Size(max = 200) String displayName,
        List<String> roles,
        Boolean enabled,
        LocalDate dateOfJoining,
        @Size(max = 120) String department,
        @Size(max = 120) String branch,
        @Size(max = 500) String homeAddress,
        LocalDate dateOfBirth,
        @Size(max = 120) String designation,
        @Size(max = 80) String regularTiming,
        @Size(max = 40) String contactNumber
) {
}
