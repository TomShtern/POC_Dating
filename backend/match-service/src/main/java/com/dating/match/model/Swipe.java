package com.dating.match.model;

import com.dating.common.constant.SwipeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Swipe entity - represents a user's swipe action on another user.
 * Maps to the 'swipes' table in the database.
 */
@Entity
@Table(name = "swipes", indexes = {
    @Index(name = "idx_swipes_user_id", columnList = "user_id"),
    @Index(name = "idx_swipes_target_user_id", columnList = "target_user_id"),
    @Index(name = "idx_swipes_user_created", columnList = "user_id, created_at"),
    @Index(name = "idx_swipes_created_at", columnList = "created_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_swipes_user_target", columnNames = {"user_id", "target_user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Swipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "target_user_id", nullable = false)
    private UUID targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private SwipeType action;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
