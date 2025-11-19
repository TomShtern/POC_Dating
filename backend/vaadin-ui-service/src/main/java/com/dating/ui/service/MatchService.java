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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service layer for match-related operations
 * Handles swiping, matching, and profile discovery
 */
@Service
@Slf4j
public class MatchService {

    private final MatchServiceClient matchClient;
    private final MeterRegistry meterRegistry;
    private final Timer apiCallTimer;
    private final Timer feedGenerationTimer;
    private final Map<String, Counter> swipeCounters;
    private final Counter matchCounter;

    public MatchService(MatchServiceClient matchClient, MeterRegistry meterRegistry) {
        this.matchClient = matchClient;
        this.meterRegistry = meterRegistry;
        this.swipeCounters = new ConcurrentHashMap<>();
        this.apiCallTimer = Timer.builder("ui.api.call.time")
            .description("Time spent calling backend services")
            .tag("service", "match-service")
            .register(meterRegistry);
        this.feedGenerationTimer = Timer.builder("ui.feed.generation.time")
            .description("Time spent generating/fetching user feed")
            .register(meterRegistry);
        this.matchCounter = Counter.builder("ui.matches.total")
            .description("Total number of matches created")
            .register(meterRegistry);
    }

    private Counter getSwipeCounter(SwipeType swipeType) {
        return swipeCounters.computeIfAbsent(swipeType.name(), key ->
            Counter.builder("ui.swipes.total")
                .description("Total number of swipes by direction")
                .tag("direction", swipeType.name().toLowerCase())
                .register(meterRegistry)
        );
    }

    /**
     * Get next profile to swipe on
     */
    public User getNextProfile() {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return feedGenerationTimer.record(() -> matchClient.getNextProfile("Bearer " + token));
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
        SwipeResponse response;
        try {
            response = apiCallTimer.record(() -> matchClient.recordSwipe(request, "Bearer " + token));
        } catch (Exception e) {
            // Still record the swipe attempt even on failure
            getSwipeCounter(swipeType).increment();
            log.warn("Failed to record swipe for target user: {}", targetUserId, e);
            throw e;
        }

        getSwipeCounter(swipeType).increment();

        if (response.isMatch()) {
            matchCounter.increment();
            log.debug("Match created with target user: {}", targetUserId);
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
