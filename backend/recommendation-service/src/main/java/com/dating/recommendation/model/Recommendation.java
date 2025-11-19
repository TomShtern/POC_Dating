package com.dating.recommendation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Recommendation entity - stores ML/algorithm-based recommendations.
 * Maps to the 'recommendations' table in the database.
 */
@Entity
@Table(name = "recommendations", indexes = {
    @Index(name = "idx_recommendations_user_id", columnList = "user_id"),
    @Index(name = "idx_recommendations_created_at", columnList = "created_at"),
    @Index(name = "idx_recommendations_expires_at", columnList = "expires_at"),
    @Index(name = "idx_recommendations_score", columnList = "score"),
    @Index(name = "idx_recommendations_user_score", columnList = "user_id, score"),
    @Index(name = "idx_recommendations_user_expires", columnList = "user_id, expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "recommended_user_id", nullable = false)
    private UUID recommendedUserId;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "algorithm_version", length = 20)
    private String algorithmVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "factors", columnDefinition = "jsonb")
    private Map<String, Object> factors;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
