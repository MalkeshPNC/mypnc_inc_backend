package com.pnc.masters.security;

import com.pnc.masters.security.api.AuthResponse;
import com.pnc.masters.security.api.ChangePasswordRequest;
import com.pnc.masters.security.api.ForgotPasswordRequest;
import com.pnc.masters.security.api.LoginRequest;
import com.pnc.masters.security.api.MessageResponse;
import com.pnc.masters.security.api.ProfileResponse;
import com.pnc.masters.security.api.ResetPasswordRequest;
import com.pnc.masters.security.api.SignupRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/signup")
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return passwordResetService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return passwordResetService.resetPassword(request);
    }

    @GetMapping("/me")
    public ProfileResponse me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return authService.me(userId);
    }

    @PutMapping("/password")
    public MessageResponse changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return authService.changePassword(userId, request);
    }
}
