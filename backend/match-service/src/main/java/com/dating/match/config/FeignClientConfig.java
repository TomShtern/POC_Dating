package com.dating.match.config;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * Feign client configuration for inter-service communication.
 * Handles request forwarding, timeouts, and error decoding.
 */
@Configuration
@Slf4j
public class FeignClientConfig {

    /**
     * Request interceptor to forward headers to other services.
     * Forwards the X-User-Id header for authorization context.
     */
    @Bean
    RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();

            if (attributes != null) {
                String userId = attributes.getRequest().getHeader("X-User-Id");
                if (userId != null) {
                    requestTemplate.header("X-User-Id", userId);
                }

                // Forward correlation ID for distributed tracing
                String correlationId = attributes.getRequest().getHeader("X-Correlation-Id");
                if (correlationId != null) {
                    requestTemplate.header("X-Correlation-Id", correlationId);
                }
            }
        };
    }

    /**
     * Request options with timeouts to prevent hanging connections.
     */
    @Bean
    Request.Options requestOptions() {
        return new Request.Options(
                5, TimeUnit.SECONDS, // connect timeout
                10, TimeUnit.SECONDS, // read timeout
                true // follow redirects
        );
    }

    /**
     * Custom error decoder for Feign client errors.
     * Prevents unhandled feign.FeignException from cascading.
     */
    @Bean
    ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            log.error("Feign client error calling {}: {} - {}",
                    methodKey, response.status(), response.reason());

            return switch (response.status()) {
                case 404 -> new RuntimeException("Resource not found: " + response.reason());
                case 503 -> new RuntimeException("Downstream service unavailable");
                case 500 -> new RuntimeException("Downstream service error: " + response.reason());
                default -> new RuntimeException("Error in downstream call: " + response.reason());
            };
        };
    }

    /**
     * Feign logger level for debugging.
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
