package com.pnc.masters.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private String jwtSecret = "pnc-masters-dev-jwt-secret-key-32b!!";
    private long jwtExpirationMs = 28_800_000L;
    private String bootstrapEmail = "admin@mypncinc.local";
    private String bootstrapPassword = "Admin123!";
    private String resetLinkBaseUrl = "http://localhost:4200";
    private long resetTokenTtlMs = 3_600_000L;

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
    public long getJwtExpirationMs() { return jwtExpirationMs; }
    public void setJwtExpirationMs(long jwtExpirationMs) { this.jwtExpirationMs = jwtExpirationMs; }
    public String getBootstrapEmail() { return bootstrapEmail; }
    public void setBootstrapEmail(String bootstrapEmail) { this.bootstrapEmail = bootstrapEmail; }
    public String getBootstrapPassword() { return bootstrapPassword; }
    public void setBootstrapPassword(String bootstrapPassword) { this.bootstrapPassword = bootstrapPassword; }
    public String getResetLinkBaseUrl() { return resetLinkBaseUrl; }
    public void setResetLinkBaseUrl(String resetLinkBaseUrl) { this.resetLinkBaseUrl = resetLinkBaseUrl; }
    public long getResetTokenTtlMs() { return resetTokenTtlMs; }
    public void setResetTokenTtlMs(long resetTokenTtlMs) { this.resetTokenTtlMs = resetTokenTtlMs; }
}
