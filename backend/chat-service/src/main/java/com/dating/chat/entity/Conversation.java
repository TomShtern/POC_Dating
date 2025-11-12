package com.dating.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Conversation Entity
 *
 * Represents a chat conversation between two matched users
 */
@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_user1_id", columnList = "user1_id"),
        @Index(name = "idx_user2_id", columnList = "user2_id"),
        @Index(name = "idx_match_id", columnList = "match_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user1_id", nullable = false)
    private UUID user1Id;

    @Column(name = "user2_id", nullable = false)
    private UUID user2Id;

    @Column(name = "match_id", nullable = false, unique = true)
    private UUID matchId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "archived", nullable = false)
    @Builder.Default
    private Boolean archived = false;

    /**
     * Check if a user is participant of this conversation
     */
    public boolean isParticipant(UUID userId) {
        return user1Id.equals(userId) || user2Id.equals(userId);
    }

    /**
     * Get the other participant in the conversation
     */
    public UUID getOtherParticipant(UUID userId) {
        if (user1Id.equals(userId)) {
            return user2Id;
        } else if (user2Id.equals(userId)) {
            return user1Id;
        }
        throw new IllegalArgumentException("User " + userId + " is not a participant of this conversation");
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
