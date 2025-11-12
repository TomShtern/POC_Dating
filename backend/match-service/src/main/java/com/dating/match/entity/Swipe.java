package com.dating.match.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing a swipe action performed by a user
 *
 * PURPOSE: Record all swipe actions for match detection and analytics
 *
 * FIELDS:
 * - id: Primary key (auto-generated)
 * - userId: User who performed the swipe
 * - targetUserId: User who was swiped on
 * - swipeType: Type of swipe (LIKE, PASS, SUPER_LIKE)
 * - timestamp: When the swipe occurred (auto-populated)
 *
 * WHY LOMBOK:
 * - @Data generates getters, setters, toString, equals, hashCode
 * - @Builder enables fluent object creation
 * - @NoArgsConstructor/@AllArgsConstructor for JPA and builders
 * - Reduces boilerplate code significantly
 *
 * DATABASE DESIGN:
 * - Composite unique index on (userId, targetUserId) prevents duplicate swipes
 * - Index on userId for fast lookup of user's swipe history
 * - Index on targetUserId for reverse lookups (who swiped on me)
 * - CreatedDate audit field for analytics
 *
 * ASSUMPTIONS:
 * - Users cannot change their swipe decision (no updates)
 * - Users cannot swipe on the same person twice
 * - Swipes are permanent records for analytics
 *
 * ALTERNATIVES:
 * - Store only LIKE swipes: Loses analytics data
 * - Soft delete for PASS: More complex, same result
 * - Separate tables per swipe type: Overkill, harder to query
 */
@Entity
@Table(
    name = "swipes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "target_user_id"}),
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_target_user_id", columnList = "target_user_id"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Swipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "swipe_type", nullable = false, length = 20)
    private SwipeType swipeType;

    @CreatedDate
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
