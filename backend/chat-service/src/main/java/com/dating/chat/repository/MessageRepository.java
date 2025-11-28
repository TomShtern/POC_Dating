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
     * Unread messages are those sent by the OTHER user (not the current user).
     *
     * @param matchId Match/conversation ID
     * @param userId The current user (receiver of messages from others)
     * @return Count of unread messages
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.matchId = :matchId " +
           "AND m.senderId != :userId AND m.status != 'READ' " +
           "AND m.deletedAt IS NULL")
    long countUnreadByMatchIdAndUserId(
            @Param("matchId") UUID matchId,
            @Param("userId") UUID userId);

    /**
     * Count total unread messages for a user across all conversations.
     * Since we don't store receiverId, this counts messages where the user is NOT the sender.
     * Note: This requires knowing which matchIds the user is a participant of.
     * For a proper implementation, we need to join with match participants data.
     *
     * @param userId The current user
     * @param matchIds List of match IDs the user participates in
     * @return Count of unread messages
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.matchId IN :matchIds " +
           "AND m.senderId != :userId AND m.status != 'READ' AND m.deletedAt IS NULL")
    long countUnreadByUserIdAndMatchIds(
            @Param("userId") UUID userId,
            @Param("matchIds") List<UUID> matchIds);

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
     * Find all match IDs where a user has sent messages.
     * Note: Without receiverId stored, this only finds conversations where the user sent at least one message.
     * TODO: For a complete conversation list, match participant data should be fetched from Match Service.
     *
     * @param userId User ID
     * @return List of match IDs
     */
    @Query("SELECT DISTINCT m.matchId FROM Message m " +
           "WHERE m.senderId = :userId AND m.deletedAt IS NULL")
    List<UUID> findMatchIdsByUserId(@Param("userId") UUID userId);

    /**
     * Mark all messages as read for a user in a conversation.
     * Marks messages sent by OTHERS (not the current user) as read.
     *
     * @param matchId Match/conversation ID
     * @param userId User who is reading the messages (marks messages from others as read)
     * @param readAt Timestamp when marked as read
     * @return Number of updated messages
     */
    @Modifying
    @Query("UPDATE Message m SET m.status = 'READ', m.readAt = :readAt " +
           "WHERE m.matchId = :matchId AND m.senderId != :userId " +
           "AND m.status != 'READ' AND m.deletedAt IS NULL")
    int markAllAsRead(
            @Param("matchId") UUID matchId,
            @Param("userId") UUID userId,
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
     * Count total messages in a conversation.
     *
     * @param matchId Match/conversation ID
     * @return Total count of messages
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.matchId = :matchId AND m.deletedAt IS NULL")
    long countByMatchId(@Param("matchId") UUID matchId);

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

    /**
     * Mark all messages from a sender as deleted.
     * Used when a user is deleted.
     *
     * @param senderId Sender user ID
     * @return Number of updated messages
     */
    @Modifying
    @Query("UPDATE Message m SET m.deletedAt = CURRENT_TIMESTAMP WHERE m.senderId = :senderId AND m.deletedAt IS NULL")
    int markMessagesAsDeletedBySenderId(@Param("senderId") UUID senderId);
}
