package com.dating.chat.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Fallback implementation for MatchServiceClient
 *
 * Provides default responses when match-service is unavailable
 */
@Component
@Slf4j
public class MatchServiceClientFallback implements MatchServiceClient {

    @Override
    public Boolean matchExists(UUID user1Id, UUID user2Id) {
        log.warn("MatchService unavailable, assuming match exists for users: {} and {}", user1Id, user2Id);
        return true; // Assume match exists to avoid blocking chat functionality
    }

    @Override
    public MatchDTO getMatchById(UUID matchId) {
        log.warn("MatchService unavailable, returning default match info for matchId: {}", matchId);
        return new MatchDTO(matchId, null, null, "UNKNOWN");
    }
}
