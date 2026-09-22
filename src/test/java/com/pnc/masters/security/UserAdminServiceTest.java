package com.pnc.masters.security;

import com.pnc.masters.security.api.AdminUserCreateRequest;
import com.pnc.masters.security.api.AdminUserUpdateRequest;
import com.pnc.masters.security.api.DuplicateEmailException;
import com.pnc.masters.security.api.RoleConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock private AppUserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private UserAdminService service;

    @BeforeEach
    void setUp() {
        service = new UserAdminService(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void createStoresNormalizedEmailProfileAndDefaultUserRole() {
        Role userRole = role("USER");
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(roleRepository.findByRoleCodeIgnoreCase("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("Secret123")).thenReturn("hash");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser saved = invocation.getArgument(0);
            saved.setUserId(12L);
            return saved;
        });

        var response = service.create(new AdminUserCreateRequest(
                "  Ada@Example.com ",
                "Secret123",
                "Ada Lovelace",
                List.of(),
                null,
                LocalDate.of(2024, 3, 1),
                "Engineering",
                "HQ",
                null,
                null,
                "Engineer",
                null,
                "555-0100"
        ));

        assertThat(response.userId()).isEqualTo(12L);
        assertThat(response.email()).isEqualTo("ada@example.com");
        assertThat(response.roles()).containsExactly("USER");
        assertThat(response.department()).isEqualTo("Engineering");
        assertThat(response.contactNumber()).isEqualTo("555-0100");
        assertThat(response.enabled()).isTrue();
        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new AdminUserCreateRequest(
                "ada@example.com",
                "Secret123",
                "Ada",
                List.of(),
                true,
                null, null, null, null, null, null, null, null
        ))).isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void updateChangesProfileAndOptionalPassword() {
        AppUser existing = user(12L, "ada@example.com", "Ada", true, Set.of(role("USER")));
        when(userRepository.findById(12L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("Newpass12")).thenReturn("new-hash");
        when(roleRepository.findByRoleCodeIgnoreCase("USER")).thenReturn(Optional.of(role("USER")));
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(12L, new AdminUserUpdateRequest(
                "ada@example.com",
                "Newpass12",
                "Ada L",
                List.of("USER"),
                true,
                null,
                "Sales",
                "East",
                null,
                null,
                "Lead",
                "9-5",
                null
        ));

        assertThat(response.displayName()).isEqualTo("Ada L");
        assertThat(response.department()).isEqualTo("Sales");
        assertThat(response.regularTiming()).isEqualTo("9-5");
        assertThat(existing.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void updateCannotRemoveLastAdmin() {
        Role admin = role("ADMIN");
        AppUser existing = user(1L, "admin@example.com", "Admin", true, Set.of(admin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.countEnabledAdmins()).thenReturn(1L);
        when(roleRepository.findByRoleCodeIgnoreCase("USER")).thenReturn(Optional.of(role("USER")));

        assertThatThrownBy(() -> service.update(1L, new AdminUserUpdateRequest(
                "admin@example.com",
                null,
                "Admin",
                List.of("USER"),
                true,
                null, null, null, null, null, null, null, null
        ))).isInstanceOf(RoleConflictException.class);
    }

    private static AppUser user(Long id, String email, String name, boolean enabled, Set<Role> roles) {
        AppUser user = new AppUser();
        user.setUserId(id);
        user.setEmail(email);
        user.setDisplayName(name);
        user.setEnabled(enabled);
        user.setRoles(roles);
        return user;
    }

    private static Role role(String code) {
        Role role = new Role();
        role.setRoleCode(code);
        role.setRoleName(code);
        return role;
    }
}
