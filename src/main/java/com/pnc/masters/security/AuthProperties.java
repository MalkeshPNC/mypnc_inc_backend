package com.pnc.masters.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private String jwtSecret = "pnc-masters-dev-jwt-secret-key-32b!!";
    private long jwtExpirationMs = 28_800_000L;
    private String bootstrapEmail = "admin@mypncinc.local";
    private String bootstrapPassword = "Admin123!";

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
    public long getJwtExpirationMs() { return jwtExpirationMs; }
    public void setJwtExpirationMs(long jwtExpirationMs) { this.jwtExpirationMs = jwtExpirationMs; }
    public String getBootstrapEmail() { return bootstrapEmail; }
    public void setBootstrapEmail(String bootstrapEmail) { this.bootstrapEmail = bootstrapEmail; }
    public String getBootstrapPassword() { return bootstrapPassword; }
    public void setBootstrapPassword(String bootstrapPassword) { this.bootstrapPassword = bootstrapPassword; }
}
