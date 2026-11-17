package com.pnc.masters.configuration;

import com.pnc.masters.configuration.api.ConfigurationBundleResponse;
import com.pnc.masters.configuration.api.ConfigurationCreateRequest;
import com.pnc.masters.configuration.api.ConfigurationResponse;
import com.pnc.masters.configuration.api.ConfigurationUpdateRequest;
import com.pnc.masters.configuration.api.QuoteFaiConfigRequest;
import com.pnc.masters.configuration.api.QuoteFaiConfigResponse;
import com.pnc.masters.configuration.api.SubConfigurationEntryRequest;
import com.pnc.masters.configuration.api.SubConfigurationEntryResponse;
import com.pnc.masters.configuration.api.SubConfigurationTypeRequest;
import com.pnc.masters.configuration.api.SubConfigurationTypeResponse;
import com.pnc.masters.configuration.api.SubConfigurationValidationException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/configurations")
public class AppConfigurationController {

    private final AppConfigurationService configurationService;
    private final SubConfigurationService subConfigurationService;
    private final QuoteFaiConfigService faiConfigService;

    public AppConfigurationController(
            AppConfigurationService configurationService,
            SubConfigurationService subConfigurationService,
            QuoteFaiConfigService faiConfigService
    ) {
        this.configurationService = configurationService;
        this.subConfigurationService = subConfigurationService;
        this.faiConfigService = faiConfigService;
    }

    @GetMapping
    public ConfigurationBundleResponse findAll(@RequestParam(name = "module", required = false) String module) {
        String scope = scope(module);
        boolean withSettings = scope == null || "QUOTE".equals(scope);
        return new ConfigurationBundleResponse(
                withSettings ? configurationService.findAll() : List.of(),
                subConfigurationService.findTypes(scope),
                withSettings ? faiConfigService.find() : null
        );
    }

    private static String scope(String module) {
        if (module == null || module.isBlank() || "ALL".equalsIgnoreCase(module.trim())) {
            return null;
        }
        String code = module.trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z][A-Z0-9_]{0,49}")) {
            throw new SubConfigurationValidationException("Unknown module.");
        }
        return code;
    }

    @GetMapping("/fai")
    public QuoteFaiConfigResponse findFai() {
        return faiConfigService.find();
    }

    @PutMapping("/fai")
    public QuoteFaiConfigResponse updateFai(
            @RequestBody QuoteFaiConfigRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return faiConfigService.update(request, userId);
    }

    @PostMapping
    public ResponseEntity<ConfigurationResponse> create(
            @Valid @RequestBody ConfigurationCreateRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        ConfigurationResponse response = configurationService.create(request, userId);
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

    @PutMapping("/types/{typeId}")
    public SubConfigurationTypeResponse updateType(
            @PathVariable Long typeId,
            @Valid @RequestBody SubConfigurationTypeRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return subConfigurationService.updateType(typeId, request, userId);
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

    @PutMapping("/types/{typeId}/entries/{entryId}")
    public SubConfigurationEntryResponse updateEntry(
            @PathVariable Long typeId,
            @PathVariable Long entryId,
            @Valid @RequestBody SubConfigurationEntryRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return subConfigurationService.updateEntry(typeId, entryId, request, userId);
    }

    @DeleteMapping("/types/{typeId}/entries/{entryId}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long typeId, @PathVariable Long entryId) {
        subConfigurationService.deleteEntry(typeId, entryId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{key:.+}")
    public ConfigurationResponse update(
            @PathVariable String key,
            @Valid @RequestBody ConfigurationUpdateRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return configurationService.update(key, request, userId);
    }

    @DeleteMapping("/{key:.+}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        configurationService.delete(key);
        return ResponseEntity.noContent().build();
    }
}
