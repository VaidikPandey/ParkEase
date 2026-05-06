package com.parkease.gateway.filter;

import com.parkease.gateway.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/oauth2/",
            "/login/oauth2/",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars/"
    );

    private static final List<String> ADMIN_PATHS = List.of(
            "/api/v1/admin/",
            "/api/v1/parking/admin/",
            "/api/v1/bookings/admin/",
            "/api/v1/payments/admin/",
            "/api/v1/notifications/admin/",
            "/api/v1/analytics/"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // Prefer Authorization header; fall back to HttpOnly access_token cookie
        String token = null;
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            var cookie = exchange.getRequest().getCookies().getFirst("access_token");
            if (cookie != null) token = cookie.getValue();
        }

        if (token == null) {
            return unauthorizedResponse(exchange, "Missing Authorization header");
        }

        if (!jwtUtil.isTokenValid(token)) {
            return unauthorizedResponse(exchange, "Invalid or expired token");
        }

        Long userId = jwtUtil.extractUserId(token);
        String role  = jwtUtil.extractRole(token);

        if (!"ADMIN".equals(role) && ADMIN_PATHS.stream().anyMatch(path::startsWith)) {
            return forbiddenResponse(exchange, "Access denied: admin only");
        }

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Role", role)
                .build();

        log.debug("Forwarding request: path={} userId={} role={}", path, userId, role);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        return errorResponse(exchange, HttpStatus.UNAUTHORIZED, message);
    }

    private Mono<Void> forbiddenResponse(ServerWebExchange exchange, String message) {
        return errorResponse(exchange, HttpStatus.FORBIDDEN, message);
    }

    private Mono<Void> errorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String body = "{\"status\":" + status.value() + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"message\":\"" + message + "\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes());
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
