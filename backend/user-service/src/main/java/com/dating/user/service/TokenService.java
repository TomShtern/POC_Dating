package com.dating.user.service;

import com.dating.user.config.JwtConfig;
import com.dating.user.model.RefreshToken;
import com.dating.user.model.User;
import com.dating.user.repository.RefreshTokenRepository;
import com.dating.user.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Service for managing JWT tokens and refresh tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * Generate access token for user.
     *
     * @param userId User UUID
     * @return Access token string
     */
    public String generateAccessToken(UUID userId) {
        return jwtTokenProvider.generateAccessToken(userId);
    }

    /**
     * Generate and persist refresh token for user.
     *
     * @param user User entity
     * @return Refresh token string
     */
    @Transactional
    public String createRefreshToken(User user) {
        String token = jwtTokenProvider.generateRefreshToken(user.getId());

        // Hash the token before storing
        String tokenHash = passwordEncoder.encode(token);

        Instant expiresAt = Instant.now().plusMillis(jwtConfig.getRefreshTokenExpiration());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(refreshToken);
        log.debug("Created refresh token for user: {}", user.getId());

        return token;
    }

    /**
     * Validate refresh token.
     *
     * @param token Refresh token string
     * @return true if valid
     */
    public boolean validateRefreshToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            return false;
        }

        if (!jwtTokenProvider.isRefreshToken(token)) {
            log.warn("Token is not a refresh token");
            return false;
        }

        return true;
    }

    /**
     * Get user ID from refresh token.
     *
     * @param token Refresh token
     * @return User UUID
     */
    public UUID getUserIdFromRefreshToken(String token) {
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    /**
     * Revoke all refresh tokens for a user.
     *
     * @param userId User UUID
     */
    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        int revoked = refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        log.info("Revoked {} refresh tokens for user: {}", revoked, userId);
    }

    /**
     * Blacklist an access token by storing its jti in Redis.
     * The token will be blacklisted until it expires.
     *
     * @param token Access token to blacklist
     */
    public void blacklistAccessToken(String token) {
        try {
            // Get token ID and expiration from the token
            Date expiration = jwtTokenProvider.getExpirationFromToken(token);
            UUID userId = jwtTokenProvider.getUserIdFromToken(token);

            // Parse the token to get jti - we need to extract it from the claims
            // Using reflection or parsing directly since we need the jti
            io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String jti = claims.getId();
            if (jti == null) {
                log.warn("Cannot blacklist token - no jti claim present for user: {}", userId);
                return;
            }

            // Calculate TTL as remaining token lifetime
            long ttlMillis = expiration.getTime() - System.currentTimeMillis();
            if (ttlMillis <= 0) {
                log.debug("Token already expired, no need to blacklist for user: {}", userId);
                return;
            }

            // Store jti in Redis with TTL
            redisTemplate.opsForValue().set(
                    TOKEN_BLACKLIST_PREFIX + jti,
                    userId.toString(),
                    Duration.ofMillis(ttlMillis)
            );

            log.info("Blacklisted access token with jti {} for user {}, TTL: {}ms", jti, userId, ttlMillis);
        } catch (Exception e) {
            log.error("Failed to blacklist access token: {}", e.getMessage(), e);
            // Don't rethrow - logout should still succeed even if blacklisting fails
        }
    }

    /**
     * Get the signing key from the JWT config.
     */
    private javax.crypto.SecretKey getSigningKey() {
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(jwtConfig.getSecret());
        return io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Get access token expiration in seconds.
     *
     * @return Expiration time in seconds
     */
    public long getAccessTokenExpirationSeconds() {
        return jwtTokenProvider.getAccessTokenExpirationSeconds();
    }

    /**
     * Scheduled task to clean up expired refresh tokens.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredTokens(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired refresh tokens", deleted);
        }
    }
}
