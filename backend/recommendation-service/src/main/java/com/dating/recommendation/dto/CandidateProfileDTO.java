package com.dating.recommendation.dto;

import com.dating.recommendation.model.User;

import java.util.Set;
import java.util.UUID;

/**
 * ============================================================================
 * CANDIDATE PROFILE DTO - SAFE USER REPRESENTATION
 * ============================================================================
 *
 * PURPOSE:
 * Provides a safe, client-facing view of a candidate user.
 * Excludes sensitive internal fields that should not be exposed via API.
 *
 * WHY THIS EXISTS:
 * The User entity contains internal fields like:
 * - verified, active (internal status)
 * - createdAt, updatedAt (database timestamps)
 * - genderPreferences (privacy concern)
 * - minAgePreference, maxAgePreference (privacy concern)
 *
 * This DTO only exposes what the client needs to display a profile card.
 *
 * FIELDS INCLUDED:
 * - id: For API calls (like, swipe, etc.)
 * - username: Display name
 * - age: Calculated from dateOfBirth
 * - photoUrl: Profile picture
 * - bio: User's description
 * - interests: Shared interests display
 * - city, country: General location (not exact coordinates)
 * - gender: For display if needed
 *
 * FIELDS EXCLUDED (Security/Privacy):
 * - email, password (authentication)
 * - latitude, longitude (exact location)
 * - genderPreferences (private preference)
 * - min/maxAgePreference (private preference)
 * - verified, active (internal status)
 * - createdAt, updatedAt (internal timestamps)
 * - lastActiveAt (could be used for stalking)
 *
 * HOW TO MODIFY:
 * - Add fields: Update record and fromUser() factory method
 * - Remove fields: Same process
 * - Add computed fields: Add method to record
 *
 * ============================================================================
 */
public record CandidateProfileDTO(
        /**
         * Unique identifier for API calls.
         */
        UUID id,

        /**
         * Display name shown on profile card.
         */
        String username,

        /**
         * Age calculated from date of birth.
         * 0 if date of birth is not set.
         */
        int age,

        /**
         * URL to profile picture.
         * May be null if user hasn't uploaded a photo.
         */
        String photoUrl,

        /**
         * User's bio/description.
         * May be null or empty.
         */
        String bio,

        /**
         * User's interests for "shared interests" display.
         * May be empty but never null.
         */
        Set<String> interests,

        /**
         * City for general location display.
         * May be null if not set.
         */
        String city,

        /**
         * Country for general location display.
         * May be null if not set.
         */
        String country,

        /**
         * Gender for display purposes.
         * Values: MALE, FEMALE, OTHER
         */
        String gender
) {

    /**
     * Factory method to create CandidateProfileDTO from User entity.
     *
     * This is the ONLY way to create a CandidateProfileDTO from a User.
     * Ensures consistent mapping and excludes sensitive fields.
     *
     * @param user The User entity to convert
     * @return Safe CandidateProfileDTO for client exposure
     */
    public static CandidateProfileDTO fromUser(User user) {
        return new CandidateProfileDTO(
                user.getId(),
                user.getUsername(),
                user.getAge(),
                user.getPhotoUrl(),
                user.getBio(),
                user.getInterests() != null ? Set.copyOf(user.getInterests()) : Set.of(),
                user.getCity(),
                user.getCountry(),
                user.getGender()
        );
    }
}
