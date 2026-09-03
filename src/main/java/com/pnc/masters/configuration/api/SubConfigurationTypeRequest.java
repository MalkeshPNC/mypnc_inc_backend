package com.pnc.masters.configuration.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubConfigurationTypeRequest(
        @NotBlank
        @Size(max = 80)
        @Pattern(
                regexp = "^[a-zA-Z][a-zA-Z0-9_-]*$",
                message = "typeCode must start with a letter and contain only letters, numbers, hyphens, or underscores"
        )
        String typeCode,
        @NotBlank @Size(max = 120) String typeName,
        @NotEmpty @Size(min = 1, max = 8) List<@NotBlank @Size(max = 80) String> fieldLabels
) {
}
