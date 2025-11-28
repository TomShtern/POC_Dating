package com.dating.chat.repository;

import com.dating.chat.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Conversation entity.
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /**
     * Find a conversation by match ID.
     */
    Optional<Conversation> findByMatchId(UUID matchId);

    /**
     * Find all conversations for a user (where they are user1 or user2).
     */
    @Query("SELECT c FROM Conversation c WHERE (c.user1Id = :userId OR c.user2Id = :userId) AND c.archivedAt IS NULL ORDER BY c.lastMessageAt DESC NULLS LAST")
    List<Conversation> findByUserId(@Param("userId") UUID userId);

    /**
     * Check if a user is part of a conversation.
     */
    @Query("SELECT COUNT(c) > 0 FROM Conversation c WHERE c.id = :conversationId AND (c.user1Id = :userId OR c.user2Id = :userId)")
    boolean isUserInConversation(@Param("userId") UUID userId, @Param("conversationId") UUID conversationId);

    /**
     * Check if a user is part of a match (via conversation).
     */
    @Query("SELECT COUNT(c) > 0 FROM Conversation c WHERE c.matchId = :matchId AND (c.user1Id = :userId OR c.user2Id = :userId)")
    boolean isUserInMatch(@Param("userId") UUID userId, @Param("matchId") UUID matchId);

    /**
     * Find conversation between two users (regardless of order).
     */
    @Query("SELECT c FROM Conversation c WHERE (c.user1Id = :userId1 AND c.user2Id = :userId2) OR (c.user1Id = :userId2 AND c.user2Id = :userId1)")
    Optional<Conversation> findByUsers(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);
}
