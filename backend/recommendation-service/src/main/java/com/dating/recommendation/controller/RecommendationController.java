package com.dating.recommendation.controller;

import com.dating.recommendation.dto.RecommendedProfileDTO;
import com.dating.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for recommendation endpoints.
 *
 * Endpoints:
 * - GET /api/recommendations - Get all recommendations for the authenticated user
 * - GET /api/recommendations/next - Get the next recommendation
 * - POST /api/recommendations/refresh - Refresh recommendations
 * - GET /api/recommendations/stats - Get recommendation statistics
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * Get all recommendations for the authenticated user.
     *
     * @param authentication Spring Security authentication object
     * @return list of recommended profiles
     */
    @GetMapping
    public ResponseEntity<List<RecommendedProfileDTO>> getRecommendations(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("GET /api/recommendations - User: {}", userId);

        List<RecommendedProfileDTO> recommendations = recommendationService.getRecommendations(userId);

        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get the next recommendation (single profile).
     *
     * @param authentication Spring Security authentication object
     * @return the next recommended profile
     */
    @GetMapping("/next")
    public ResponseEntity<RecommendedProfileDTO> getNextRecommendation(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("GET /api/recommendations/next - User: {}", userId);

        RecommendedProfileDTO recommendation = recommendationService.getNextRecommendation(userId);

        if (recommendation == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(recommendation);
    }

    /**
     * Refresh recommendations for the authenticated user.
     * Useful after preferences change or to get fresh recommendations.
     *
     * @param authentication Spring Security authentication object
     * @return refreshed list of recommended profiles
     */
    @PostMapping("/refresh")
    public ResponseEntity<List<RecommendedProfileDTO>> refreshRecommendations(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("POST /api/recommendations/refresh - User: {}", userId);

        List<RecommendedProfileDTO> recommendations = recommendationService.refreshRecommendations(userId);

        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get recommendation statistics.
     *
     * @param authentication Spring Security authentication object
     * @return map of statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRecommendationStats(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("GET /api/recommendations/stats - User: {}", userId);

        Map<String, Object> stats = recommendationService.getRecommendationStats(userId);

        return ResponseEntity.ok(stats);
    }
}
