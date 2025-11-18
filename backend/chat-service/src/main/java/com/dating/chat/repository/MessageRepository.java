package com.dating.chat.repository;

import com.dating.chat.model.Message;
import com.dating.common.constant.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Message entity operations.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Find messages by match ID (conversation), ordered by creation time descending.
     * Excludes deleted messages.
     *
     * @param matchId Match/conversation ID
     * @param pageable Pagination parameters
     * @return Page of messages
     */
    @Query("SELECT m FROM Message m WHERE m.matchId = :matchId AND m.deletedAt IS NULL " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findByMatchIdOrderByCreatedAtDesc(
            @Param("matchId") UUID matchId,
            Pageable pageable);

    /**
     * Count unread messages for a user in a specific conversation.
     *
     * @param matchId Match/conversation ID
     * @param receiverId The user who should receive the messages
     * @return Count of unread messages
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.matchId = :matchId " +
           "AND m.receiverId = :receiverId AND m.status != 'READ' " +
           "AND m.deletedAt IS NULL")
    long countUnreadByMatchIdAndReceiverId(
            @Param("matchId") UUID matchId,
            @Param("receiverId") UUID receiverId);

    /**
     * Count total unread messages for a user across all conversations.
     *
     * @param receiverId The user who should receive the messages
     * @return Count of unread messages
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiverId = :receiverId " +
           "AND m.status != 'READ' AND m.deletedAt IS NULL")
    long countUnreadByReceiverId(@Param("receiverId") UUID receiverId);

    /**
     * Find the last message in a conversation.
     *
     * @param matchId Match/conversation ID
     * @return Optional last message
     */
    @Query("SELECT m FROM Message m WHERE m.matchId = :matchId AND m.deletedAt IS NULL " +
           "ORDER BY m.createdAt DESC LIMIT 1")
    Message findLastMessageByMatchId(@Param("matchId") UUID matchId);

    /**
     * Find all match IDs where a user has messages (for conversation list).
     *
     * @param userId User ID
     * @return List of match IDs
     */
    @Query("SELECT DISTINCT m.matchId FROM Message m " +
           "WHERE (m.senderId = :userId OR m.receiverId = :userId) " +
           "AND m.deletedAt IS NULL")
    List<UUID> findMatchIdsByUserId(@Param("userId") UUID userId);

    /**
     * Mark all messages as read for a user in a conversation.
     *
     * @param matchId Match/conversation ID
     * @param receiverId User who reads the messages
     * @param readAt Timestamp when marked as read
     * @return Number of updated messages
     */
    @Modifying
    @Query("UPDATE Message m SET m.status = 'READ', m.readAt = :readAt " +
           "WHERE m.matchId = :matchId AND m.receiverId = :receiverId " +
           "AND m.status != 'READ' AND m.deletedAt IS NULL")
    int markAllAsRead(
            @Param("matchId") UUID matchId,
            @Param("receiverId") UUID receiverId,
            @Param("readAt") Instant readAt);

    /**
     * Find messages by status.
     *
     * @param status Message status
     * @param pageable Pagination parameters
     * @return Page of messages
     */
    Page<Message> findByStatusAndDeletedAtIsNull(MessageStatus status, Pageable pageable);

    /**
     * Check if conversation exists between users.
     *
     * @param matchId Match/conversation ID
     * @return true if messages exist
     */
    boolean existsByMatchId(UUID matchId);

    /**
     * Find messages created after a specific time in a conversation.
     * Useful for real-time sync.
     *
     * @param matchId Match/conversation ID
     * @param after Timestamp
     * @return List of messages
     */
    @Query("SELECT m FROM Message m WHERE m.matchId = :matchId " +
           "AND m.createdAt > :after AND m.deletedAt IS NULL " +
           "ORDER BY m.createdAt ASC")
    List<Message> findByMatchIdAndCreatedAtAfter(
            @Param("matchId") UUID matchId,
            @Param("after") Instant after);
}
