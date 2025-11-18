package com.dating.ui.service;

import com.dating.ui.client.MatchServiceClient;
import com.dating.ui.dto.Match;
import com.dating.ui.dto.SwipeRequest;
import com.dating.ui.dto.SwipeResponse;
import com.dating.ui.dto.SwipeType;
import com.dating.ui.dto.User;
import com.dating.ui.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for match-related operations
 * Handles swiping, matching, and profile discovery
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {

    private final MatchServiceClient matchClient;

    /**
     * Get next profile to swipe on
     */
    public User getNextProfile() {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return matchClient.getNextProfile("Bearer " + token);
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
        SwipeResponse response = matchClient.recordSwipe(request, "Bearer " + token);

        if (response.isMatch()) {
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

        return matchClient.getMyMatches("Bearer " + token);
    }

    /**
     * Get specific match details
     */
    public Match getMatch(String matchId) {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return matchClient.getMatch(matchId, "Bearer " + token);
    }

    /**
     * Get match details - alias for getMatch
     */
    public Match getMatchDetails(String matchId) {
        return getMatch(matchId);
    }

    /**
     * Unmatch with a user
     */
    public void unmatch(String matchId) {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        matchClient.unmatch(matchId, "Bearer " + token);
        log.info("Unmatched from match: {}", matchId);
    }

    /**
     * Undo last swipe
     */
    public void undoLastSwipe() {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        matchClient.undoLastSwipe("Bearer " + token);
        log.info("Last swipe undone for user: {}", SecurityUtils.getCurrentUserId());
    }
}
