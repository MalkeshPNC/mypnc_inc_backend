package com.pnc.masters.configuration.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfigurationUpdateRequest(
        @NotBlank(message = "configValue is required")
        @Size(max = 8000, message = "configValue must be at most 8000 characters")
        String configValue,
        @Size(max = 255, message = "description must be at most 255 characters")
        String description,
        boolean useEditor
) {
}
