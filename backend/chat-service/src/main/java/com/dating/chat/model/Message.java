package com.dating.chat.model;

import com.dating.chat.dto.websocket.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import com.dating.common.constant.MessageStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Message entity - represents a chat message between matched users.
 * Maps to the 'messages' table in the database.
 */
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_messages_match_id", columnList = "match_id"),
    @Index(name = "idx_messages_sender", columnList = "sender_id"),
    @Index(name = "idx_messages_status", columnList = "status"),
    @Index(name = "idx_messages_created", columnList = "created_at"),
    @Index(name = "idx_messages_match_created", columnList = "match_id, created_at"),
    @Index(name = "idx_messages_match_status", columnList = "match_id, sender_id, status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Match ID (also serves as conversation ID).
     */
    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    /**
     * ID of the user who sent the message.
     */
    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    /**
     * Message content.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Message delivery status (SENT, DELIVERED, READ).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    /**
     * When the message was created/sent.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * When the message was delivered to receiver's device.
     */
    @Column(name = "delivered_at")
    private Instant deliveredAt;

    /**
     * When the message was read by the receiver.
     */
    @Column(name = "read_at")
    private Instant readAt;

    /**
     * Soft delete timestamp.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Message delivery status.
     */
    public enum MessageStatus {
        SENT,
        DELIVERED,
        READ
     * Mark message as delivered.
     */
    public void markAsDelivered() {
        if (this.status == MessageStatus.SENT) {
            this.status = MessageStatus.DELIVERED;
            this.deliveredAt = Instant.now();
        }
    }

    /**
     * Mark message as read.
     */
    public void markAsRead() {
        if (this.status != MessageStatus.READ) {
            this.status = MessageStatus.READ;
            this.readAt = Instant.now();
            if (this.deliveredAt == null) {
                this.deliveredAt = Instant.now();
            }
        }
    }

    /**
     * Check if message is deleted.
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
