package com.dating.recommendation.service;

import com.dating.recommendation.dto.RecommendedProfileDTO;
import com.dating.recommendation.dto.UserSummaryDTO;
import com.dating.recommendation.entity.UserPreferences;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Recommendation Engine - Core algorithm for generating personalized recommendations.
 *
 * This service implements a simple but extensible recommendation algorithm based on:
 * 1. Age compatibility
 * 2. Distance proximity
 * 3. Common interests
 *
 * The algorithm is designed to be easily replaceable with more sophisticated
 * approaches (e.g., collaborative filtering, machine learning models) in the future.
 */
@Service
@Slf4j
public class RecommendationEngine {

    @Value("${recommendation.algorithm.max-results:50}")
    private int maxResults;

    @Value("${recommendation.algorithm.score-weights.age-compatibility:0.3}")
    private double ageWeight;

    @Value("${recommendation.algorithm.score-weights.distance-proximity:0.4}")
    private double distanceWeight;

    @Value("${recommendation.algorithm.score-weights.common-interests:0.3}")
    private double interestsWeight;

    /**
     * Generate recommendations for a user.
     *
     * @param currentUser the current user's profile
     * @param preferences the user's preferences
     * @param candidates list of candidate users
     * @param excludeUserIds user IDs to exclude (already swiped/matched)
     * @return list of recommended profiles with scores
     */
    public List<RecommendedProfileDTO> generateRecommendations(
            UserSummaryDTO currentUser,
            UserPreferences preferences,
            List<UserSummaryDTO> candidates,
            Set<Long> excludeUserIds
    ) {
        log.debug("Generating recommendations for user {} from {} candidates",
                currentUser.getId(), candidates.size());

        List<RecommendedProfileDTO> recommendations = candidates.stream()
            // Filter out excluded users
            .filter(candidate -> !excludeUserIds.contains(candidate.getId()))
            // Filter out the current user (shouldn't happen, but just in case)
            .filter(candidate -> !candidate.getId().equals(currentUser.getId()))
            // Filter out inactive users
            .filter(UserSummaryDTO::getActive)
            // Apply preference filters
            .filter(candidate -> matchesPreferences(candidate, preferences))
            // Calculate match scores
            .map(candidate -> scoreCandidate(currentUser, candidate, preferences))
            // Sort by score descending
            .sorted(Comparator.comparing(RecommendedProfileDTO::getMatchScore).reversed())
            // Limit results
            .limit(maxResults)
            .collect(Collectors.toList());

        log.info("Generated {} recommendations for user {}", recommendations.size(), currentUser.getId());

        return recommendations;
    }

    /**
     * Check if a candidate matches the user's preferences.
     */
    private boolean matchesPreferences(UserSummaryDTO candidate, UserPreferences preferences) {
        // Gender filter
        if (!preferences.getPreferredGender().equalsIgnoreCase("ANY") &&
            !preferences.getPreferredGender().equalsIgnoreCase(candidate.getGender())) {
            return false;
        }

        // Age filter
        Integer candidateAge = candidate.getAge();
        if (candidateAge != null) {
            if (!preferences.getFlexibleAgeRange()) {
                if (candidateAge < preferences.getMinAge() || candidateAge > preferences.getMaxAge()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Calculate match score for a candidate.
     */
    private RecommendedProfileDTO scoreCandidate(
            UserSummaryDTO currentUser,
            UserSummaryDTO candidate,
            UserPreferences preferences
    ) {
        // Calculate individual score components
        double ageScore = calculateAgeCompatibilityScore(currentUser, candidate, preferences);
        double distanceScore = calculateDistanceProximityScore(currentUser, candidate, preferences);
        double interestsScore = calculateCommonInterestsScore(currentUser, candidate);

        // Calculate weighted total score
        double totalScore = (ageScore * ageWeight) +
                           (distanceScore * distanceWeight) +
                           (interestsScore * interestsWeight);

        // Calculate actual distance
        double distanceKm = calculateDistance(
                currentUser.getLatitude(), currentUser.getLongitude(),
                candidate.getLatitude(), candidate.getLongitude()
        );

        // Count common interests
        int commonInterestCount = countCommonInterests(
                currentUser.getInterests(),
                candidate.getInterests()
        );

        // Build score breakdown
        RecommendedProfileDTO.ScoreBreakdown breakdown = RecommendedProfileDTO.ScoreBreakdown.builder()
            .ageCompatibilityScore(ageScore)
            .distanceProximityScore(distanceScore)
            .commonInterestsScore(interestsScore)
            .commonInterestCount(commonInterestCount)
            .build();

        // Build recommended profile DTO
        return RecommendedProfileDTO.builder()
            .userId(candidate.getId())
            .username(candidate.getUsername())
            .firstName(candidate.getFirstName())
            .lastName(candidate.getLastName())
            .age(candidate.getAge())
            .gender(candidate.getGender())
            .bio(candidate.getBio())
            .photos(candidate.getPhotos())
            .interests(candidate.getInterests())
            .latitude(candidate.getLatitude())
            .longitude(candidate.getLongitude())
            .city(candidate.getCity())
            .country(candidate.getCountry())
            .matchScore(Math.round(totalScore * 100.0) / 100.0) // Round to 2 decimal places
            .distanceKm(Math.round(distanceKm * 100.0) / 100.0)
            .scoreBreakdown(breakdown)
            .build();
    }

    /**
     * Calculate age compatibility score (0-100).
     * Closer ages get higher scores.
     */
    private double calculateAgeCompatibilityScore(
            UserSummaryDTO currentUser,
            UserSummaryDTO candidate,
            UserPreferences preferences
    ) {
        if (currentUser.getAge() == null || candidate.getAge() == null) {
            return 50.0; // Neutral score if age is missing
        }

        int ageDifference = Math.abs(currentUser.getAge() - candidate.getAge());

        // Perfect match (same age or within 2 years)
        if (ageDifference <= 2) {
            return 100.0;
        }

        // Calculate score based on age difference
        // Score decreases as age difference increases
        int maxAgeDifference = Math.max(
                Math.abs(preferences.getMinAge() - currentUser.getAge()),
                Math.abs(preferences.getMaxAge() - currentUser.getAge())
        );

        if (maxAgeDifference == 0) {
            return 100.0;
        }

        double score = 100.0 - (ageDifference * 100.0 / maxAgeDifference);
        return Math.max(0, Math.min(100, score));
    }

    /**
     * Calculate distance proximity score (0-100).
     * Closer distances get higher scores.
     */
    private double calculateDistanceProximityScore(
            UserSummaryDTO currentUser,
            UserSummaryDTO candidate,
            UserPreferences preferences
    ) {
        if (currentUser.getLatitude() == null || currentUser.getLongitude() == null ||
            candidate.getLatitude() == null || candidate.getLongitude() == null) {
            return 50.0; // Neutral score if location is missing
        }

        double distanceKm = calculateDistance(
                currentUser.getLatitude(), currentUser.getLongitude(),
                candidate.getLatitude(), candidate.getLongitude()
        );

        // Filter by max distance
        if (!preferences.getFlexibleDistance() && distanceKm > preferences.getMaxDistance()) {
            return 0.0;
        }

        // Perfect match (within 5 km)
        if (distanceKm <= 5) {
            return 100.0;
        }

        // Calculate score based on distance
        int maxDistance = preferences.getMaxDistance();
        double score = 100.0 - (distanceKm * 100.0 / maxDistance);
        return Math.max(0, Math.min(100, score));
    }

    /**
     * Calculate common interests score (0-100).
     * More common interests = higher score.
     */
    private double calculateCommonInterestsScore(UserSummaryDTO currentUser, UserSummaryDTO candidate) {
        List<String> userInterests = currentUser.getInterests();
        List<String> candidateInterests = candidate.getInterests();

        if (userInterests == null || userInterests.isEmpty() ||
            candidateInterests == null || candidateInterests.isEmpty()) {
            return 50.0; // Neutral score if no interests
        }

        int commonCount = countCommonInterests(userInterests, candidateInterests);

        if (commonCount == 0) {
            return 0.0;
        }

        // Calculate score based on percentage of common interests
        int totalUniqueInterests = (int) userInterests.stream()
                .distinct()
                .count();

        double score = (commonCount * 100.0) / totalUniqueInterests;
        return Math.min(100, score);
    }

    /**
     * Count common interests between two lists.
     */
    private int countCommonInterests(List<String> interests1, List<String> interests2) {
        if (interests1 == null || interests2 == null) {
            return 0;
        }

        Set<String> set1 = interests1.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        Set<String> set2 = interests2.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        set1.retainAll(set2);
        return set1.size();
    }

    /**
     * Calculate distance between two points using Haversine formula.
     * Returns distance in kilometers.
     */
    private double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.MAX_VALUE;
        }

        final int EARTH_RADIUS_KM = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
