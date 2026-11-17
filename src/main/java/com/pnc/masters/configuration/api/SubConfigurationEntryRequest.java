package com.pnc.masters.configuration.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubConfigurationEntryRequest(
        @NotNull @Size(min = 1, max = 8) List<@Size(max = 255) String> values
) {
}
