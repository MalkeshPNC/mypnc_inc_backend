package com.pnc.masters.security;

import com.pnc.masters.security.api.RoleAccessResponse;
import com.pnc.masters.security.api.RoleAccessUpdateRequest;
import com.pnc.masters.security.api.RoleRequest;
import com.pnc.masters.security.api.RoleResponse;
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
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;
    private final ModuleAccessService moduleAccessService;

    public RoleController(RoleService roleService, ModuleAccessService moduleAccessService) {
        this.roleService = roleService;
        this.moduleAccessService = moduleAccessService;
    }

    @GetMapping
    public List<RoleResponse> findAll() {
        return roleService.findAll();
    }

    @PostMapping
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleRequest request) {
        RoleResponse response = roleService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.roleId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public RoleResponse update(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return roleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/access")
    public List<RoleAccessResponse> access(@PathVariable Long id) {
        return moduleAccessService.accessFor(id);
    }

    @PutMapping("/{id}/access")
    public List<RoleAccessResponse> replaceAccess(
            @PathVariable Long id,
            @Valid @RequestBody RoleAccessUpdateRequest request
    ) {
        return moduleAccessService.replaceAccess(id, request);
    }
}
