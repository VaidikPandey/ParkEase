package com.parkease.payment.config;

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
                        .title("ParkEase Payment Service API")
                        .version("1.0.0")
                        .description("Handles payment processing, refunds, PDF receipts, and revenue reports")
                        .contact(new Contact().name("ParkEase Team").email("dev@parkease.com")));
    }
}
