package com.pnc.masters.security;

import com.pnc.masters.security.api.ForgotPasswordRequest;
import com.pnc.masters.security.api.InvalidResetTokenException;
import com.pnc.masters.security.api.MessageResponse;
import com.pnc.masters.security.api.ResetPasswordRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@Transactional
public class PasswordResetService {

    static final String GENERIC_MESSAGE = "If that email is registered, a reset link was sent.";

    private final AppUserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetNotifier notifier;
    private final AuthProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            AppUserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetNotifier notifier,
            AuthProperties properties
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.notifier = notifier;
        this.properties = properties;
    }

    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(email)
                .filter(AppUser::isEnabled)
                .ifPresent(this::issueResetToken);
        return new MessageResponse(GENERIC_MESSAGE);
    }

    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String tokenHash = hashToken(request.token().trim());
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidResetTokenException::new);
        LocalDateTime now = LocalDateTime.now();
        if (resetToken.getUsedAt() != null || resetToken.getExpiresAt().isBefore(now)) {
            throw new InvalidResetTokenException();
        }
        AppUser user = resetToken.getUser();
        if (!user.isEnabled()) {
            throw new InvalidResetTokenException();
        }
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        resetToken.setUsedAt(now);
        tokenRepository.save(resetToken);
        return new MessageResponse("Your password has been updated. You can sign in now.");
    }

    private void issueResetToken(AppUser user) {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.findByUserAndUsedAtIsNull(user).forEach(token -> {
            token.setUsedAt(now);
            tokenRepository.save(token);
        });
        String rawToken = newToken();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setTokenHash(hashToken(rawToken));
        resetToken.setUser(user);
        resetToken.setExpiresAt(now.plus(properties.getResetTokenTtlMs(), ChronoUnit.MILLIS));
        tokenRepository.save(resetToken);
        String base = properties.getResetLinkBaseUrl().replaceAll("/$", "");
        notifier.sendResetLink(user.getEmail(), base + "/reset-password?token=" + rawToken);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }
}
