package com.dating.recommendation.repository;

import com.dating.recommendation.model.InteractionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for InteractionHistory entity.
 */
@Repository
public interface InteractionHistoryRepository extends JpaRepository<InteractionHistory, UUID> {

    /**
     * Find all interactions for a user.
     *
     * @param userId User ID
     * @return List of interactions
     */
    List<InteractionHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find interactions by action type.
     *
     * @param userId User ID
     * @param action Action type
     * @return List of interactions
     */
    List<InteractionHistory> findByUserIdAndAction(UUID userId, String action);

    /**
     * Count interactions of a specific type within a time range.
     *
     * @param userId User ID
     * @param action Action type
     * @param since Start timestamp
     * @return Count of interactions
     */
    @Query("SELECT COUNT(ih) FROM InteractionHistory ih " +
           "WHERE ih.userId = :userId AND ih.action = :action " +
           "AND ih.createdAt >= :since")
    long countInteractionsSince(
            @Param("userId") UUID userId,
            @Param("action") String action,
            @Param("since") Instant since);

    /**
     * Find recent interactions with a specific target.
     *
     * @param userId User ID
     * @param targetId Target user ID
     * @return List of interactions
     */
    List<InteractionHistory> findByUserIdAndTargetIdOrderByCreatedAtDesc(UUID userId, UUID targetId);

    /**
     * Get interaction count for a user within a time period.
     *
     * @param userId User ID
     * @param since Start timestamp
     * @return Count of interactions
     */
    @Query("SELECT COUNT(ih) FROM InteractionHistory ih " +
           "WHERE ih.userId = :userId AND ih.createdAt >= :since")
    long countTotalInteractionsSince(
            @Param("userId") UUID userId,
            @Param("since") Instant since);

    /**
     * Count swipes for a user (LIKE, PASS, SUPER_LIKE actions).
     *
     * @param userId User ID
     * @param since Start timestamp
     * @return Count of swipes
     */
    @Query("SELECT COUNT(ih) FROM InteractionHistory ih " +
           "WHERE ih.userId = :userId " +
           "AND ih.action IN ('LIKE', 'PASS', 'SUPER_LIKE') " +
           "AND ih.createdAt >= :since")
    long countSwipesSince(
            @Param("userId") UUID userId,
            @Param("since") Instant since);

    /**
     * Count messages answered by a user.
     *
     * @param userId User ID
     * @return Count of message replies
     */
    @Query("SELECT COUNT(ih) FROM InteractionHistory ih " +
           "WHERE ih.userId = :userId AND ih.action = 'MESSAGE_SENT'")
    long countMessagesSent(@Param("userId") UUID userId);

    /**
     * Count messages received by a user.
     *
     * @param userId User ID
     * @return Count of messages received
     */
    @Query("SELECT COUNT(ih) FROM InteractionHistory ih " +
           "WHERE ih.targetId = :userId AND ih.action = 'MESSAGE_SENT'")
    long countMessagesReceived(@Param("userId") UUID userId);
}
