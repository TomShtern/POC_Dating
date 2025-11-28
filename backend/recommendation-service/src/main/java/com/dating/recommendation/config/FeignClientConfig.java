package com.dating.recommendation.config;

import feign.Logger;
import feign.Request;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign client configuration for inter-service communication.
 */
@Configuration
@Slf4j
public class FeignClientConfig {

    /**
     * Feign logger level.
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Request options with timeouts.
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                5, TimeUnit.SECONDS,    // connect timeout
                10, TimeUnit.SECONDS,   // read timeout
                true                     // follow redirects
        );
    }

    /**
     * Custom error decoder for Feign client errors.
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            log.error("Feign client error: {} - {}", response.status(), response.reason());

            return switch (response.status()) {
                case 404 -> new RuntimeException("User not found in user-service");
                case 503 -> new RuntimeException("User service unavailable");
                default -> new RuntimeException("Error calling user-service: " + response.reason());
            };
        };
    }
}
