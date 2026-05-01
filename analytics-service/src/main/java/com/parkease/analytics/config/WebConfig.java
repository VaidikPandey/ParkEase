package com.parkease.analytics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebConfig {

    @Value("${services.payment-url}")
    private String paymentServiceUrl;

    @Bean
    public WebClient paymentWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(paymentServiceUrl)
                .build();
    }
}
