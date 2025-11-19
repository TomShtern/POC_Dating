package com.dating.chat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.UUID;

/**
 * JWT Token Provider
 *
 * Validates JWT tokens for WebSocket authentication.
 * Note: Token generation is done by User Service only.
 * This service only validates tokens.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    /**
     * Get the signing key from the secret.
     */
    private SecretKey getSigningKey() {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT secret must be configured");
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Validate a JWT token.
     *
     * @param token The JWT token to validate
     * @return The claims if valid
     * @throws Exception if token is invalid
     */
    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Check if a token is valid without throwing exceptions.
     *
     * @param token The JWT token to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidToken(String token) {
        try {
            validateToken(token);
            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extract user ID from token.
     *
     * @param token The JWT token
     * @return The user ID
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        String subject = claims.getSubject();
        if (subject == null || subject.isEmpty()) {
            throw new IllegalArgumentException("Token does not contain a valid user ID");
        }
        return UUID.fromString(subject);
    }

    /**
     * Extract username from token.
     *
     * @param token The JWT token
     * @return The username, or empty string if not present
     */
    public String getUsernameFromToken(String token) {
        Claims claims = validateToken(token);
        String username = claims.get("username", String.class);
        return username != null ? username : "";
    }
}
