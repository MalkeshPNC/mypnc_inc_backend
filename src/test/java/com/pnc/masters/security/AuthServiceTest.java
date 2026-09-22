package com.pnc.masters.security;

import com.pnc.masters.security.api.ChangePasswordRequest;
import com.pnc.masters.security.api.InvalidCredentialsException;
import com.pnc.masters.security.api.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AppUserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, new JwtService(new AuthProperties()));
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
    void meReturnsProfileFields() {
        AppUser user = enabledUser(9L);
        user.setDepartment("Engineering");
        user.setDateOfJoining(LocalDate.of(2024, 3, 1));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));

        var profile = authService.me(9L);

        assertThat(profile.email()).isEqualTo("ada@example.com");
        assertThat(profile.displayName()).isEqualTo("Ada");
        assertThat(profile.department()).isEqualTo("Engineering");
        assertThat(profile.dateOfJoining()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(profile.branch()).isNull();
        assertThat(profile.homeAddress()).isNull();
        assertThat(profile.dateOfBirth()).isNull();
        assertThat(profile.designation()).isNull();
        assertThat(profile.regularTiming()).isNull();
        assertThat(profile.contactNumber()).isNull();
    }

    @Test
    void changePasswordEncodesAndPersists() {
        AppUser user = enabledUser(9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass12")).thenReturn("encoded-hash");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.changePassword(9L, new ChangePasswordRequest("newpass12"));

        assertThat(response.message()).contains("updated");
        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded-hash");
    }

    private static AppUser enabledUser(Long id) {
        AppUser user = new AppUser();
        user.setUserId(id);
        user.setEmail("ada@example.com");
        user.setDisplayName("Ada");
        user.setPasswordHash("old-hash");
        user.setEnabled(true);
        return user;
    }
}
