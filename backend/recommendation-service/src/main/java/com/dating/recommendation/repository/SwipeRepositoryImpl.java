package com.dating.recommendation.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * ============================================================================
 * SWIPE REPOSITORY IMPLEMENTATION
 * ============================================================================
 *
 * PURPOSE:
 * Implementation of SwipeRepository using JDBC for cross-schema access.
 * Queries the swipes table to get swiped user IDs.
 *
 * WHY JDBC INSTEAD OF JPA:
 * - Swipes table may be in a different schema or database
 * - JPA would require entity mapping which couples services
 * - JDBC is simpler for read-only queries
 *
 * HOW TO MODIFY:
 * - For microservice isolation: Replace with Feign client to Match Service
 * - For better performance: Use Redis cache for swiped IDs
 * - For event-driven: Subscribe to swipe events and maintain local cache
 *
 * ============================================================================
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class SwipeRepositoryImpl implements SwipeRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get all user IDs that the given user has swiped on.
     *
     * QUERY:
     * SELECT target_user_id FROM swipes WHERE user_id = ?
     *
     * PERFORMANCE:
     * - Expects index on (user_id) column
     * - Returns only UUIDs for minimal data transfer
     *
     * @param userId The user requesting recommendations
     * @return Set of user IDs that have been swiped on
     */
    @Override
    public Set<UUID> findSwipedUserIds(UUID userId) {
        // =====================================================================
        // QUERY: Get all target user IDs from swipes by this user
        // =====================================================================
        // Uses parameterized query to prevent SQL injection
        // Returns target_user_id for all swipes (like, pass, super_like)
        String sql = "SELECT target_user_id FROM swipes WHERE user_id = ?";

        try {
            List<UUID> swipedIds = jdbcTemplate.queryForList(sql, UUID.class, userId);

            log.debug("Found {} swiped users for user {}", swipedIds.size(), userId);

            return new HashSet<>(swipedIds);
        } catch (Exception e) {
            // =====================================================================
            // ERROR HANDLING DECISION:
            // =====================================================================
            // Currently returns empty set for resilience (service keeps working).
            //
            // TRADE-OFF:
            // - PRO: Service works without Match Service dependency (useful in dev/test)
            // - CON: Users may see already-swiped profiles until fixed
            //
            // ALTERNATIVE (strict mode):
            // throw new RuntimeException("Failed to query swiped users", e);
            //
            // TODO: Add configuration to switch between resilient/strict modes
            // =====================================================================
            log.error("CRITICAL: Error querying swiped users for {}: {}. " +
                      "Users may see already-swiped profiles! Returning empty set.",
                    userId, e.getMessage());
            return new HashSet<>();
        }
    }
}
