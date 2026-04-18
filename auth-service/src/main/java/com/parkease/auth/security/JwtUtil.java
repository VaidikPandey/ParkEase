package com.parkease.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expiryMs;
    private final long refreshExpiryMs;

    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.expiry-ms}") long expiryMs,
        @Value("${jwt.refresh-expiry-ms}") long refreshExpiryMs
    ) {
        this.secretKey     = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMs      = expiryMs;
        this.refreshExpiryMs = refreshExpiryMs;
    }

    public String generateAccessToken(String email, String role, Long userId) {
        return Jwts.builder()
            .subject(email)
            .claim("role", role)
            .claim("userId", userId)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiryMs))
            .signWith(secretKey)
            .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
            .subject(email)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshExpiryMs))
            .signWith(secretKey)
            .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }
}
