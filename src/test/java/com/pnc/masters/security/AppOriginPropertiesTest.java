package com.pnc.masters.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppOriginPropertiesTest {

    @Test
    void usesTheDevelopmentOriginsWhenUnset() {
        AppOriginProperties properties = new AppOriginProperties();

        assertThat(properties.originList()).containsExactlyElementsOf(AppOriginProperties.DEFAULT_ORIGINS);
    }

    @Test
    void splitsOriginsFromTheEnvironment() {
        AppOriginProperties properties = new AppOriginProperties();
        properties.setOrigins(" http://staging-pc:8080, http://localhost:8080 ");

        assertThat(properties.originList()).containsExactly(
                "http://staging-pc:8080",
                "http://localhost:8080"
        );
    }
}
