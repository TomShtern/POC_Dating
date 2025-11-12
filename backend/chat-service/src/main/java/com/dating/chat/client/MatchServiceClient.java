package com.dating.chat.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Feign Client for Match Service
 *
 * Communicates with match-service to verify matches and user relationships
 */
@FeignClient(
        name = "match-service",
        url = "${feign.match-service.url}",
        fallback = MatchServiceClientFallback.class
)
public interface MatchServiceClient {

    /**
     * Check if a match exists between two users
     */
    @GetMapping("/api/matches/exists")
    Boolean matchExists(@RequestParam("user1Id") UUID user1Id, @RequestParam("user2Id") UUID user2Id);

    /**
     * Get match details by match ID
     */
    @GetMapping("/api/matches/{matchId}")
    MatchDTO getMatchById(@PathVariable("matchId") UUID matchId);

    /**
     * Simple DTO for match information
     */
    record MatchDTO(
            UUID id,
            UUID user1Id,
            UUID user2Id,
            String status
    ) {}
}
