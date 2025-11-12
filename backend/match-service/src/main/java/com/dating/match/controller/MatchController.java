package com.dating.match.controller;

import com.dating.match.dto.MatchDTO;
import com.dating.match.dto.SwipeRequest;
import com.dating.match.dto.SwipeResponse;
import com.dating.match.dto.UserSummaryDTO;
import com.dating.match.service.MatchService;
import com.dating.match.service.RecommendationService;
import com.dating.match.service.SwipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Match Service
 *
 * PURPOSE: Handle all match-related HTTP requests
 *
 * BASE PATH: /api/matches
 *
 * ENDPOINTS:
 * - POST   /swipe              → Record a swipe
 * - GET    /next-profile       → Get next profile to swipe on
 * - GET    /recommendations    → Get multiple recommendations
 * - GET    /my-matches         → Get user's matches
 * - GET    /{matchId}          → Get match details
 * - DELETE /{matchId}          → Unmatch
 *
 * AUTHENTICATION:
 * - All endpoints require JWT token
 * - User ID extracted from Authentication principal
 * - No need to pass userId in request body/params
 *
 * ERROR HANDLING:
 * - 400 Bad Request: Invalid input
 * - 401 Unauthorized: No/invalid JWT token
 * - 404 Not Found: Match/user not found
 * - 409 Conflict: Duplicate swipe
 * - 500 Internal Server Error: Unexpected errors
 *
 * DESIGN DECISIONS:
 * - RESTful design (standard HTTP methods)
 * - JSON request/response bodies
 * - @Valid for automatic validation
 * - Authentication principal for user ID
 * - ResponseEntity for flexible status codes
 *
 * ALTERNATIVES:
 * - GraphQL: More complex, overkill for simple CRUD
 * - RPC-style: Less standard, harder to document
 * - SOAP: Legacy, verbose
 *
 * RATIONALE:
 * - REST is industry standard for microservices
 * - Spring MVC provides excellent REST support
 * - Easy to test and document (Swagger/OpenAPI)
 */
@Slf4j
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final SwipeService swipeService;
    private final MatchService matchService;
    private final RecommendationService recommendationService;

    /**
     * Record a swipe action
     *
     * POST /api/matches/swipe
     * Authorization: Bearer <token>
     * Body: { "targetUserId": 123, "swipeType": "LIKE" }
     *
     * @param authentication JWT authentication (contains user ID)
     * @param request Swipe request
     * @return Swipe response with match status
     */
    @PostMapping("/swipe")
    public ResponseEntity<SwipeResponse> swipe(
        Authentication authentication,
        @Valid @RequestBody SwipeRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Swipe request: userId={}, targetUserId={}, type={}",
            userId, request.getTargetUserId(), request.getSwipeType());

        try {
            SwipeResponse response = swipeService.recordSwipe(userId, request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid swipe request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (IllegalStateException e) {
            log.warn("Duplicate swipe attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        } catch (Exception e) {
            log.error("Error recording swipe: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get next profile to swipe on
     *
     * GET /api/matches/next-profile
     * Authorization: Bearer <token>
     *
     * @param authentication JWT authentication (contains user ID)
     * @return Next user profile or 204 if no more profiles
     */
    @GetMapping("/next-profile")
    public ResponseEntity<UserSummaryDTO> getNextProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get next profile request: userId={}", userId);

        try {
            UserSummaryDTO profile = recommendationService.getNextProfile(userId);

            if (profile == null) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            log.error("Error getting next profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get multiple profile recommendations
     *
     * GET /api/matches/recommendations?limit=20
     * Authorization: Bearer <token>
     *
     * @param authentication JWT authentication (contains user ID)
     * @param limit Max number of profiles to return (default: 20)
     * @return List of recommended user profiles
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<UserSummaryDTO>> getRecommendations(
        Authentication authentication,
        @RequestParam(defaultValue = "20") Integer limit
    ) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get recommendations request: userId={}, limit={}", userId, limit);

        try {
            List<UserSummaryDTO> recommendations = recommendationService.getRecommendations(userId, limit);
            return ResponseEntity.ok(recommendations);

        } catch (Exception e) {
            log.error("Error getting recommendations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user's matches
     *
     * GET /api/matches/my-matches
     * Authorization: Bearer <token>
     *
     * @param authentication JWT authentication (contains user ID)
     * @return List of user's matches with profiles
     */
    @GetMapping("/my-matches")
    public ResponseEntity<List<MatchDTO>> getMyMatches(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get matches request: userId={}", userId);

        try {
            List<MatchDTO> matches = matchService.getUserMatches(userId);
            return ResponseEntity.ok(matches);

        } catch (Exception e) {
            log.error("Error getting matches: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get match details
     *
     * GET /api/matches/{matchId}
     * Authorization: Bearer <token>
     *
     * @param authentication JWT authentication (contains user ID)
     * @param matchId Match ID
     * @return Match details with user profile
     */
    @GetMapping("/{matchId}")
    public ResponseEntity<MatchDTO> getMatch(
        Authentication authentication,
        @PathVariable Long matchId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get match details: userId={}, matchId={}", userId, matchId);

        try {
            MatchDTO match = matchService.getMatchById(matchId, userId);
            return ResponseEntity.ok(match);

        } catch (IllegalArgumentException e) {
            log.warn("Match not found or unauthorized: {}", e.getMessage());
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error getting match details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Unmatch users
     *
     * DELETE /api/matches/{matchId}
     * Authorization: Bearer <token>
     *
     * @param authentication JWT authentication (contains user ID)
     * @param matchId Match ID to deactivate
     * @return 204 No Content on success
     */
    @DeleteMapping("/{matchId}")
    public ResponseEntity<Void> unmatch(
        Authentication authentication,
        @PathVariable Long matchId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Unmatch request: userId={}, matchId={}", userId, matchId);

        try {
            matchService.unmatch(matchId, userId);
            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            log.warn("Match not found or unauthorized: {}", e.getMessage());
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error unmatching: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get users who liked current user (premium feature)
     *
     * GET /api/matches/who-liked-me
     * Authorization: Bearer <token>
     *
     * @param authentication JWT authentication (contains user ID)
     * @return List of users who liked current user
     */
    @GetMapping("/who-liked-me")
    public ResponseEntity<List<UserSummaryDTO>> getWhoLikedMe(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get who liked me request: userId={}", userId);

        try {
            List<UserSummaryDTO> users = recommendationService.getUsersWhoLikedMe(userId);
            return ResponseEntity.ok(users);

        } catch (Exception e) {
            log.error("Error getting who liked me: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint
     *
     * GET /api/matches/health
     *
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Match Service is running");
    }
}
