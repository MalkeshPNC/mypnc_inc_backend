package com.pnc.masters.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final AuthProperties properties;

    public JwtService(AuthProperties properties) {
        this.properties = properties;
    }

    public String createToken(Long userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(properties.getJwtExpirationMs());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey())
                .compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException exception) {
            throw new JwtException("Invalid token", exception);
        }
    }

    public long expirationMs() {
        return properties.getJwtExpirationMs();
    }

    private SecretKey signingKey() {
        byte[] bytes = properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
