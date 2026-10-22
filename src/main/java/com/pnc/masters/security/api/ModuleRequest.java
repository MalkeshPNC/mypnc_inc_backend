package com.pnc.masters.security.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ModuleRequest(
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "moduleCode must be uppercase letters, numbers, or underscores")
        String moduleCode,
        @NotBlank @Size(max = 100) String moduleName
) {
}
