package com.pnc.masters.security.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters") String password,
        @NotBlank @Size(max = 200) String displayName
) {
}
