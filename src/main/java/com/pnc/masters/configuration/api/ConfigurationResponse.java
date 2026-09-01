package com.pnc.masters.configuration.api;

import java.time.LocalDateTime;

public record ConfigurationResponse(
        String configKey,
        String configValue,
        String description,
        LocalDateTime updatedAt
) {
}
