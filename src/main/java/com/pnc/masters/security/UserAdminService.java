package com.pnc.masters.security;

import com.pnc.masters.security.api.AdminUserCreateRequest;
import com.pnc.masters.security.api.AdminUserResponse;
import com.pnc.masters.security.api.AdminUserUpdateRequest;
import com.pnc.masters.security.api.DuplicateEmailException;
import com.pnc.masters.security.api.RoleConflictException;
import com.pnc.masters.security.api.RoleNotFoundException;
import com.pnc.masters.security.api.UserEnabledRequest;
import com.pnc.masters.security.api.UserNotFoundException;
import com.pnc.masters.security.api.UserRolesRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class UserAdminService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(
            AppUserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> findAll() {
        return userRepository.findAllByOrderByEmailAsc().stream().map(this::toResponse).toList();
    }

    public AdminUserResponse create(AdminUserCreateRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setEnabled(request.enabled() == null || request.enabled());
        applyProfile(
                user,
                request.dateOfJoining(),
                request.department(),
                request.branch(),
                request.homeAddress(),
                request.dateOfBirth(),
                request.designation(),
                request.regularTiming(),
                request.contactNumber()
        );
        user.setRoles(resolveRoles(request.roles(), true));
        return toResponse(userRepository.save(user));
    }

    public AdminUserResponse update(Long userId, AdminUserUpdateRequest request) {
        AppUser user = getUser(userId);
        String email = normalizeEmail(request.email());
        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }
        user.setEmail(email);
        user.setDisplayName(request.displayName().trim());
        applyProfile(
                user,
                request.dateOfJoining(),
                request.department(),
                request.branch(),
                request.homeAddress(),
                request.dateOfBirth(),
                request.designation(),
                request.regularTiming(),
                request.contactNumber()
        );
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.roles() != null) {
            applyRoles(user, resolveRoles(request.roles(), false));
        }
        if (request.enabled() != null) {
            applyEnabled(user, request.enabled());
        }
        return toResponse(userRepository.save(user));
    }

    public AdminUserResponse setEnabled(Long userId, UserEnabledRequest request) {
        AppUser user = getUser(userId);
        applyEnabled(user, request.enabled());
        return toResponse(userRepository.save(user));
    }

    public AdminUserResponse setRoles(Long userId, UserRolesRequest request) {
        AppUser user = getUser(userId);
        applyRoles(user, resolveRoles(request.roles(), false));
        return toResponse(userRepository.save(user));
    }

    private void applyEnabled(AppUser user, boolean enabled) {
        if (!enabled && isLastEnabledAdmin(user)) {
            throw new RoleConflictException("Cannot disable the last enabled administrator");
        }
        user.setEnabled(enabled);
    }

    private void applyRoles(AppUser user, Set<Role> roles) {
        boolean removingAdmin = hasAdmin(user) && roles.stream().noneMatch(role -> AuthService.ROLE_ADMIN.equals(role.getRoleCode()));
        if (removingAdmin && isLastEnabledAdmin(user)) {
            throw new RoleConflictException("Cannot remove ADMIN from the last enabled administrator");
        }
        user.setRoles(roles);
    }

    private Set<Role> resolveRoles(List<String> codes, boolean defaultUserWhenEmpty) {
        List<String> wanted = codes == null
                ? List.of()
                : codes.stream().map(String::trim).filter(code -> !code.isEmpty()).toList();
        if (wanted.isEmpty()) {
            if (!defaultUserWhenEmpty) {
                throw new RoleConflictException("Assign at least one role");
            }
            wanted = List.of(AuthService.ROLE_USER);
        }
        Set<Role> roles = new LinkedHashSet<>();
        for (String code : wanted) {
            Role role = roleRepository.findByRoleCodeIgnoreCase(code)
                    .orElseThrow(() -> new RoleNotFoundException(code));
            roles.add(role);
        }
        return roles;
    }

    private static void applyProfile(
            AppUser user,
            java.time.LocalDate dateOfJoining,
            String department,
            String branch,
            String homeAddress,
            java.time.LocalDate dateOfBirth,
            String designation,
            String regularTiming,
            String contactNumber
    ) {
        user.setDateOfJoining(dateOfJoining);
        user.setDepartment(blankToNull(department));
        user.setBranch(blankToNull(branch));
        user.setHomeAddress(blankToNull(homeAddress));
        user.setDateOfBirth(dateOfBirth);
        user.setDesignation(blankToNull(designation));
        user.setRegularTiming(blankToNull(regularTiming));
        user.setContactNumber(blankToNull(contactNumber));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
                user.getRoles().stream().map(Role::getRoleCode).sorted().toList(),
                user.getDateOfJoining(),
                user.getDepartment(),
                user.getBranch(),
                user.getHomeAddress(),
                user.getDateOfBirth(),
                user.getDesignation(),
                user.getRegularTiming(),
                user.getContactNumber()
        );
    }
}
