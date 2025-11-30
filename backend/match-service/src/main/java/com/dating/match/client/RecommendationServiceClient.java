package com.dating.match.client;

import com.dating.match.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Feign client for Recommendation Service communication.
 */
@FeignClient(name = "recommendation-service", url = "${services.recommendation-service.url}", configuration = FeignClientConfig.class)
public interface RecommendationServiceClient {

    /**
     * Get recommendations for a user.
     *
     * @param userId User ID
     * @param limit  Number of recommendations to fetch
     * @return Recommendations response
     */
    @GetMapping("/api/recommendations/{userId}")
    RecommendationsResponse getRecommendations(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "50") int limit);

    /**
     * Get compatibility score between two users.
     *
     * @param userId       First user ID
     * @param targetUserId Target user ID
     * @return Compatibility score response
     */
    @GetMapping("/api/recommendations/{userId}/{targetUserId}/score")
    CompatibilityScoreResponse getCompatibilityScore(
            @PathVariable UUID userId,
            @PathVariable UUID targetUserId);

    /**
     * Recommendations response from recommendation-service.
     */
    record RecommendationsResponse(
            List<RecommendedUser> recommendations,
            long total,
            boolean hasMore) {
    }

    /**
     * Recommended user with score.
     */
    record RecommendedUser(
            UUID id,
            int score,
            Map<String, Object> scoreFactors,
            String reason) {
    }

    /**
     * Compatibility score response.
     */
    record CompatibilityScoreResponse(
            int score,
            Map<String, Object> factors) {
    }
}
