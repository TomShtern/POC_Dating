package com.dating.ui.client;

import com.dating.ui.dto.FeedResponse;
import com.dating.ui.dto.Match;
import com.dating.ui.dto.MatchListResponse;
import com.dating.ui.dto.SwipeRequest;
import com.dating.ui.dto.SwipeResponse;
import com.dating.ui.dto.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Feign Client for Match Service
 * Handles swiping, matching, and profile discovery
 */
@FeignClient(name = "match-service", url = "${services.match-service.url}")
public interface MatchServiceClient {

    /**
     * Get next profile for swiping.
     */
    @GetMapping("/api/matches/next")
    User getNextProfile(@RequestHeader("Authorization") String token);

    /**
     * Get feed of potential matches for a user.
     */
    @GetMapping("/api/matches/feed/{userId}")
    FeedResponse getFeed(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") UUID requestUserId,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset);

    /**
     * Record a swipe action.
     */
    @PostMapping("/api/matches/swipes")
    SwipeResponse recordSwipe(
            @RequestBody SwipeRequest request,
            @RequestHeader("Authorization") String token);

    /**
     * Get all matches for current user.
     */
    @GetMapping("/api/matches")
    List<Match> getMyMatches(@RequestHeader("Authorization") String token);

    /**
     * Get match details.
     */
    @GetMapping("/api/matches/{matchId}")
    Match getMatch(@PathVariable String matchId, @RequestHeader("Authorization") String token);

    @DeleteMapping("/api/matches/{matchId}")
    void unmatch(@PathVariable String matchId, @RequestHeader("Authorization") String token);

    @PostMapping("/api/matches/undo")
    void undoLastSwipe(@RequestHeader("Authorization") String token);
}
