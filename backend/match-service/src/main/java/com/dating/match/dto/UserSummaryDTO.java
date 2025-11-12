package com.dating.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for user profile summary (from user-service)
 *
 * PURPOSE: Represent essential user profile data for matches and recommendations
 *
 * FIELDS:
 * - userId: Unique user identifier
 * - name: User's display name
 * - age: User's age
 * - bio: Short bio/description
 * - photoUrls: List of profile photo URLs
 * - interests: User's interests/hobbies
 * - location: City or area
 * - distance: Distance from current user (in km)
 *
 * WHY THIS DTO:
 * - Matches data returned by user-service
 * - Contains only public profile info (no sensitive data)
 * - Optimized for display in match cards
 * - Reduced payload size (no full user entity)
 *
 * DATA SOURCE:
 * - Fetched from user-service via Feign client
 * - Cached in Redis for performance
 * - Updated when user-service publishes user:profile-updated events
 *
 * DESIGN DECISIONS:
 * - All fields nullable for graceful degradation
 * - photoUrls is a list (first = primary photo)
 * - distance calculated by user-service based on geo-location
 * - No email/phone (privacy)
 *
 * CLIENT USAGE:
 * - Display user card in swipe feed
 * - Show match profile in match list
 * - Render chat header with name/photo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {

    private Long userId;
    private String name;
    private Integer age;
    private String bio;
    private List<String> photoUrls;
    private List<String> interests;
    private String location;
    private Double distance;
}
