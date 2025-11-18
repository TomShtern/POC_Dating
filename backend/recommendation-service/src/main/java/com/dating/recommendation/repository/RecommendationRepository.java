package com.dating.recommendation.repository;

import com.dating.recommendation.model.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Recommendation entity.
 */
@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    /**
     * Find recommendations for a user, ordered by score descending.
     *
     * @param userId User ID
     * @return List of recommendations sorted by score
     */
    List<Recommendation> findByUserIdOrderByScoreDesc(UUID userId);

    /**
     * Find active (non-expired) recommendations for a user.
     *
     * @param userId User ID
     * @param now Current timestamp
     * @return List of active recommendations
     */
    @Query("SELECT r FROM Recommendation r WHERE r.userId = :userId " +
           "AND (r.expiresAt IS NULL OR r.expiresAt > :now) " +
           "ORDER BY r.score DESC")
    List<Recommendation> findActiveRecommendations(
            @Param("userId") UUID userId,
            @Param("now") Instant now);

    /**
     * Find a specific recommendation between two users.
     *
     * @param userId Source user ID
     * @param recommendedUserId Target user ID
     * @return Optional recommendation
     */
    Optional<Recommendation> findByUserIdAndRecommendedUserId(UUID userId, UUID recommendedUserId);

    /**
     * Find recommendations by algorithm version.
     *
     * @param userId User ID
     * @param algorithmVersion Algorithm version
     * @return List of recommendations
     */
    List<Recommendation> findByUserIdAndAlgorithmVersion(UUID userId, String algorithmVersion);

    /**
     * Delete expired recommendations.
     *
     * @param expirationTime Expiration threshold
     * @return Number of deleted records
     */
    @Modifying
    @Query("DELETE FROM Recommendation r WHERE r.expiresAt < :expirationTime")
    int deleteExpiredRecommendations(@Param("expirationTime") Instant expirationTime);

    /**
     * Delete all recommendations for a user.
     *
     * @param userId User ID
     * @return Number of deleted records
     */
    @Modifying
    int deleteByUserId(UUID userId);

    /**
     * Check if recommendations exist for a user.
     *
     * @param userId User ID
     * @return True if recommendations exist
     */
    boolean existsByUserId(UUID userId);

    /**
     * Count recommendations for a user.
     *
     * @param userId User ID
     * @return Number of recommendations
     */
    long countByUserId(UUID userId);
}
