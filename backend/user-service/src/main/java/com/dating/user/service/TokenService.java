package com.dating.user.service;

import com.dating.user.config.JwtConfig;
import com.dating.user.model.RefreshToken;
import com.dating.user.model.User;
import com.dating.user.repository.RefreshTokenRepository;
import com.dating.user.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
