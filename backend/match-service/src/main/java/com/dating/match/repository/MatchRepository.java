package com.dating.match.repository;

import com.dating.match.model.Match;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Match entity operations.
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    /**
     * Find all active matches for a user (either as user1 or user2).
     *
     * @param userId User ID
     * @param pageable Pagination info
     * @return Page of matches
     */
    @Query("SELECT m FROM Match m WHERE (m.user1Id = :userId OR m.user2Id = :userId) " +
           "AND m.endedAt IS NULL ORDER BY m.matchedAt DESC")
    Page<Match> findActiveMatchesByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find all active matches for a user.
     *
     * @param userId User ID
     * @return List of matches
     */
    @Query("SELECT m FROM Match m WHERE (m.user1Id = :userId OR m.user2Id = :userId) " +
           "AND m.endedAt IS NULL ORDER BY m.matchedAt DESC")
    List<Match> findAllActiveMatchesByUserId(@Param("userId") UUID userId);

    /**
     * Find match between two specific users.
     *
     * @param user1Id First user ID (smaller UUID)
     * @param user2Id Second user ID (larger UUID)
     * @return Optional match
     */
    Optional<Match> findByUser1IdAndUser2Id(UUID user1Id, UUID user2Id);

    /**
     * Find active match between two users (in any order).
     *
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return Optional match
     */
    @Query("SELECT m FROM Match m WHERE " +
           "((m.user1Id = :userId1 AND m.user2Id = :userId2) OR " +
           "(m.user1Id = :userId2 AND m.user2Id = :userId1)) " +
           "AND m.endedAt IS NULL")
    Optional<Match> findActiveMatchBetweenUsers(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);

    /**
     * Check if an active match exists between two users.
     *
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return true if active match exists
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Match m WHERE " +
           "((m.user1Id = :userId1 AND m.user2Id = :userId2) OR " +
           "(m.user1Id = :userId2 AND m.user2Id = :userId1)) " +
           "AND m.endedAt IS NULL")
    boolean existsActiveMatchBetweenUsers(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);

    /**
     * Count active matches for a user.
     *
     * @param userId User ID
     * @return Count of active matches
     */
    @Query("SELECT COUNT(m) FROM Match m WHERE (m.user1Id = :userId OR m.user2Id = :userId) " +
           "AND m.endedAt IS NULL")
    long countActiveMatchesByUserId(@Param("userId") UUID userId);

    /**
     * Find match by ID ensuring the user is part of the match.
     *
     * @param matchId Match ID
     * @param userId User ID
     * @return Optional match
     */
    @Query("SELECT m FROM Match m WHERE m.id = :matchId AND " +
           "(m.user1Id = :userId OR m.user2Id = :userId)")
    Optional<Match> findByIdAndUserId(@Param("matchId") UUID matchId, @Param("userId") UUID userId);
}
