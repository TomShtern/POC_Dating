package com.dating.match.repository;

import com.dating.common.constant.SwipeType;
import com.dating.match.model.Swipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Swipe entity operations.
 */
@Repository
public interface SwipeRepository extends JpaRepository<Swipe, UUID> {

    /**
     * Find a swipe by user and target user.
     *
     * @param userId User who swiped
     * @param targetUserId Target user
     * @return Optional swipe
     */
    Optional<Swipe> findByUserIdAndTargetUserId(UUID userId, UUID targetUserId);

    /**
     * Check if a swipe exists between two users.
     *
     * @param userId User who swiped
     * @param targetUserId Target user
     * @return true if swipe exists
     */
    boolean existsByUserIdAndTargetUserId(UUID userId, UUID targetUserId);

    /**
     * Find all swipes by a user.
     *
     * @param userId User ID
     * @return List of swipes
     */
    List<Swipe> findByUserId(UUID userId);

    /**
     * Find all user IDs that a user has already swiped on.
     *
     * @param userId User ID
     * @return List of target user IDs
     */
    @Query("SELECT s.targetUserId FROM Swipe s WHERE s.userId = :userId")
    List<UUID> findSwipedUserIdsByUserId(@Param("userId") UUID userId);

    /**
     * Check if there's a mutual like (target user has liked the current user).
     *
     * @param targetUserId Target user who may have liked
     * @param userId Current user who is being checked
     * @return Optional swipe if mutual like exists
     */
    @Query("SELECT s FROM Swipe s WHERE s.userId = :targetUserId AND s.targetUserId = :userId " +
           "AND s.action IN ('LIKE', 'SUPER_LIKE')")
    Optional<Swipe> findMutualLike(@Param("targetUserId") UUID targetUserId, @Param("userId") UUID userId);

    /**
     * Count swipes by user and action type.
     *
     * @param userId User ID
     * @param action Swipe action type
     * @return Count of swipes
     */
    long countByUserIdAndAction(UUID userId, SwipeType action);

    /**
     * Find all swipes on a target user.
     *
     * @param targetUserId Target user ID
     * @return List of swipes
     */
    List<Swipe> findByTargetUserId(UUID targetUserId);
}
