package com.pnc.masters.configuration;

import com.pnc.masters.configuration.api.ConfigurationCreateRequest;
import com.pnc.masters.configuration.api.ConfigurationResponse;
import com.pnc.masters.configuration.api.ConfigurationUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/configurations")
public class AppConfigurationController {

    private final AppConfigurationService configurationService;

    public AppConfigurationController(AppConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping
    public List<ConfigurationResponse> findAll() {
        return configurationService.findAll();
    }

    @PostMapping
    public ResponseEntity<ConfigurationResponse> create(@Valid @RequestBody ConfigurationCreateRequest request) {
        ConfigurationResponse response = configurationService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{key}")
                .buildAndExpand(response.configKey()).toUri();
        return ResponseEntity.created(location).body(response);
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
