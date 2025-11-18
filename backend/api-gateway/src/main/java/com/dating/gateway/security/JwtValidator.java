package com.dating.gateway.security;

import com.dating.gateway.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.UUID;

/**
 * JWT token validator for the API Gateway.
 *
 * Validates tokens using the same secret as the user-service.
 * Extracts user ID for downstream service headers.
 * Checks token blacklist in Redis for revoked tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtValidator {

    private final JwtConfig jwtConfig;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * Get the signing key from the base64 encoded secret.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Validate JWT token signature and expiration.
     *
     * @param token JWT token string
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);

            // Check if token is blacklisted
            String jti = claims.getId();
            if (jti != null && isTokenBlacklisted(jti)) {
                log.warn("Token is blacklisted: {}", jti);
                return false;
            }

            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Check if a token is blacklisted.
     *
     * @param jti Token ID (jti claim)
     * @return true if blacklisted
     */
    private boolean isTokenBlacklisted(String jti) {
        try {
            Boolean exists = redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + jti);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Error checking token blacklist: {}", e.getMessage());
            return false; // Fail open - if Redis is down, allow the token
        }
    }

    /**
     * Extract user ID from JWT token.
     *
     * @param token JWT token string
     * @return User ID as UUID
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Check if token is an access token (not refresh token).
     *
     * @param token JWT token string
     * @return true if it's an access token
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = parseToken(token);
            String type = claims.get("type", String.class);
            return "access".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parse and validate JWT token.
     *
     * @param token JWT token string
     * @return Claims from token
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
