package com.pnc.masters.security;

import com.pnc.masters.security.api.ModuleRequest;
import com.pnc.masters.security.api.ModuleResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/modules")
public class ModuleController {

    private final ModuleAccessService moduleAccessService;

    public ModuleController(ModuleAccessService moduleAccessService) {
        this.moduleAccessService = moduleAccessService;
    }

    @GetMapping
    public List<ModuleResponse> findAll() {
        return moduleAccessService.findModules();
    }

    @PostMapping
    public ResponseEntity<ModuleResponse> create(@Valid @RequestBody ModuleRequest request) {
        ModuleResponse response = moduleAccessService.createModule(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.moduleId()).toUri();
        return ResponseEntity.created(location).body(response);
    }
}
