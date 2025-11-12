package com.dating.chat.repository;

import com.dating.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Conversation entity
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /**
     * Find conversation by match ID
     */
    Optional<Conversation> findByMatchId(UUID matchId);

    /**
     * Find all conversations for a user (where user is participant)
     */
    @Query("SELECT c FROM Conversation c WHERE (c.user1Id = :userId OR c.user2Id = :userId) AND c.archived = false ORDER BY c.lastMessageAt DESC NULLS LAST")
    List<Conversation> findAllByUserId(@Param("userId") UUID userId);

    /**
     * Find conversation between two users
     */
    @Query("SELECT c FROM Conversation c WHERE (c.user1Id = :user1Id AND c.user2Id = :user2Id) OR (c.user1Id = :user2Id AND c.user2Id = :user1Id)")
    Optional<Conversation> findByUserIds(@Param("user1Id") UUID user1Id, @Param("user2Id") UUID user2Id);

    /**
     * Check if conversation exists for a match
     */
    boolean existsByMatchId(UUID matchId);

    /**
     * Count active conversations for a user
     */
    @Query("SELECT COUNT(c) FROM Conversation c WHERE (c.user1Id = :userId OR c.user2Id = :userId) AND c.archived = false")
    long countActiveConversationsByUserId(@Param("userId") UUID userId);
}
