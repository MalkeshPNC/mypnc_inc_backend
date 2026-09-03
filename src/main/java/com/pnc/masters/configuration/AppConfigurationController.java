package com.pnc.masters.configuration;

import com.pnc.masters.configuration.api.ConfigurationBundleResponse;
import com.pnc.masters.configuration.api.ConfigurationCreateRequest;
import com.pnc.masters.configuration.api.ConfigurationResponse;
import com.pnc.masters.configuration.api.ConfigurationUpdateRequest;
import com.pnc.masters.configuration.api.SubConfigurationEntryRequest;
import com.pnc.masters.configuration.api.SubConfigurationEntryResponse;
import com.pnc.masters.configuration.api.SubConfigurationTypeRequest;
import com.pnc.masters.configuration.api.SubConfigurationTypeResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/configurations")
public class AppConfigurationController {

    private final AppConfigurationService configurationService;
    private final SubConfigurationService subConfigurationService;

    public AppConfigurationController(
            AppConfigurationService configurationService,
            SubConfigurationService subConfigurationService
    ) {
        this.configurationService = configurationService;
        this.subConfigurationService = subConfigurationService;
    }

    @GetMapping
    public ConfigurationBundleResponse findAll() {
        return new ConfigurationBundleResponse(
                configurationService.findAll(),
                subConfigurationService.findAllTypes()
        );
    }

    @PostMapping
    public ResponseEntity<ConfigurationResponse> create(@Valid @RequestBody ConfigurationCreateRequest request) {
        ConfigurationResponse response = configurationService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{key}")
                .buildAndExpand(response.configKey()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/types")
    public ResponseEntity<SubConfigurationTypeResponse> createType(
            @Valid @RequestBody SubConfigurationTypeRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        SubConfigurationTypeResponse response = subConfigurationService.createType(request, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{typeId}")
                .buildAndExpand(response.typeId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/types/{typeId}/entries")
    public ResponseEntity<SubConfigurationEntryResponse> createEntry(
            @PathVariable Long typeId,
            @Valid @RequestBody SubConfigurationEntryRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        SubConfigurationEntryResponse response = subConfigurationService.createEntry(typeId, request, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{entryId}")
                .buildAndExpand(response.entryId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @DeleteMapping("/types/{typeId}/entries/{entryId}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long typeId, @PathVariable Long entryId) {
        subConfigurationService.deleteEntry(typeId, entryId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{key:.+}")
    public ConfigurationResponse update(
            @PathVariable String key,
            @Valid @RequestBody ConfigurationUpdateRequest request
    ) {
        return configurationService.update(key, request);
    }

    @DeleteMapping("/{key:.+}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        configurationService.delete(key);
        return ResponseEntity.noContent().build();
    }
}
