package com.pnc.masters.security;

import com.pnc.masters.security.api.ForgotPasswordRequest;
import com.pnc.masters.security.api.InvalidResetTokenException;
import com.pnc.masters.security.api.ResetPasswordRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private AppUserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordResetNotifier notifier;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                userRepository,
                tokenRepository,
                passwordEncoder,
                notifier,
                new AuthProperties()
        );
    }

    @Test
    void forgotPasswordAlwaysReturnsGenericMessage() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        var response = service.forgotPassword(new ForgotPasswordRequest("missing@example.com"));

        assertThat(response.message()).isEqualTo(PasswordResetService.GENERIC_MESSAGE);
        verify(notifier, never()).sendResetLink(anyString(), anyString());
    }

    @Test
    void forgotPasswordIssuesTokenForEnabledUser() {
        AppUser user = enabledUser();
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByUserAndUsedAtIsNull(user)).thenReturn(List.of());
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.forgotPassword(new ForgotPasswordRequest("ada@example.com"));

        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(notifier).sendResetLink(org.mockito.ArgumentMatchers.eq("ada@example.com"), url.capture());
        assertThat(url.getValue()).startsWith("http://localhost:4200/reset-password?token=");
    }

    @Test
    void resetPasswordRejectsUnknownToken() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("nope", "password1")))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    void resetPasswordUpdatesHashAndConsumesToken() {
        AppUser user = enabledUser();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newpass12")).thenReturn("new-hash");

        service.resetPassword(new ResetPasswordRequest("raw-token", "newpass12"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(token.getUsedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    private static AppUser enabledUser() {
        AppUser user = new AppUser();
        user.setUserId(1L);
        user.setEmail("ada@example.com");
        user.setEnabled(true);
        user.setPasswordHash("old");
        return user;
    }
}
