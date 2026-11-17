package com.pnc.masters.security;

public interface PasswordResetNotifier {

    void sendResetLink(String email, String resetUrl);
}
