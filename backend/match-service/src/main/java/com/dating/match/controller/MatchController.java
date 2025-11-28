package com.dating.match.controller;

import com.dating.match.dto.response.MatchDetailResponse;
import com.dating.match.dto.response.MatchListResponse;
import com.dating.match.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for match management operations.
 */
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Slf4j
public class MatchController {

    private final MatchService matchService;

    /**
     * Get all matches for current user.
     *
     * @param userId User ID from X-User-Id header
     * @param limit Page size (default 20)
     * @param offset Page offset (default 0)
     * @return List of matches
     */
    @GetMapping
    public ResponseEntity<MatchListResponse> getMatches(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {
        log.debug("Get matches request from user {}", userId);

        MatchListResponse response = matchService.getMatches(userId, limit, offset);
        return ResponseEntity.ok(response);
    }

    /**
     * Get match details.
     *
     * @param userId User ID from X-User-Id header
     * @param matchId Match ID
     * @return Match details
     */
    @GetMapping("/{matchId}")
    public ResponseEntity<MatchDetailResponse> getMatchDetails(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID matchId) {
        log.debug("Get match details request for {} from user {}", matchId, userId);

        MatchDetailResponse response = matchService.getMatchDetails(matchId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Unmatch (end a match).
     *
     * @param userId User ID from X-User-Id header
     * @param matchId Match ID
     * @return 204 No Content
     */
    @DeleteMapping("/{matchId}")
    public ResponseEntity<Void> unmatch(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID matchId) {
        log.debug("Unmatch request for {} from user {}", matchId, userId);

        matchService.unmatch(matchId, userId);
        return ResponseEntity.noContent().build();
    }
}
