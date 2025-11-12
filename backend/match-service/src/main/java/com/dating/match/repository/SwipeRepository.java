package com.dating.match.repository;

import com.dating.match.entity.Swipe;
import com.dating.match.entity.SwipeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Swipe entity operations
 *
 * PURPOSE: Data access layer for swipe records
 *
 * SPRING DATA JPA:
 * - Extends JpaRepository for CRUD operations
 * - Provides query methods by naming convention
 * - Custom queries via @Query annotation
 * - Automatic transaction management
 *
 * WHY THESE METHODS:
 * - findByUserIdAndTargetUserId: Check if swipe already exists (prevent duplicates)
 * - findByTargetUserIdAndUserId: Check if target liked current user (detect matches)
 * - findByUserId: Get user's swipe history
 * - findSwipedUserIds: Get IDs of users already swiped on (exclude from feed)
 * - existsByUserIdAndTargetUserId: Quick check without loading entity
 *
 * PERFORMANCE CONSIDERATIONS:
 * - Indexes on user_id, target_user_id, timestamp (defined in entity)
 * - Query methods use indexes for fast lookups
 * - @Query for complex queries with explicit SQL
 * - Consider caching for frequently accessed data
 *
 * ALTERNATIVES:
 * - Native SQL: Less portable, but sometimes faster
 * - Criteria API: More complex, but type-safe
 * - QueryDSL: Better for dynamic queries
 *
 * RATIONALE:
 * - Spring Data JPA provides 80% of needed functionality
 * - Query methods are self-documenting
 * - Easy to test with in-memory database
 */
@Repository
public interface SwipeRepository extends JpaRepository<Swipe, Long> {

    /**
     * Find swipe by user and target user
     * Used to check if user already swiped on target
     */
    Optional<Swipe> findByUserIdAndTargetUserId(Long userId, Long targetUserId);

    /**
     * Check if swipe exists (faster than findBy)
     * Used for duplicate prevention
     */
    boolean existsByUserIdAndTargetUserId(Long userId, Long targetUserId);

    /**
     * Find reverse swipe (target user's swipe on current user)
     * Used to detect mutual matches
     */
    Optional<Swipe> findByUserIdAndTargetUserIdAndSwipeType(
        Long userId,
        Long targetUserId,
        SwipeType swipeType
    );

    /**
     * Get all swipes by a user
     * Used for swipe history and analytics
     */
    List<Swipe> findByUserIdOrderByTimestampDesc(Long userId);

    /**
     * Get IDs of all users that this user has swiped on
     * Used to exclude already-swiped profiles from recommendation feed
     */
    @Query("SELECT s.targetUserId FROM Swipe s WHERE s.userId = :userId")
    List<Long> findSwipedUserIds(@Param("userId") Long userId);

    /**
     * Get IDs of users who liked this user (swiped right)
     * Used for "Who Liked You" premium feature
     */
    @Query("SELECT s.userId FROM Swipe s WHERE s.targetUserId = :targetUserId AND s.swipeType = 'LIKE'")
    List<Long> findUsersWhoLiked(@Param("targetUserId") Long targetUserId);

    /**
     * Count swipes by user (for rate limiting)
     * Used to enforce swipe limits
     */
    long countByUserId(Long userId);
}
