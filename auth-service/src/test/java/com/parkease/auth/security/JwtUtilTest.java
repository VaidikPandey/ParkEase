package com.parkease.auth.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "thisisasupersecretkeyforjwttestinganditshouldbeverylong";
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 3_600_000L, 86_400_000L);
    }

    @Test
    void generateAccessToken_ShouldContainClaims() {
        String token = jwtUtil.generateAccessToken("user@test.com", "DRIVER", 1L);

        assertThat(token).isNotBlank();
        Claims claims = jwtUtil.extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo("user@test.com");
        assertThat(claims.get("role", String.class)).isEqualTo("DRIVER");
        assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
    }

    @Test
    void generateRefreshToken_ShouldContainEmail() {
        String token = jwtUtil.generateRefreshToken("user@test.com");

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@test.com");
    }

    @Test
    void isTokenValid_ShouldReturnTrue_WhenTokenIsValid() {
        String token = jwtUtil.generateAccessToken("user@test.com", "ADMIN", 2L);

        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenTokenIsTampered() {
        assertThat(jwtUtil.isTokenValid("invalid.jwt.token")).isFalse();
    }

    @Test
    void extractEmail_ShouldReturnCorrectEmail() {
        String token = jwtUtil.generateAccessToken("admin@test.com", "ADMIN", 5L);

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("admin@test.com");
    }
}
