package com.dating.ui.service;

import com.dating.ui.client.MatchServiceClient;
import com.dating.ui.dto.Match;
import com.dating.ui.dto.SwipeRequest;
import com.dating.ui.dto.SwipeResponse;
import com.dating.ui.dto.SwipeType;
import com.dating.ui.dto.User;
import com.dating.ui.security.SecurityUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for match-related operations
 * Handles swiping, matching, and profile discovery
 */
@Service
@Slf4j
public class MatchService {

    private final MatchServiceClient matchClient;
    private final Timer apiCallTimer;
    private final Counter swipeCounter;
    private final Counter matchCounter;

    public MatchService(MatchServiceClient matchClient, MeterRegistry meterRegistry) {
        this.matchClient = matchClient;
        this.apiCallTimer = Timer.builder("ui.api.call.time")
            .description("Time spent calling backend services")
            .tag("service", "match-service")
            .register(meterRegistry);
        this.swipeCounter = Counter.builder("ui.swipes.total")
            .description("Total number of swipes")
            .register(meterRegistry);
        this.matchCounter = Counter.builder("ui.matches.total")
            .description("Total number of matches created")
            .register(meterRegistry);
    }

    /**
     * Get next profile to swipe on
     */
    public User getNextProfile() {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return apiCallTimer.record(() -> matchClient.getNextProfile("Bearer " + token));
    }

    /**
     * Record a swipe (like, pass, super like)
     */
    public SwipeResponse recordSwipe(String targetUserId, SwipeType swipeType) {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        SwipeRequest request = new SwipeRequest(targetUserId, swipeType);
        SwipeResponse response = apiCallTimer.record(() -> matchClient.recordSwipe(request, "Bearer " + token));

        swipeCounter.increment();

        if (response.isMatch()) {
            matchCounter.increment();
            log.info("Match created! User: {} matched with: {}",
                SecurityUtils.getCurrentUserId(), targetUserId);
        }

        return response;
    }

    /**
     * Get all matches for current user
     */
    public List<Match> getMyMatches() {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return apiCallTimer.record(() -> matchClient.getMyMatches("Bearer " + token));
    }

    /**
     * Get specific match details
     */
    public Match getMatch(String matchId) {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return apiCallTimer.record(() -> matchClient.getMatch(matchId, "Bearer " + token));
    }
}
