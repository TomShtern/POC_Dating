package com.dating.chat.repository;

import com.dating.chat.entity.Message;
import com.dating.chat.entity.enums.MessageStatus;
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
 * Repository for Message entity
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Find all messages in a conversation (paginated, ordered by timestamp DESC)
     */
    Page<Message> findByConversationIdAndDeletedFalseOrderByTimestampDesc(UUID conversationId, Pageable pageable);

    /**
     * Find all messages in a conversation (list, ordered by timestamp ASC for chat display)
     */
    List<Message> findByConversationIdAndDeletedFalseOrderByTimestampAsc(UUID conversationId);

    /**
     * Find the last message in a conversation
     */
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId AND m.deleted = false ORDER BY m.timestamp DESC LIMIT 1")
    Optional<Message> findLastMessageByConversationId(@Param("conversationId") UUID conversationId);

    /**
     * Count unread messages for a user in a conversation
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId AND m.recipientId = :userId AND m.status != 'READ' AND m.deleted = false")
    long countUnreadMessages(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    /**
     * Find unread messages for a user in a conversation
     */
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId AND m.recipientId = :userId AND m.status != 'READ' AND m.deleted = false")
    List<Message> findUnreadMessages(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    /**
     * Find messages by IDs
     */
    List<Message> findByIdIn(List<UUID> ids);

    /**
     * Count total messages in a conversation
     */
    long countByConversationIdAndDeletedFalse(UUID conversationId);

    /**
     * Update message status for multiple messages
     */
    @Query("UPDATE Message m SET m.status = :status WHERE m.id IN :messageIds")
    void updateMessageStatus(@Param("messageIds") List<UUID> messageIds, @Param("status") MessageStatus status);
}
