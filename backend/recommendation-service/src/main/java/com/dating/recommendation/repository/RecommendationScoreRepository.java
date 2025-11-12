package com.dating.recommendation.repository;

import com.dating.recommendation.entity.RecommendationScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RecommendationScore entity.
 * Provides database access methods for cached recommendation scores.
 */
@Repository
public interface RecommendationScoreRepository extends JpaRepository<RecommendationScore, Long> {

    /**
     * Find a cached score for a specific user-candidate pair.
     *
     * @param userId the user ID
     * @param candidateUserId the candidate user ID
     * @return Optional containing the score if found and valid
     */
    Optional<RecommendationScore> findByUserIdAndCandidateUserIdAndValidTrue(Long userId, Long candidateUserId);

    /**
     * Find all valid, non-shown recommendations for a user, ordered by score descending.
     *
     * @param userId the user ID
     * @param expiresAfter only return scores that expire after this time
     * @return List of recommendation scores
     */
    @Query("SELECT rs FROM RecommendationScore rs WHERE rs.userId = :userId " +
           "AND rs.valid = true AND rs.shown = false " +
           "AND (rs.expiresAt IS NULL OR rs.expiresAt > :expiresAfter) " +
           "ORDER BY rs.score DESC")
    List<RecommendationScore> findValidUnshownRecommendations(
            @Param("userId") Long userId,
            @Param("expiresAfter") LocalDateTime expiresAfter);

    /**
     * Mark recommendations as shown.
     *
     * @param ids the IDs of recommendations to mark as shown
     * @param shownAt the timestamp when they were shown
     */
    @Modifying
    @Query("UPDATE RecommendationScore rs SET rs.shown = true, rs.shownAt = :shownAt " +
           "WHERE rs.id IN :ids")
    void markAsShown(@Param("ids") List<Long> ids, @Param("shownAt") LocalDateTime shownAt);

    /**
     * Invalidate all cached scores for a user (when their preferences change).
     *
     * @param userId the user ID
     */
    @Modifying
    @Query("UPDATE RecommendationScore rs SET rs.valid = false WHERE rs.userId = :userId")
    void invalidateScoresForUser(@Param("userId") Long userId);

    /**
     * Invalidate all cached scores involving a candidate user (when their profile changes).
     *
     * @param candidateUserId the candidate user ID
     */
    @Modifying
    @Query("UPDATE RecommendationScore rs SET rs.valid = false WHERE rs.candidateUserId = :candidateUserId")
    void invalidateScoresForCandidate(@Param("candidateUserId") Long candidateUserId);

    /**
     * Delete expired scores.
     *
     * @param before delete scores that expired before this time
     */
    @Modifying
    @Query("DELETE FROM RecommendationScore rs WHERE rs.expiresAt < :before")
    void deleteExpiredScores(@Param("before") LocalDateTime before);

    /**
     * Get top N recommendations for a user.
     *
     * @param userId the user ID
     * @param limit maximum number of recommendations
     * @return List of top recommendation scores
     */
    @Query("SELECT rs FROM RecommendationScore rs WHERE rs.userId = :userId " +
           "AND rs.valid = true ORDER BY rs.score DESC")
    List<RecommendationScore> findTopRecommendations(@Param("userId") Long userId,
                                                      @Param("limit") int limit);
}
