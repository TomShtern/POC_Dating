package com.dating.match.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Match entity - represents a mutual match between two users.
 * Maps to the 'matches' table in the database.
 */
@Entity
@Table(name = "matches", indexes = {
    @Index(name = "idx_matches_user1_id", columnList = "user1_id"),
    @Index(name = "idx_matches_user2_id", columnList = "user2_id"),
    @Index(name = "idx_matches_matched_at", columnList = "matched_at"),
    @Index(name = "idx_matches_user1_matched", columnList = "user1_id, matched_at"),
    @Index(name = "idx_matches_user2_matched", columnList = "user2_id, matched_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_matches_user_pair", columnNames = {"user1_id", "user2_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user1_id", nullable = false)
    private UUID user1Id;

    @Column(name = "user2_id", nullable = false)
    private UUID user2Id;

    @CreationTimestamp
    @Column(name = "matched_at", nullable = false, updatable = false)
    private Instant matchedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @OneToOne(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private MatchScore matchScore;

    /**
     * Check if this match is active (not ended).
     *
     * @return true if active
     */
    public boolean isActive() {
        return endedAt == null;
    }

    /**
     * Check if a user is part of this match.
     *
     * @param userId User ID to check
     * @return true if user is part of this match
     */
    public boolean involvesUser(UUID userId) {
        return user1Id.equals(userId) || user2Id.equals(userId);
    }

    /**
     * Get the other user in this match.
     *
     * @param userId Current user's ID
     * @return Other user's ID
     */
    public UUID getOtherUserId(UUID userId) {
        return user1Id.equals(userId) ? user2Id : user1Id;
    }
}
