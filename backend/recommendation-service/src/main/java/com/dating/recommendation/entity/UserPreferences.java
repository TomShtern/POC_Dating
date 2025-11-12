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
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a user's matching preferences.
 *
 * This entity stores the criteria that users specify for finding potential matches:
 * - Age range (min and max age)
 * - Gender preference
 * - Maximum distance (in kilometers)
 * - List of interests
 *
 * These preferences are used by the recommendation engine to filter and score
 * potential matches.
 */
@Entity
@Table(name = "user_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The ID of the user who owns these preferences.
     * This should match the user ID from the user-service.
     */
    @Column(nullable = false, unique = true)
    private Long userId;

    /**
     * Minimum age preference (inclusive)
     */
    @Column(nullable = false)
    private Integer minAge;

    /**
     * Maximum age preference (inclusive)
     */
    @Column(nullable = false)
    private Integer maxAge;

    /**
     * Preferred gender(s) to match with
     * Examples: "MALE", "FEMALE", "ANY", "NON_BINARY"
     */
    @Column(nullable = false)
    private String preferredGender;

    /**
     * Maximum distance in kilometers for potential matches
     */
    @Column(nullable = false)
    private Integer maxDistance;

    /**
     * List of interests/hobbies
     * Stored as a comma-separated string or JSON array
     */
    @ElementCollection
    @CollectionTable(name = "user_preference_interests", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "interest")
    @Builder.Default
    private List<String> interests = new ArrayList<>();

    /**
     * Whether the user wants to see users outside their age range
     * if there aren't enough matches
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean flexibleAgeRange = false;

    /**
     * Whether the user wants to see users outside their distance range
     * if there aren't enough matches
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean flexibleDistance = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
