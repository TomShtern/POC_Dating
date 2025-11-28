package com.dating.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for JWT tokens.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {

    /**
     * Secret key for signing JWT tokens.
     * Should be at least 256 bits for HS256 algorithm.
     */
    private String secret = "your-256-bit-secret-key-for-jwt-token-signing-must-be-long-enough";

    /**
     * Access token expiration time in milliseconds.
     * Default: 15 minutes (900000ms)
     */
    private long accessTokenExpiration = 900000;

    /**
     * Refresh token expiration time in milliseconds.
     * Default: 7 days (604800000ms)
     */
    private long refreshTokenExpiration = 604800000;

    /**
     * Token issuer name.
     */
    private String issuer = "poc-dating-user-service";

    /**
     * Get access token expiration in seconds.
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration / 1000;
    }
}
