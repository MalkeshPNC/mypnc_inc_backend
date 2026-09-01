package com.pnc.masters.security;

import com.pnc.masters.security.api.AdminUserResponse;
import com.pnc.masters.security.api.RoleConflictException;
import com.pnc.masters.security.api.RoleNotFoundException;
import com.pnc.masters.security.api.UserEnabledRequest;
import com.pnc.masters.security.api.UserNotFoundException;
import com.pnc.masters.security.api.UserRolesRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class UserAdminService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserAdminService(AppUserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> findAll() {
        return userRepository.findAllByOrderByEmailAsc().stream().map(this::toResponse).toList();
    }

    public AdminUserResponse setEnabled(Long userId, UserEnabledRequest request) {
        AppUser user = getUser(userId);
        if (!request.enabled() && isLastEnabledAdmin(user)) {
            throw new RoleConflictException("Cannot disable the last enabled administrator");
        }
        user.setEnabled(request.enabled());
        return toResponse(userRepository.save(user));
    }

    public AdminUserResponse setRoles(Long userId, UserRolesRequest request) {
        AppUser user = getUser(userId);
        Set<Role> roles = new LinkedHashSet<>();
        for (String code : request.roles()) {
            Role role = roleRepository.findByRoleCodeIgnoreCase(code.trim())
                    .orElseThrow(() -> new RoleNotFoundException(code));
            roles.add(role);
        }
        boolean removingAdmin = hasAdmin(user) && roles.stream().noneMatch(role -> AuthService.ROLE_ADMIN.equals(role.getRoleCode()));
        if (removingAdmin && isLastEnabledAdmin(user)) {
            throw new RoleConflictException("Cannot remove ADMIN from the last enabled administrator");
        }
        user.setRoles(roles);
        return toResponse(userRepository.save(user));
    }

    private boolean isLastEnabledAdmin(AppUser user) {
        return user.isEnabled() && hasAdmin(user) && userRepository.countEnabledAdmins() <= 1;
    }

    private static boolean hasAdmin(AppUser user) {
        return user.getRoles().stream().anyMatch(role -> AuthService.ROLE_ADMIN.equals(role.getRoleCode()));
    }

    private AppUser getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private AdminUserResponse toResponse(AppUser user) {
        return new AdminUserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isEnabled(),
                user.getRoles().stream().map(Role::getRoleCode).sorted().toList()
        );
    }
}
