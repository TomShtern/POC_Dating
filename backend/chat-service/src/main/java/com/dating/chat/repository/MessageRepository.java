package com.dating.chat.repository;

import com.dating.chat.model.Message;
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
 * Repository for Message entity.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Find messages in a conversation, ordered by creation time descending.
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    Page<Message> findByConversationId(@Param("conversationId") UUID conversationId, Pageable pageable);

    /**
     * Find messages in a conversation by match ID.
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.matchId = :matchId AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    Page<Message> findByMatchId(@Param("matchId") UUID matchId, Pageable pageable);

    /**
     * Find recent messages in a conversation (for caching).
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<Message> findRecentMessages(@Param("conversationId") UUID conversationId, Pageable pageable);

    /**
     * Count unread messages for a user in a conversation.
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId AND m.senderId != :userId AND m.readAt IS NULL AND m.deletedAt IS NULL")
    long countUnreadMessages(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    /**
     * Mark messages as read up to a specific message ID.
     */
    @Modifying
    @Query("UPDATE Message m SET m.readAt = :readAt, m.status = 'READ' WHERE m.conversation.matchId = :matchId AND m.senderId != :userId AND m.readAt IS NULL AND m.createdAt <= (SELECT m2.createdAt FROM Message m2 WHERE m2.id = :lastReadMessageId)")
    int markMessagesAsRead(@Param("matchId") UUID matchId, @Param("userId") UUID userId, @Param("lastReadMessageId") UUID lastReadMessageId, @Param("readAt") Instant readAt);

    /**
     * Find the latest message in a conversation.
     */
    default Message findLatestMessage(UUID conversationId) {
        Page<Message> page = findByConversationId(conversationId, org.springframework.data.domain.PageRequest.of(0, 1));
        return page.hasContent() ? page.getContent().get(0) : null;
    }
}
