package com.pnc.masters.security;

import com.pnc.masters.security.api.PermissionResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class PermissionController {

    private final ModuleAccessService moduleAccessService;

    public PermissionController(ModuleAccessService moduleAccessService) {
        this.moduleAccessService = moduleAccessService;
    }

    @GetMapping("/permissions")
    public PermissionResponse permissions(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return moduleAccessService.permissionsFor(userId);
    }
}
