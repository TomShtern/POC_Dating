package com.dating.recommendation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * InteractionHistory entity - stores user interaction data for analytics.
 * Maps to the 'interaction_history' table in the database.
 */
@Entity
@Table(name = "interaction_history", indexes = {
    @Index(name = "idx_interaction_history_user_id", columnList = "user_id"),
    @Index(name = "idx_interaction_history_action", columnList = "action"),
    @Index(name = "idx_interaction_history_created_at", columnList = "created_at"),
    @Index(name = "idx_interaction_history_user_action", columnList = "user_id, action, created_at"),
    @Index(name = "idx_interaction_history_user_target", columnList = "user_id, target_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "target_id")
    private UUID targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
