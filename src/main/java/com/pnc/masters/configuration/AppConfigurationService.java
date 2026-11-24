package com.pnc.masters.configuration;

import com.pnc.masters.configuration.api.ConfigurationCreateRequest;
import com.pnc.masters.configuration.api.ConfigurationKeyExistsException;
import com.pnc.masters.configuration.api.ConfigurationNotFoundException;
import com.pnc.masters.configuration.api.ConfigurationResponse;
import com.pnc.masters.configuration.api.ConfigurationUpdateRequest;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AppConfigurationService {

    private static final String VALUE_TYPE_TEXT = "text";

    private final AppConfigurationRepository repository;
    private final AppUserRepository userRepository;

    public AppConfigurationService(AppConfigurationRepository repository, AppUserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ConfigurationResponse> findAll() {
        return repository.findAllByOrderByConfigKeyAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ConfigurationResponse findByKey(String key) {
        return toResponse(getConfiguration(key));
    }

    public ConfigurationResponse create(ConfigurationCreateRequest request, Long userId) {
        String key = request.configKey().trim();
        if (repository.existsById(key)) {
            throw new ConfigurationKeyExistsException(key);
        }
        AppConfiguration configuration = new AppConfiguration();
        configuration.setConfigKey(key);
        applyValue(configuration, request.configValue(), request.description(), request.useEditor(), request.valueType(), userId);
        return toResponse(repository.save(configuration));
    }

    public ConfigurationResponse update(String key, ConfigurationUpdateRequest request, Long userId) {
        AppConfiguration configuration = getConfiguration(key);
        applyValue(configuration, request.configValue(), request.description(), request.useEditor(), request.valueType(), userId);
        return toResponse(repository.save(configuration));
    }

    public void delete(String key) {
        repository.delete(getConfiguration(key));
    }

    private AppConfiguration getConfiguration(String key) {
        return repository.findById(key)
                .orElseThrow(() -> new ConfigurationNotFoundException(key));
    }

    private void applyValue(
            AppConfiguration configuration,
            String value,
            String description,
            boolean useEditor,
            String valueType,
            Long userId
    ) {
        configuration.setConfigValue(value.trim());
        configuration.setDescription(blankToNull(description));
        configuration.setUseEditor(useEditor);
        configuration.setValueType(valueType == null || valueType.isBlank() ? VALUE_TYPE_TEXT : valueType.trim());
        configuration.setUpdatedAt(LocalDateTime.now());
        AppUser user = userId == null ? null : userRepository.findById(userId).orElse(null);
        configuration.setUpdatedBy(user == null ? "Unknown" : user.getDisplayName());
        configuration.setUpdatedByUserId(user == null ? null : user.getUserId());
    }

    private ConfigurationResponse toResponse(AppConfiguration configuration) {
        return new ConfigurationResponse(
                configuration.getConfigKey(),
                configuration.getConfigValue(),
                configuration.getDescription(),
                configuration.isUseEditor(),
                configuration.getValueType(),
                configuration.getUpdatedAt(),
                configuration.getUpdatedBy(),
                configuration.getUpdatedByUserId());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
