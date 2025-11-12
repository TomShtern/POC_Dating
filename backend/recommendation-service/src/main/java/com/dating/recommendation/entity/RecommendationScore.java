package com.dating.recommendation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing a cached recommendation score between two users.
 *
 * This is an optional optimization to cache computed recommendation scores
 * to avoid recalculating them on every request. Scores can be invalidated
 * when user preferences or profiles change.
 *
 * The score is computed based on:
 * - Age compatibility
 * - Distance proximity
 * - Common interests
 * - Other algorithmic factors
 */
@Entity
@Table(name = "recommendation_scores",
    indexes = {
        @Index(name = "idx_user_candidate", columnList = "userId,candidateUserId"),
        @Index(name = "idx_user_score", columnList = "userId,score")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class RecommendationScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user for whom this recommendation is generated
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * The candidate user being recommended
     */
    @Column(nullable = false)
    private Long candidateUserId;

    /**
     * The computed score (0.0 to 100.0)
     * Higher scores indicate better matches
     */
    @Column(nullable = false)
    private Double score;

    /**
     * Breakdown of the score components for transparency
     */
    @Column(columnDefinition = "TEXT")
    private String scoreBreakdown;

    /**
     * Whether this recommendation has been shown to the user
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean shown = false;

    /**
     * Timestamp when this recommendation was shown
     */
    private LocalDateTime shownAt;

    /**
     * Whether the score is still valid (can be invalidated on profile changes)
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean valid = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Expiration time for this cached score
     * After this time, the score should be recalculated
     */
    private LocalDateTime expiresAt;
}
