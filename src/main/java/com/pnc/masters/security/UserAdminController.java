package com.pnc.masters.security;

import com.pnc.masters.security.api.AdminUserResponse;
import com.pnc.masters.security.api.UserEnabledRequest;
import com.pnc.masters.security.api.UserRolesRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public List<AdminUserResponse> findAll() {
        return userAdminService.findAll();
    }

    @PutMapping("/{id}/enabled")
    public AdminUserResponse setEnabled(@PathVariable Long id, @Valid @RequestBody UserEnabledRequest request) {
        return userAdminService.setEnabled(id, request);
    }

    @PutMapping("/{id}/roles")
    public AdminUserResponse setRoles(@PathVariable Long id, @Valid @RequestBody UserRolesRequest request) {
        return userAdminService.setRoles(id, request);
    }
}
