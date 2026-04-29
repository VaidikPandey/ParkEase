package com.parkease.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtCookieFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractFromCookie(request, "access_token");

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
                    filterChain.doFilter(request, response);
                    return;
                }
                if (jwtUtil.isTokenValid(token)) {
                    var claims = jwtUtil.extractClaims(token);
                    String role   = claims.get("role", String.class);
                    Long   userId = claims.get("userId", Long.class);

                    var auth = new UsernamePasswordAuthenticationToken(
                            String.valueOf(userId), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                log.warn("JWT cookie validation failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    public static String extractFromCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
