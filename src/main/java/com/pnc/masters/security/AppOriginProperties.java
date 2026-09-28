package com.pnc.masters.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppOriginProperties {

    static final List<String> DEFAULT_ORIGINS = List.of(
            "http://localhost:4200",
            "http://localhost:58877",
            "http://192.168.1.122:4200"
    );

    private String origins = "";

    public String getOrigins() {
        return origins;
    }

    public void setOrigins(String origins) {
        this.origins = origins;
    }

    public List<String> originList() {
        if (origins == null || origins.isBlank()) {
            return DEFAULT_ORIGINS;
        }
        return Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }
}
