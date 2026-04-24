package com.parkease.notification.config;

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
                        .title("ParkEase Notification & Analytics Service API")
                        .version("1.0.0")
                        .description("In-app, email, SMS notifications + occupancy analytics and peak hour reports")
                        .contact(new Contact().name("ParkEase Team").email("dev@parkease.com")));
    }
}
