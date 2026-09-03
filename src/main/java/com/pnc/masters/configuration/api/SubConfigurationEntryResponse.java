package com.pnc.masters.configuration.api;

import java.time.LocalDateTime;
import java.util.List;

public record SubConfigurationEntryResponse(
        Long entryId,
        List<String> values,
        String createdBy,
        LocalDateTime createdAt
) {
}
