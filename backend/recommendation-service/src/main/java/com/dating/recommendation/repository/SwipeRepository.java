package com.dating.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

/**
 * ============================================================================
 * SWIPE REPOSITORY
 * ============================================================================
 *
 * PURPOSE:
 * Data access layer for swipe history in the Recommendation Service.
 * Used to exclude already-swiped users from recommendations.
 *
 * NOTE ON DATA SOURCE:
 * Swipes are stored in the Match Service database.
 * For the recommendation service, we need read access to this data.
 *
 * OPTIONS FOR CROSS-SERVICE DATA ACCESS:
 * 1. Shared database (current approach) - Simple but couples services
 * 2. API call to Match Service - Decoupled but adds latency
 * 3. Event-driven sync - Match Service publishes swipes, we store locally
 * 4. Redis cache - Store swiped IDs in Redis for fast lookup
 *
 * HOW TO MODIFY:
 * If using separate databases, replace this with a Feign client
 * to call Match Service's API for swiped user IDs.
 *
 * ============================================================================
 */
@Repository
public interface SwipeRepository {

    /**
     * Get all user IDs that the given user has swiped on.
     *
     * PURPOSE:
     * Exclude already-swiped users from recommendations.
     * Users shouldn't see someone they've already swiped left or right on.
     *
     * PERFORMANCE:
     * - Uses index on user_id column
     * - Returns only IDs (not full swipe records)
     *
     * @param userId The user requesting recommendations
     * @return Set of user IDs that have been swiped on
     */
    Set<UUID> findSwipedUserIds(UUID userId);
}
