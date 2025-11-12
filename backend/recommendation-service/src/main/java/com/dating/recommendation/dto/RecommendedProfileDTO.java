package com.dating.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing a recommended user profile with match score.
 * Returned by the recommendation engine.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedProfileDTO {

    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private Integer age;
    private String gender;
    private String bio;
    private List<String> photos;
    private List<String> interests;

    // Location information
    private Double latitude;
    private Double longitude;
    private String city;
    private String country;

    // Matching score and details
    private Double matchScore; // 0.0 to 100.0
    private Double distanceKm;
    private ScoreBreakdown scoreBreakdown;

    /**
     * Breakdown of the match score for transparency
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScoreBreakdown {
        private Double ageCompatibilityScore;
        private Double distanceProximityScore;
        private Double commonInterestsScore;
        private Integer commonInterestCount;
    }
}
