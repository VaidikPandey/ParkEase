package com.parkease.booking.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${gateway.url:http://localhost:8080}")
    private String gatewayUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .servers(List.of(new Server().url(resolveServerUrl()).description("API Gateway")))
                .info(new Info()
                        .title("ParkEase — Booking Service API")
                        .version("1.0.0")
                        .description("Booking lifecycle — create, check-in, check-out, cancel, extend"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    private String resolveServerUrl() {
        return gatewayUrl == null || gatewayUrl.isBlank() || gatewayUrl.contains("localhost")
                ? "/"
                : gatewayUrl;
    }
}
