package com.pnc.masters;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EnvFileLoaderTest {

    @Test
    void readsAssignmentsAndSkipsComments() {
        Map<String, String> values = EnvFileLoader.parse(List.of(
                "# comment",
                "",
                "MYSQL_USER=root",
                "export MAIL_FROM=\"quotes@example.com\"",
                "JWT_SECRET='keep this # hash'"
        ));

        assertThat(values)
                .containsEntry("MYSQL_USER", "root")
                .containsEntry("MAIL_FROM", "quotes@example.com")
                .containsEntry("JWT_SECRET", "keep this # hash");
    }

    @Test
    void treatsStagingAsARunFlag() {
        assertThat(EnvFileLoader.stagingRequested(new String[]{"--spring.profiles.active=staging"})).isTrue();
        assertThat(EnvFileLoader.stagingRequested(new String[]{"--spring.profiles.active=dev,staging"})).isTrue();
        assertThat(EnvFileLoader.stagingRequested(new String[]{})).isFalse();
    }

    @Test
    void doesNotReplaceAValueAlreadySetOnTheProcess() {
        String key = "PNC_ENV_LOADER_TEST";
        System.setProperty(key, "already-set");
        try {
            EnvFileLoader.apply(Map.of(key, "from-file"));
            assertThat(System.getProperty(key)).isEqualTo("already-set");
        } finally {
            System.clearProperty(key);
        }
    }
}
