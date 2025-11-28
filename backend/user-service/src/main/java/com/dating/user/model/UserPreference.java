package com.dating.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * UserPreference entity - stores user matching preferences.
 * Maps to the 'user_preferences' table in the database.
 */
@Entity
@Table(name = "user_preferences", indexes = {
    @Index(name = "idx_user_preferences_user_id", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "min_age")
    @Builder.Default
    private Integer minAge = 18;

    @Column(name = "max_age")
    @Builder.Default
    private Integer maxAge = 99;

    @Column(name = "max_distance_km")
    @Builder.Default
    private Integer maxDistanceKm = 50;

    @Column(name = "interested_in", length = 20)
    @Builder.Default
    private String interestedIn = "BOTH";

    @Column(name = "interests", columnDefinition = "TEXT[]")
    private String[] interests;

    @Column(name = "notification_enabled")
    @Builder.Default
    private Boolean notificationEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
