package com.parkease.analytics.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ParkEase Analytics Service API")
                        .version("1.0.0")
                        .description("Parking occupancy, utilisation, traffic, and revenue analytics")
                        .contact(new Contact().name("ParkEase Team").email("dev@parkease.com")));
    }
}
