package com.dating.ui.client;

import com.dating.ui.dto.Match;
import com.dating.ui.dto.SwipeRequest;
import com.dating.ui.dto.SwipeResponse;
import com.dating.ui.dto.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feign Client for Match Service
 * Handles swiping, matching, and profile discovery
 */
@FeignClient(name = "match-service", url = "${services.match-service.url}")
public interface MatchServiceClient {

    @GetMapping("/api/matches/next-profile")
    User getNextProfile(@RequestHeader("Authorization") String token);

    @PostMapping("/api/matches/swipe")
    SwipeResponse recordSwipe(@RequestBody SwipeRequest request,
                              @RequestHeader("Authorization") String token);

    @GetMapping("/api/matches/my-matches")
    List<Match> getMyMatches(@RequestHeader("Authorization") String token);

    @GetMapping("/api/matches/{matchId}")
    Match getMatch(@PathVariable String matchId, @RequestHeader("Authorization") String token);

    @DeleteMapping("/api/matches/{matchId}")
    void unmatch(@PathVariable String matchId, @RequestHeader("Authorization") String token);

    @PostMapping("/api/matches/undo")
    void undoLastSwipe(@RequestHeader("Authorization") String token);
}
