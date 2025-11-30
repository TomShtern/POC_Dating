package com.dating.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for user profile data.
 * Includes basic profile information and location/preference data for matching.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private UUID id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Integer age;
    private String gender;
    private String bio;
    private String profilePictureUrl;
    private String status;

    // Location information (nullable for users who haven't set location)
    private String city;
    private String country;
    private Double latitude;
    private Double longitude;

    // Preferences (from UserPreference)
    private Integer minAge;
    private Integer maxAge;
    private Integer maxDistanceKm;
    private String interestedIn;
    private List<String> interests;

    // Metadata
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLogin;
}
