package com.dating.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for JWT token validation.
 * Must use the same secret as the user-service.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {

    /**
     * Secret key for validating JWT tokens.
     * Must match the secret used by user-service for token generation.
     */
    private String secret;

    /**
     * Token issuer name for validation.
     */
    private String issuer = "poc-dating-user-service";
}
