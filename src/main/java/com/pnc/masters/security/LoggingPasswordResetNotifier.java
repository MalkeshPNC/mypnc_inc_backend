package com.pnc.masters.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingPasswordResetNotifier implements PasswordResetNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetNotifier.class);

    @Override
    public void sendResetLink(String email, String resetUrl) {
        log.warn("Password reset link for {}: {}", email, resetUrl);
    }
}
