package com.pnc.masters.configuration.api;

import java.util.List;

public record SubConfigurationTypeResponse(
        Long typeId,
        String typeCode,
        String typeName,
        List<String> fieldLabels,
        List<SubConfigurationEntryResponse> entries
) {
}
