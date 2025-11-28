package com.dating.user.repository;

import com.dating.user.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RefreshToken entity.
 * Handles all database operations for refresh tokens.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find token by hash.
     *
     * @param tokenHash Token hash
     * @return Optional containing token if found
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Find valid (not revoked and not expired) token by hash.
     *
     * @param tokenHash Token hash
     * @param now Current time for expiry check
     * @return Optional containing token if valid
     */
    @Query("SELECT t FROM RefreshToken t WHERE t.tokenHash = :tokenHash AND t.revoked = false AND t.expiresAt > :now")
    Optional<RefreshToken> findValidByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    /**
     * Revoke all tokens for a user.
     *
     * @param userId User UUID
     * @param revokedAt Revocation timestamp
     * @return Number of tokens revoked
     */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true, t.revokedAt = :revokedAt WHERE t.user.id = :userId AND t.revoked = false")
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);

    /**
     * Delete expired tokens.
     *
     * @param now Current time
     * @return Number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    /**
     * Check if user has any valid tokens.
     *
     * @param userId User UUID
     * @param now Current time
     * @return true if user has valid tokens
     */
    @Query("SELECT COUNT(t) > 0 FROM RefreshToken t WHERE t.user.id = :userId AND t.revoked = false AND t.expiresAt > :now")
    boolean hasValidTokens(@Param("userId") UUID userId, @Param("now") Instant now);
}
