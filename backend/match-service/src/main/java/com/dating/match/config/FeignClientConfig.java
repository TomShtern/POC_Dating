package com.dating.match.config;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign client configuration for inter-service communication.
 */
@Configuration
public class FeignClientConfig {

    /**
     * Request interceptor to forward headers to other services.
     * Forwards the X-User-Id header for authorization context.
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

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
     * Feign logger level for debugging.
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
