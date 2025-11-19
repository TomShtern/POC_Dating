package com.dating.ui.service;

import com.dating.ui.client.MatchServiceClient;
import com.dating.ui.dto.Match;
import com.dating.ui.dto.SwipeRequest;
import com.dating.ui.dto.SwipeResponse;
import com.dating.ui.dto.SwipeType;
import com.dating.ui.dto.User;
import com.dating.ui.exception.ServiceException;
import com.dating.ui.security.SecurityUtils;
import feign.FeignException;
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
            throw new ServiceException("User not authenticated");
        }

        try {
            User profile = matchClient.getNextProfile("Bearer " + token);
            // Profile can be null if no more profiles available - this is valid
            return profile;
        } catch (FeignException e) {
            log.error("Failed to get next profile for user: {}", SecurityUtils.getCurrentUserId(), e);
            throw new ServiceException("Unable to load profiles. Please try again.", e);
        }
    }

    /**
     * Record a swipe (like, pass, super like)
     */
    public SwipeResponse recordSwipe(String targetUserId, SwipeType swipeType) {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new ServiceException("User not authenticated");
        }

        try {
            SwipeRequest request = new SwipeRequest(targetUserId, swipeType);
            SwipeResponse response = matchClient.recordSwipe(request, "Bearer " + token);

            if (response == null) {
                throw new ServiceException("Failed to record swipe");
            }

            if (response.isMatch()) {
                log.info("Match created! User: {} matched with: {}",
                    SecurityUtils.getCurrentUserId(), targetUserId);
            }

            return response;
        } catch (FeignException e) {
            log.error("Failed to record swipe for user {} on target {}",
                SecurityUtils.getCurrentUserId(), targetUserId, e);
            if (e.status() == 429) {
                throw new ServiceException("You've reached your daily swipe limit. Please try again tomorrow.", e);
            }
            throw new ServiceException("Unable to record swipe. Please try again.", e);
        }
    }

    /**
     * Get all matches for current user
     */
    public List<Match> getMyMatches() {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new ServiceException("User not authenticated");
        }

        try {
            List<Match> matches = matchClient.getMyMatches("Bearer " + token);
            if (matches == null) {
                throw new ServiceException("Failed to retrieve matches");
            }
            return matches;
        } catch (FeignException e) {
            log.error("Failed to get matches for user: {}", SecurityUtils.getCurrentUserId(), e);
            throw new ServiceException("Unable to load matches. Please try again.", e);
        }
    }

    /**
     * Get specific match details
     */
    public Match getMatch(String matchId) {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new ServiceException("User not authenticated");
        }

        try {
            Match match = matchClient.getMatch(matchId, "Bearer " + token);
            if (match == null) {
                throw new ServiceException("Match not found");
            }
            return match;
        } catch (FeignException e) {
            log.error("Failed to get match: {} for user: {}", matchId, SecurityUtils.getCurrentUserId(), e);
            if (e.status() == 404) {
                throw new ServiceException("Match not found", e);
            }
            throw new ServiceException("Unable to load match details. Please try again.", e);
        }
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
            throw new ServiceException("User not authenticated");
        }

        try {
            matchClient.unmatch(matchId, "Bearer " + token);
            log.info("Unmatched from match: {}", matchId);
        } catch (FeignException e) {
            log.error("Failed to unmatch: {} for user: {}", matchId, SecurityUtils.getCurrentUserId(), e);
            if (e.status() == 404) {
                throw new ServiceException("Match not found", e);
            }
            throw new ServiceException("Unable to unmatch. Please try again.", e);
        }
    }

    /**
     * Undo last swipe
     */
    public void undoLastSwipe() {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new ServiceException("User not authenticated");
        }

        try {
            matchClient.undoLastSwipe("Bearer " + token);
            log.info("Last swipe undone for user: {}", SecurityUtils.getCurrentUserId());
        } catch (FeignException e) {
            log.error("Failed to undo last swipe for user: {}", SecurityUtils.getCurrentUserId(), e);
            if (e.status() == 404) {
                throw new ServiceException("No swipe to undo", e);
            }
            if (e.status() == 400) {
                throw new ServiceException("Cannot undo this swipe", e);
            }
            throw new ServiceException("Unable to undo swipe. Please try again.", e);
        }
    }
}
