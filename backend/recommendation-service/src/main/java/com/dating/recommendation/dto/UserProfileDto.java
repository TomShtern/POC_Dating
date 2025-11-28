package com.dating.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing user profile data for scoring calculations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto implements Serializable {

    private static final long serialVersionUID = 1L;

    // Basic info
    private UUID id;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String bio;
    private String profilePictureUrl;
    private Instant lastLogin;
    private boolean verified;

    // Location
    private String city;
    private String country;
    private Double latitude;
    private Double longitude;

    // Preferences
    private int minAge;
    private int maxAge;
    private int maxDistanceKm;
    private String interestedIn;
    private List<String> interests;

    // Statistics
    private int photoCount;
    private long swipeCount;
    private long messagesSent;
    private long messagesReceived;

    /**
     * Calculate user's age from date of birth.
     */
    public int getAge() {
        if (dateOfBirth == null) {
            return 0;
        }
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }

    /**
     * Get display name.
     */
    public String getDisplayName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        }
        return "User";
    }
}
