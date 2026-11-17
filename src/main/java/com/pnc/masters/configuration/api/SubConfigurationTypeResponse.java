package com.pnc.masters.configuration.api;

import java.time.LocalDateTime;
import java.util.List;

public record SubConfigurationTypeResponse(
        Long typeId,
        String typeCode,
        String typeName,
        String moduleCode,
        List<String> fieldLabels,
        List<SubConfigurationEntryResponse> entries,
        String updatedBy,
        Long updatedByUserId,
        LocalDateTime updatedAt
) {
}
