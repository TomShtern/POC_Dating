package com.dating.recommendation.service;

import com.dating.recommendation.client.MatchServiceClient;
import com.dating.recommendation.client.UserServiceClient;
import com.dating.recommendation.dto.RecommendedProfileDTO;
import com.dating.recommendation.dto.UserSummaryDTO;
import com.dating.recommendation.entity.UserPreferences;
import com.dating.recommendation.repository.UserPreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for generating recommendations.
 *
 * This service orchestrates the recommendation process:
 * 1. Fetch user profile and preferences
 * 2. Fetch candidate users from user-service
 * 3. Fetch swipe history from match-service to exclude already swiped users
 * 4. Use RecommendationEngine to calculate scores and filter candidates
 * 5. Return sorted recommendations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final UserServiceClient userServiceClient;
    private final MatchServiceClient matchServiceClient;
    private final UserPreferencesRepository preferencesRepository;
    private final RecommendationEngine recommendationEngine;
    private final PreferencesService preferencesService;

    /**
     * Get recommendations for a user.
     *
     * @param userId the user ID
     * @return list of recommended profiles
     */
    public List<RecommendedProfileDTO> getRecommendations(Long userId) {
        log.info("Generating recommendations for user: {}", userId);

        try {
            // 1. Get current user's profile
            UserSummaryDTO currentUser = userServiceClient.getUserById(userId);
            log.debug("Fetched current user profile: {}", currentUser.getUsername());

            // 2. Get user's preferences
            UserPreferences preferences = preferencesRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("No preferences found for user {}, creating defaults", userId);
                    preferencesService.createDefaultPreferences(userId);
                    return preferencesRepository.findByUserId(userId)
                        .orElseThrow(() -> new IllegalStateException("Failed to create default preferences"));
                });

            // 3. Get candidate users from user-service
            List<UserSummaryDTO> candidates = userServiceClient.getActiveUsers(
                userId,
                preferences.getPreferredGender().equals("ANY") ? null : preferences.getPreferredGender(),
                preferences.getMinAge(),
                preferences.getMaxAge()
            );
            log.debug("Fetched {} candidate users", candidates.size());

            // 4. Get already swiped/matched user IDs to exclude
            Set<Long> excludeUserIds = getExcludedUserIds(userId);
            log.debug("Excluding {} users (already swiped/matched)", excludeUserIds.size());

            // 5. Generate recommendations using the engine
            List<RecommendedProfileDTO> recommendations = recommendationEngine.generateRecommendations(
                currentUser,
                preferences,
                candidates,
                excludeUserIds
            );

            log.info("Generated {} recommendations for user {}", recommendations.size(), userId);
            return recommendations;

        } catch (Exception e) {
            log.error("Error generating recommendations for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate recommendations", e);
        }
    }

    /**
     * Get a single recommendation (next profile to show).
     *
     * @param userId the user ID
     * @return the next recommended profile or null if none available
     */
    public RecommendedProfileDTO getNextRecommendation(Long userId) {
        List<RecommendedProfileDTO> recommendations = getRecommendations(userId);

        if (recommendations.isEmpty()) {
            log.info("No recommendations available for user {}", userId);
            return null;
        }

        return recommendations.get(0);
    }

    /**
     * Refresh recommendations for a user.
     * This is useful after preferences change or after a period of time.
     *
     * @param userId the user ID
     * @return list of fresh recommendations
     */
    public List<RecommendedProfileDTO> refreshRecommendations(Long userId) {
        log.info("Refreshing recommendations for user: {}", userId);
        // The getRecommendations method always generates fresh recommendations,
        // so we can just call it directly
        return getRecommendations(userId);
    }

    /**
     * Get excluded user IDs (already swiped or matched).
     */
    private Set<Long> getExcludedUserIds(Long userId) {
        Set<Long> excludedIds = new HashSet<>();

        try {
            // Get swiped user IDs
            List<Long> swipedIds = matchServiceClient.getSwipedUserIds(userId);
            if (swipedIds != null) {
                excludedIds.addAll(swipedIds);
            }

            // Get matched user IDs
            List<Long> matchedIds = matchServiceClient.getMatchedUserIds(userId);
            if (matchedIds != null) {
                excludedIds.addAll(matchedIds);
            }
        } catch (Exception e) {
            log.warn("Error fetching excluded user IDs for user {}: {}", userId, e.getMessage());
            // Continue with empty exclusion list rather than failing
        }

        return excludedIds;
    }

    /**
     * Get recommendation statistics for a user.
     *
     * @param userId the user ID
     * @return map of statistics
     */
    public Map<String, Object> getRecommendationStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            List<RecommendedProfileDTO> recommendations = getRecommendations(userId);

            stats.put("totalRecommendations", recommendations.size());
            stats.put("averageScore", recommendations.stream()
                .mapToDouble(RecommendedProfileDTO::getMatchScore)
                .average()
                .orElse(0.0));
            stats.put("topScore", recommendations.stream()
                .mapToDouble(RecommendedProfileDTO::getMatchScore)
                .max()
                .orElse(0.0));
            stats.put("hasPreferences", preferencesService.hasPreferences(userId));

        } catch (Exception e) {
            log.error("Error calculating stats for user {}: {}", userId, e.getMessage());
        }

        return stats;
    }
}
