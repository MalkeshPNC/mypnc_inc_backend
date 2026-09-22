package com.pnc.masters.security;

import com.pnc.masters.security.api.AdminUserCreateRequest;
import com.pnc.masters.security.api.AdminUserResponse;
import com.pnc.masters.security.api.AdminUserUpdateRequest;
import com.pnc.masters.security.api.UserEnabledRequest;
import com.pnc.masters.security.api.UserRolesRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse create(@Valid @RequestBody AdminUserCreateRequest request) {
        return userAdminService.create(request);
    }

    @PutMapping("/{id}")
    public AdminUserResponse update(@PathVariable Long id, @Valid @RequestBody AdminUserUpdateRequest request) {
        return userAdminService.update(id, request);
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
