package com.pnc.masters.configuration;

import com.pnc.masters.configuration.api.ConfigurationCreateRequest;
import com.pnc.masters.configuration.api.ConfigurationKeyExistsException;
import com.pnc.masters.configuration.api.ConfigurationNotFoundException;
import com.pnc.masters.configuration.api.ConfigurationResponse;
import com.pnc.masters.configuration.api.ConfigurationUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AppConfigurationService {

    private final AppConfigurationRepository repository;

    public AppConfigurationService(AppConfigurationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ConfigurationResponse> findAll() {
        return repository.findAllByOrderByConfigKeyAsc().stream().map(this::toResponse).toList();
    }

    public ConfigurationResponse create(ConfigurationCreateRequest request) {
        String key = request.configKey().trim();
        if (repository.existsById(key)) {
            throw new ConfigurationKeyExistsException(key);
        }
        AppConfiguration configuration = new AppConfiguration();
        configuration.setConfigKey(key);
        applyValue(configuration, request.configValue(), request.description());
        return toResponse(repository.save(configuration));
    }

    public ConfigurationResponse update(String key, ConfigurationUpdateRequest request) {
        AppConfiguration configuration = getConfiguration(key);
        applyValue(configuration, request.configValue(), request.description());
        return toResponse(repository.save(configuration));
    }

    public void delete(String key) {
        repository.delete(getConfiguration(key));
    }

    private AppConfiguration getConfiguration(String key) {
        return repository.findById(key)
                .orElseThrow(() -> new ConfigurationNotFoundException(key));
    }

    private void applyValue(AppConfiguration configuration, String value, String description) {
        configuration.setConfigValue(value.trim());
        configuration.setDescription(blankToNull(description));
        configuration.setUpdatedAt(LocalDateTime.now());
    }

    private ConfigurationResponse toResponse(AppConfiguration configuration) {
        return new ConfigurationResponse(
                configuration.getConfigKey(),
                configuration.getConfigValue(),
                configuration.getDescription(),
                configuration.getUpdatedAt());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
