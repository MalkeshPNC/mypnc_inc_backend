package com.pnc.masters.configuration;

import com.pnc.masters.configuration.api.ConfigurationCreateRequest;
import com.pnc.masters.configuration.api.ConfigurationKeyExistsException;
import com.pnc.masters.configuration.api.ConfigurationNotFoundException;
import com.pnc.masters.configuration.api.ConfigurationResponse;
import com.pnc.masters.configuration.api.ConfigurationUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppConfigurationServiceTest {

    @Mock
    private AppConfigurationRepository repository;

    @InjectMocks
    private AppConfigurationService service;

    @Test
    void createPersistsTrimmedKeyAndValue() {
        when(repository.existsById("salesperson.defaultCommission")).thenReturn(false);
        when(repository.save(any(AppConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfigurationResponse response = service.create(new ConfigurationCreateRequest(
                "salesperson.defaultCommission", " 12.5 ", "  Default commission  "));

        assertThat(response.configKey()).isEqualTo("salesperson.defaultCommission");
        assertThat(response.configValue()).isEqualTo("12.5");
        assertThat(response.description()).isEqualTo("Default commission");
        ArgumentCaptor<AppConfiguration> captor = ArgumentCaptor.forClass(AppConfiguration.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    void createRejectsDuplicateKey() {
        when(repository.existsById("salesperson.defaultCommission")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new ConfigurationCreateRequest(
                "salesperson.defaultCommission", "10", null)))
                .isInstanceOf(ConfigurationKeyExistsException.class);
    }

    @Test
    void updateReplacesValue() {
        AppConfiguration existing = new AppConfiguration();
        existing.setConfigKey("salesperson.defaultCommission");
        existing.setConfigValue("0");
        existing.setUpdatedAt(LocalDateTime.now().minusDays(1));
        when(repository.findById("salesperson.defaultCommission")).thenReturn(Optional.of(existing));
        when(repository.save(any(AppConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfigurationResponse response = service.update(
                "salesperson.defaultCommission",
                new ConfigurationUpdateRequest("15", "Updated"));

        assertThat(response.configValue()).isEqualTo("15");
        assertThat(response.description()).isEqualTo("Updated");
    }

    @Test
    void deleteMissingKeyThrows() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("missing"))
                .isInstanceOf(ConfigurationNotFoundException.class);
    }
}
