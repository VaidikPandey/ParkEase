package com.parkease.gateway.filter;

import com.parkease.gateway.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthGlobalFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private ServerWebExchange exchange;
    @Mock
    private ServerHttpRequest request;
    @Mock
    private ServerHttpResponse response;
    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private JwtAuthGlobalFilter filter;

    @Test
    void filter_ShouldAllowPublicPaths_WithoutToken() {
        // Arrange
        when(exchange.getRequest()).thenReturn(request);
        when(request.getURI()).thenReturn(URI.create("/api/v1/auth/login"));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(exchange);
    }
@Test
void filter_ShouldForbidden_WhenNonAdminAccessesAdminPath() {
    // Arrange
    when(exchange.getRequest()).thenReturn(request);
    when(request.getURI()).thenReturn(URI.create("/api/v1/admin/users"));
    when(request.getHeaders()).thenReturn(new HttpHeaders());

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth("valid_token");
    when(request.getHeaders()).thenReturn(headers);

    when(jwtUtil.isTokenValid("valid_token")).thenReturn(true);
    when(jwtUtil.extractRole("valid_token")).thenReturn("DRIVER");

    when(exchange.getResponse()).thenReturn(response);
    when(response.getHeaders()).thenReturn(new HttpHeaders());
    when(response.bufferFactory()).thenReturn(new org.springframework.core.io.buffer.DefaultDataBufferFactory());
    when(response.writeWith(any())).thenReturn(Mono.empty());

    // Act
    Mono<Void> result = filter.filter(exchange, chain);

    // Assert
    StepVerifier.create(result).verifyComplete();
    verify(response).setStatusCode(HttpStatus.FORBIDDEN);
}
}
