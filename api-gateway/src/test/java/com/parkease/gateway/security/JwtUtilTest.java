package com.parkease.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String TEST_SECRET = "thisisasupersecretkeyforjwttestinganditshouldbeverylong";

    private JwtUtil jwtUtil;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET);
        secretKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void extractClaims_ShouldReturnClaims_WhenTokenIsValid() {
        String token = Jwts.builder()
                .claim("userId", 1L)
                .claim("role", "DRIVER")
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

        Claims claims = jwtUtil.extractClaims(token);

        assertThat(claims).isNotNull();
        assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
        assertThat(claims.get("role", String.class)).isEqualTo("DRIVER");
    }

    @Test
    void extractRole_ShouldReturnRole_WhenTokenIsValid() {
        String token = Jwts.builder()
                .claim("userId", 1L)
                .claim("role", "MANAGER")
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

        String role = jwtUtil.extractRole(token);

        assertThat(role).isEqualTo("MANAGER");
    }

    @Test
    void isTokenValid_ShouldReturnTrue_WhenTokenIsValid() {
        String token = Jwts.builder()
                .claim("userId", 1L)
                .claim("role", "ADMIN")
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

        boolean isValid = jwtUtil.isTokenValid(token);

        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenTokenIsInvalid() {
        String invalidToken = "invalid.token.signature";

        boolean isValid = jwtUtil.isTokenValid(invalidToken);

        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenSignedWithDifferentKey() {
        SecretKey otherKey = Jwts.SIG.HS256.key().build();

        String token = Jwts.builder()
                .claim("userId", 1L)
                .claim("role", "DRIVER")
                .signWith(otherKey, Jwts.SIG.HS256)
                .compact();

        boolean isValid = jwtUtil.isTokenValid(token);

        assertThat(isValid).isFalse();
    }

    @Test
    void extractUserId_ShouldReturnUserId_WhenTokenIsValid() {
        String token = Jwts.builder()
                .claim("userId", 5L)
                .claim("role", "DRIVER")
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

        Long userId = jwtUtil.extractUserId(token);

        assertThat(userId).isEqualTo(5L);
    }
}
