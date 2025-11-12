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
 * Entity representing a mutual match between two users
 *
 * PURPOSE: Record mutual matches for chat and dating features
 *
 * FIELDS:
 * - id: Primary key (auto-generated)
 * - user1Id: First user in the match (lower ID)
 * - user2Id: Second user in the match (higher ID)
 * - matchedAt: When the match was created (auto-populated)
 * - isActive: Whether the match is still active (false if unmatched)
 *
 * WHY ORDERED IDs (user1Id < user2Id):
 * - Ensures uniqueness regardless of who swiped first
 * - Prevents duplicate match records (A->B vs B->A)
 * - Simplifies queries (don't need to check both directions)
 * - Standard pattern in bidirectional relationships
 *
 * DATABASE DESIGN:
 * - Composite unique index on (user1Id, user2Id) prevents duplicates
 * - Index on user1Id for fast lookup of user's matches
 * - Index on user2Id for reverse lookups
 * - isActive allows soft delete (unmatch) instead of hard delete
 *
 * MATCH CREATION LOGIC:
 * 1. User A likes User B
 * 2. Check if User B previously liked User A
 * 3. If yes, create Match record with min(A,B), max(A,B)
 * 4. Publish match:created event
 * 5. Enable chat between users
 *
 * ASSUMPTIONS:
 * - Match is bidirectional (both users can message)
 * - Match can be deactivated (unmatch) by either user
 * - Deactivated matches remain in DB for analytics
 * - Users can re-match if both swipe again (new Match record)
 *
 * ALTERNATIVES:
 * - One row per user: Duplicates data, harder to maintain
 * - Delete on unmatch: Loses analytics data
 * - Separate MatchHistory table: More complex, same result
 */
@Entity
@Table(
    name = "matches",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user1_id", "user2_id"}),
    indexes = {
        @Index(name = "idx_user1_id", columnList = "user1_id"),
        @Index(name = "idx_user2_id", columnList = "user2_id"),
        @Index(name = "idx_matched_at", columnList = "matched_at"),
        @Index(name = "idx_is_active", columnList = "is_active")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user1_id", nullable = false)
    private Long user1Id;

    @Column(name = "user2_id", nullable = false)
    private Long user2Id;

    @CreatedDate
    @Column(name = "matched_at", nullable = false, updatable = false)
    private LocalDateTime matchedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Helper method to create a Match with ordered user IDs
     * Ensures user1Id < user2Id
     */
    public static Match createMatch(Long userIdA, Long userIdB) {
        Long user1 = Math.min(userIdA, userIdB);
        Long user2 = Math.max(userIdA, userIdB);

        return Match.builder()
                .user1Id(user1)
                .user2Id(user2)
                .isActive(true)
                .build();
    }

    /**
     * Check if a user is part of this match
     */
    public boolean includesUser(Long userId) {
        return user1Id.equals(userId) || user2Id.equals(userId);
    }

    /**
     * Get the other user in the match
     */
    public Long getOtherUserId(Long userId) {
        if (user1Id.equals(userId)) {
            return user2Id;
        } else if (user2Id.equals(userId)) {
            return user1Id;
        }
        throw new IllegalArgumentException("User " + userId + " is not part of this match");
    }
}
