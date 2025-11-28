package com.dating.recommendation.repository;

import com.dating.recommendation.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * ============================================================================
 * USER REPOSITORY
 * ============================================================================
 *
 * PURPOSE:
 * Data access layer for User entities in the Recommendation Service.
 * Provides methods to query candidate users for recommendation generation.
 *
 * NOTE ON DATA SOURCE:
 * This repository reads from the same database as User Service.
 * User Service is the source of truth; this is read-only access.
 *
 * KEY QUERIES:
 * - findCandidates: Get users not yet swiped on
 * - findById: Get specific user details
 * - findActiveUsers: Get recently active users
 *
 * PERFORMANCE CONSIDERATIONS:
 * - All queries should use indexes (foreign keys, created_at, etc.)
 * - Use pagination for large result sets
 * - Consider Redis caching for hot data
 *
 * HOW TO MODIFY:
 * - Add new query methods for different filtering strategies
 * - Use @EntityGraph to fetch relationships eagerly
 * - Add pagination support with Pageable parameter
 *
 * ============================================================================
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find candidate users for recommendations.
     *
     * PURPOSE:
     * Get all users who are:
     * - Active (account not deleted/disabled)
     * - Not in the exclude set (already swiped, blocked, self)
     *
     * PERFORMANCE:
     * - Uses index on 'active' column
     * - Exclude set comparison is O(n) but typically small
     *
     * HOW TO MODIFY:
     * - Add pagination: Add Pageable parameter, return Page<User>
     * - Add sorting: ORDER BY lastActiveAt DESC (prefer active users)
     * - Add filters: WHERE gender IN (:genders) AND age BETWEEN :min AND :max
     *
     * @param excludeIds Set of user IDs to exclude (already swiped, blocked, self)
     * @return List of candidate users
     */
    @EntityGraph(attributePaths = {"genderPreferences", "interests"})
    @Query("SELECT u FROM User u WHERE u.active = true AND u.id NOT IN :excludeIds")
    List<User> findCandidates(@Param("excludeIds") Set<UUID> excludeIds);

    /**
     * Find candidate users with basic preference filtering.
     *
     * PURPOSE:
     * Pre-filter candidates at database level for better performance.
     * Only returns users who:
     * - Are active
     * - Not in exclude set
     * - Match basic gender/age criteria
     *
     * WHY PRE-FILTER:
     * Reduces the number of users that need to be scored.
     * More efficient than scoring everyone and filtering.
     *
     * @param excludeIds Users to exclude
     * @param userGender User's gender (to match candidate's preferences)
     * @param userAge User's age (to match candidate's age range preferences)
     * @param genderPreferences User's desired genders
     * @return Pre-filtered candidate list
     */
    @EntityGraph(attributePaths = {"genderPreferences", "interests"})
    @Query("""
           SELECT u FROM User u
           WHERE u.active = true
             AND u.id NOT IN :excludeIds
             AND u.gender IN :genderPreferences
             AND :userAge BETWEEN u.minAgePreference AND u.maxAgePreference
             AND :userGender IN elements(u.genderPreferences)
           """)
    List<User> findPreFilteredCandidates(
            @Param("excludeIds") Set<UUID> excludeIds,
            @Param("userGender") String userGender,
            @Param("userAge") int userAge,
            @Param("genderPreferences") Set<String> genderPreferences);

    /**
     * Find recently active users.
     *
     * PURPOSE:
     * Get users who have been active within a time window.
     * Useful for prioritizing active users in recommendations.
     *
     * @param activeSinceDays Number of days to look back
     * @return List of recently active users
     */
    @EntityGraph(attributePaths = {"genderPreferences", "interests"})
    @Query("""
           SELECT u FROM User u
           WHERE u.active = true
             AND u.lastActiveAt >= CURRENT_TIMESTAMP - :activeSinceDays * INTERVAL '1 day'
           ORDER BY u.lastActiveAt DESC
           """)
    List<User> findRecentlyActiveUsers(@Param("activeSinceDays") int activeSinceDays);

    /**
     * Count total active users.
     *
     * PURPOSE:
     * Get count for monitoring and analytics.
     *
     * @return Number of active users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    long countActiveUsers();
}
