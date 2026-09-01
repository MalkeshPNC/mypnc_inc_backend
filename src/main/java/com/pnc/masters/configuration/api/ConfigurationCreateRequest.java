package com.pnc.masters.configuration.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConfigurationCreateRequest(
        @NotBlank(message = "configKey is required")
        @Size(max = 100, message = "configKey must be at most 100 characters")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9._-]*$", message = "configKey must start with a letter and contain only letters, numbers, dots, hyphens, or underscores")
        String configKey,
        @NotBlank(message = "configValue is required")
        @Size(max = 500, message = "configValue must be at most 500 characters")
        String configValue,
        @Size(max = 255, message = "description must be at most 255 characters")
        String description
) {
}
