package com.dating.match.repository;

import com.dating.match.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Match entity operations
 *
 * PURPOSE: Data access layer for match records
 *
 * SPRING DATA JPA:
 * - Extends JpaRepository for CRUD operations
 * - Query methods follow naming conventions
 * - Custom queries for complex lookups
 * - Automatic transaction management
 *
 * WHY THESE METHODS:
 * - findByUser1IdAndUser2Id: Find match by ordered user IDs
 * - findActiveMatchesByUserId: Get all active matches for a user
 * - existsActiveMatch: Quick check if match exists
 * - countActiveMatchesByUserId: Count user's matches
 *
 * BIDIRECTIONAL MATCHING:
 * - Match records store (user1Id, user2Id) where user1Id < user2Id
 * - Queries must check BOTH user1Id and user2Id columns
 * - OR conditions handle bidirectionality
 *
 * PERFORMANCE CONSIDERATIONS:
 * - Indexes on user1_id, user2_id, is_active (defined in entity)
 * - @Query optimizes for bidirectional lookups
 * - Consider caching active matches in Redis
 *
 * ALTERNATIVES:
 * - Store two rows per match: Wastes space, harder to maintain
 * - Separate table for each user: Doesn't scale
 * - NoSQL: Better for high-volume, but loses ACID guarantees
 *
 * RATIONALE:
 * - Relational model is perfect for match relationships
 * - Composite unique constraint prevents duplicates
 * - isActive flag enables soft delete (analytics)
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    /**
     * Find match by ordered user IDs
     * Used to check if match already exists
     */
    Optional<Match> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);

    /**
     * Find all active matches for a user (bidirectional)
     * Returns matches where user is either user1 or user2
     */
    @Query("SELECT m FROM Match m WHERE (m.user1Id = :userId OR m.user2Id = :userId) AND m.isActive = true ORDER BY m.matchedAt DESC")
    List<Match> findActiveMatchesByUserId(@Param("userId") Long userId);

    /**
     * Find all matches (active and inactive) for a user
     * Used for analytics and match history
     */
    @Query("SELECT m FROM Match m WHERE (m.user1Id = :userId OR m.user2Id = :userId) ORDER BY m.matchedAt DESC")
    List<Match> findAllMatchesByUserId(@Param("userId") Long userId);

    /**
     * Check if active match exists between two users
     * Used for quick validation
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Match m " +
           "WHERE ((m.user1Id = :userId1 AND m.user2Id = :userId2) OR " +
           "(m.user1Id = :userId2 AND m.user2Id = :userId1)) AND m.isActive = true")
    boolean existsActiveMatch(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * Find active match between two users (bidirectional)
     * Used to get match details for unmatch operation
     */
    @Query("SELECT m FROM Match m WHERE ((m.user1Id = :userId1 AND m.user2Id = :userId2) OR " +
           "(m.user1Id = :userId2 AND m.user2Id = :userId1)) AND m.isActive = true")
    Optional<Match> findActiveMatchBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * Count active matches for a user
     * Used for profile stats and limits
     */
    @Query("SELECT COUNT(m) FROM Match m WHERE (m.user1Id = :userId OR m.user2Id = :userId) AND m.isActive = true")
    long countActiveMatchesByUserId(@Param("userId") Long userId);

    /**
     * Find all active matches (for admin/analytics)
     */
    List<Match> findByIsActiveTrue();
}
