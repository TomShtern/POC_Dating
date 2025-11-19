package com.dating.recommendation.controller;

import com.dating.recommendation.dto.ScoredCandidate;
import com.dating.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ============================================================================
 * RECOMMENDATION CONTROLLER
 * ============================================================================
 *
 * PURPOSE:
 * REST API endpoints for the Recommendation Service.
 * Exposes recommendation generation and algorithm info.
 *
 * ENDPOINTS:
 * GET  /api/recommendations/users/{userId}              - Get recommendations
 * GET  /api/recommendations/users/{userId}/{targetId}/score - Get compatibility
 * POST /api/recommendations/users/{userId}/refresh      - Refresh recommendations
 * GET  /api/recommendations/algorithm/info              - Get algorithm config
 *
 * AUTHENTICATION:
 * All endpoints require valid JWT token.
 * API Gateway validates token and adds X-User-Id header.
 *
 * ============================================================================
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * Get recommendations for a user.
     *
     * ENDPOINT: GET /api/recommendations/users/{userId}
     *
     * RETURNS:
     * List of scored candidates with:
     * - User profile
     * - Compatibility score (0.0 to 1.0)
     * - Score breakdown by factor
     *
     * CACHING:
     * Results are cached for 24 hours.
     * Use POST /refresh to force regeneration.
     *
     * @param userId User ID to get recommendations for
     * @return List of ScoredCandidate
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ScoredCandidate>> getRecommendations(
            @PathVariable UUID userId) {

        log.info("GET /api/recommendations/users/{} - Getting recommendations", userId);

        List<ScoredCandidate> recommendations = recommendationService.getRecommendations(userId);

        log.info("Returning {} recommendations for user {}", recommendations.size(), userId);

        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get compatibility score between two users.
     *
     * ENDPOINT: GET /api/recommendations/users/{userId}/{targetId}/score
     *
     * PURPOSE:
     * - Display "87% match" in UI
     * - Debug scoring algorithm
     * - Analytics
     *
     * @param userId First user ID
     * @param targetId Second user ID
     * @return ScoredCandidate with breakdown
     */
    @GetMapping("/users/{userId}/{targetId}/score")
    public ResponseEntity<ScoredCandidate> getCompatibilityScore(
            @PathVariable UUID userId,
            @PathVariable UUID targetId) {

        log.info("GET /api/recommendations/users/{}/{}/score - Getting compatibility",
                userId, targetId);

        ScoredCandidate score = recommendationService.getCompatibilityScore(userId, targetId);

        return ResponseEntity.ok(score);
    }

    /**
     * Force refresh recommendations for a user.
     *
     * ENDPOINT: POST /api/recommendations/users/{userId}/refresh
     *
     * PURPOSE:
     * Invalidate cache and regenerate recommendations.
     * Use after user updates preferences.
     *
     * RETURNS:
     * 202 Accepted with new recommendations.
     * (202 indicates the request was accepted and processed)
     *
     * @param userId User ID to refresh
     * @return New recommendations with 202 status
     */
    @PostMapping("/users/{userId}/refresh")
    public ResponseEntity<List<ScoredCandidate>> refreshRecommendations(
            @PathVariable UUID userId) {

        log.info("POST /api/recommendations/users/{}/refresh - Refreshing", userId);

        // Invalidate cache
        recommendationService.invalidateRecommendations(userId);

        // Regenerate (will be cached)
        List<ScoredCandidate> recommendations = recommendationService.getRecommendations(userId);

        log.info("Refreshed {} recommendations for user {}", recommendations.size(), userId);

        // Return 202 Accepted to indicate request was processed
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(recommendations);
    }

    /**
     * Get algorithm configuration info.
     *
     * ENDPOINT: GET /api/recommendations/algorithm/info
     *
     * PURPOSE:
     * - Admin dashboard
     * - Debugging
     * - Transparency
     *
     * RETURNS:
     * {
     *   "batchSize": 20,
     *   "minimumScore": 0.3,
     *   "scorers": {
     *     "age-compatibility": 0.2,
     *     "location-distance": 0.3,
     *     ...
     *   },
     *   "activeScorerCount": 5
     * }
     *
     * @return Algorithm configuration
     */
    @GetMapping("/algorithm/info")
    public ResponseEntity<Map<String, Object>> getAlgorithmInfo() {
        log.info("GET /api/recommendations/algorithm/info");

        Map<String, Object> info = recommendationService.getAlgorithmInfo();

        return ResponseEntity.ok(info);
    }
}
