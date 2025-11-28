package com.dating.recommendation.controller;

import com.dating.common.exception.UnauthorizedException;
import com.dating.recommendation.dto.request.ScoreRequest;
import com.dating.recommendation.dto.response.BatchScoreResponse;
import com.dating.recommendation.dto.response.RecommendationListResponse;
import com.dating.recommendation.dto.response.ScoreResponse;
import com.dating.recommendation.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for recommendation endpoints.
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
     * @param userId User ID
     * @param limit Maximum number of recommendations
     * @param algorithm Algorithm version
     * @param httpRequest HTTP request for authorization
     * @return List of recommendations
     */
    @GetMapping("/{userId}")
    public ResponseEntity<RecommendationListResponse> getRecommendations(
            @PathVariable UUID userId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String algorithm,
            HttpServletRequest httpRequest) {

        log.info("GET /api/recommendations/{} - limit: {}, algorithm: {}",
                userId, limit, algorithm);

        // Authorization check - user can only get their own recommendations
        UUID requestingUserId = getRequestingUserId(httpRequest);
        if (!userId.equals(requestingUserId)) {
            log.warn("Unauthorized recommendation access: {} tried to access {}", requestingUserId, userId);
            throw new UnauthorizedException("You can only access your own recommendations");
        }

        RecommendationListResponse response = recommendationService.getRecommendations(
                userId, limit, algorithm);

        return ResponseEntity.ok(response);
    }

    /**
     * Get compatibility score between two users.
     *
     * @param userId Source user ID
     * @param targetUserId Target user ID
     * @return Compatibility score
     */
    @GetMapping("/{userId}/{targetUserId}/score")
    public ResponseEntity<ScoreResponse> getCompatibilityScore(
            @PathVariable UUID userId,
            @PathVariable UUID targetUserId) {

        log.info("GET /api/recommendations/{}/{}/score", userId, targetUserId);

        ScoreResponse response = recommendationService.getCompatibilityScore(userId, targetUserId);

        return ResponseEntity.ok(response);
    }

    /**
     * Score profiles for match-service (internal API).
     *
     * @param request Score request with user ID and candidate IDs
     * @return Batch scores
     */
    @PostMapping("/score")
    public ResponseEntity<BatchScoreResponse> scoreProfiles(
            @Valid @RequestBody ScoreRequest request) {

        log.info("POST /api/recommendations/score - user: {}, candidates: {}",
                request.getUserId(), request.getCandidateIds().size());

        BatchScoreResponse response = recommendationService.scoreProfiles(
                request.getUserId(),
                request.getCandidateIds(),
                request.getAlgorithm());

        return ResponseEntity.ok(response);
    }

    /**
     * Refresh recommendations for a user.
     *
     * @param userId User ID
     * @param httpRequest HTTP request for authorization
     * @return No content
     */
    @PostMapping("/{userId}/refresh")
    public ResponseEntity<Void> refreshRecommendations(
            @PathVariable UUID userId,
            HttpServletRequest httpRequest) {
        log.info("POST /api/recommendations/{}/refresh", userId);

        // Authorization check - user can only refresh their own recommendations
        UUID requestingUserId = getRequestingUserId(httpRequest);
        if (!userId.equals(requestingUserId)) {
            log.warn("Unauthorized recommendation refresh: {} tried to refresh {}", requestingUserId, userId);
            throw new UnauthorizedException("You can only refresh your own recommendations");
        }

        recommendationService.refreshRecommendations(userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Extract requesting user ID from X-User-Id header.
     *
     * @param request HTTP request
     * @return User UUID
     */
    private UUID getRequestingUserId(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new UnauthorizedException("Missing X-User-Id header");
        }
        return UUID.fromString(userIdHeader);
    }

    /**
     * Clean up expired recommendations.
     *
     * @return Number of deleted recommendations
     */
    @DeleteMapping("/expired")
    public ResponseEntity<Integer> cleanupExpired() {
        log.info("DELETE /api/recommendations/expired");

        int deleted = recommendationService.cleanupExpiredRecommendations();

        return ResponseEntity.ok(deleted);
    }
}
