package com.pnc.masters.security;

import com.pnc.masters.security.api.DuplicateEmailException;
import com.pnc.masters.security.api.InvalidCredentialsException;
import com.pnc.masters.security.api.LoginRequest;
import com.pnc.masters.security.api.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AppUserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleRepository, passwordEncoder, new JwtService(new AuthProperties()));
    }

    @Test
    void signupRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(new SignupRequest("ada@example.com", "password1", "Ada")))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void loginRejectsBadPassword() {
        AppUser user = new AppUser();
        user.setEmail("ada@example.com");
        user.setPasswordHash("hash");
        user.setEnabled(true);
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ada@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void signupCreatesUserWithUserRole() {
        Role role = new Role();
        role.setRoleCode("USER");
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(roleRepository.findByRoleCodeIgnoreCase("USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password1")).thenReturn("hash");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser saved = invocation.getArgument(0);
            saved.setUserId(9L);
            return saved;
        });

        var response = authService.signup(new SignupRequest("ada@example.com", "password1", "Ada"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.user().roles()).containsExactly("USER");
    }
}
