package com.pnc.masters.configuration.api;

import java.util.List;

public record ConfigurationBundleResponse(
        List<ConfigurationResponse> settings,
        List<SubConfigurationTypeResponse> subConfigurations
) {
}
