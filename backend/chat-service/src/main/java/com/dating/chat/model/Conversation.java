package com.dating.chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Conversation entity representing a chat between two matched users.
 *
 * Note: User IDs are stored in sorted order (user1Id < user2Id)
 * to ensure uniqueness and consistent lookups.
 */
@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conversation_match", columnList = "match_id"),
        @Index(name = "idx_conversation_user1", columnList = "user1_id"),
        @Index(name = "idx_conversation_user2", columnList = "user2_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "match_id", nullable = false, unique = true)
    private UUID matchId;

    @Column(name = "user1_id", nullable = false)
    private UUID user1Id;

    @Column(name = "user2_id", nullable = false)
    private UUID user2Id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    /**
     * Check if a user is part of this conversation.
     */
    public boolean hasUser(UUID userId) {
        return user1Id.equals(userId) || user2Id.equals(userId);
    }

    /**
     * Get the other user's ID in the conversation.
     */
    public UUID getOtherUserId(UUID currentUserId) {
        if (user1Id.equals(currentUserId)) {
            return user2Id;
        } else if (user2Id.equals(currentUserId)) {
            return user1Id;
        }
        throw new IllegalArgumentException("User " + currentUserId + " is not part of this conversation");
    }

    /**
     * Check if the conversation is active (not archived).
     */
    public boolean isActive() {
        return archivedAt == null;
    }
}
