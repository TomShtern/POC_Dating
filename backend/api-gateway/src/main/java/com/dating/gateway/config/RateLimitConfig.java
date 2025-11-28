package com.dating.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Rate limiting configuration properties.
 *
 * Configures request limits for authenticated and anonymous users.
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Data
public class RateLimitConfig {

    /**
     * Requests per minute for authenticated users.
     * Default: 100 requests/minute
     */
    private int authenticatedLimit = 100;

    /**
     * Requests per minute for anonymous users.
     * Default: 30 requests/minute
     */
    private int anonymousLimit = 30;

    /**
     * Time window in seconds.
     * Default: 60 seconds (1 minute)
     */
    private int windowSeconds = 60;
}
