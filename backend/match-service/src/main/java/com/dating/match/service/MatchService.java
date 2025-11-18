package com.dating.match.service;

import com.dating.common.exception.MatchNotFoundException;
import com.dating.match.client.UserServiceClient;
import com.dating.match.config.CacheConfig;
import com.dating.match.dto.response.MatchDetailResponse;
import com.dating.match.dto.response.MatchListResponse;
import com.dating.match.dto.response.MatchResponse;
import com.dating.match.event.MatchEventPublisher;
import com.dating.match.exception.UnauthorizedMatchAccessException;
import com.dating.match.model.Match;
import com.dating.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Service for match management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserServiceClient userServiceClient;
    private final MatchEventPublisher eventPublisher;

    /**
     * Get all active matches for a user.
     *
     * @param userId User ID
     * @param limit Page size
     * @param offset Page offset
     * @return List of matches
     */
    @Cacheable(value = CacheConfig.MATCHES_CACHE, key = "#userId + '_' + #limit + '_' + #offset")
    @Transactional(readOnly = true)
    public MatchListResponse getMatches(UUID userId, int limit, int offset) {
        log.debug("Getting matches for user {} with limit {} and offset {}", userId, limit, offset);

        int page = offset / limit;
        Page<Match> matchPage = matchRepository.findActiveMatchesByUserId(
                userId, PageRequest.of(page, limit));

        List<MatchResponse> matches = matchPage.getContent().stream()
                .map(match -> mapToMatchResponse(match, userId))
                .toList();

        long total = matchPage.getTotalElements();

        return new MatchListResponse(matches, total, limit, offset, matchPage.hasNext());
    }

    /**
     * Get match details.
     *
     * @param matchId Match ID
     * @param userId User ID (for authorization)
     * @return Match details
     */
    @Cacheable(value = CacheConfig.MATCH_DETAILS_CACHE, key = "#matchId")
    @Transactional(readOnly = true)
    public MatchDetailResponse getMatchDetails(UUID matchId, UUID userId) {
        log.debug("Getting match details for {} by user {}", matchId, userId);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match not found: " + matchId));

        // Verify user is part of the match
        if (!match.involvesUser(userId)) {
            throw new UnauthorizedMatchAccessException("You are not part of this match");
        }

        return mapToMatchDetailResponse(match);
    }

    /**
     * Unmatch (end a match).
     *
     * @param matchId Match ID
     * @param userId User ID (for authorization)
     */
    @CacheEvict(value = {CacheConfig.MATCHES_CACHE, CacheConfig.MATCH_DETAILS_CACHE}, allEntries = true)
    @Transactional
    public void unmatch(UUID matchId, UUID userId) {
        log.debug("Unmatching {} by user {}", matchId, userId);

        Match match = matchRepository.findByIdAndUserId(matchId, userId)
                .orElseThrow(() -> new MatchNotFoundException("Match not found or you are not part of it"));

        if (!match.isActive()) {
            throw new IllegalStateException("Match is already ended");
        }

        // Soft delete - set ended_at
        match.setEndedAt(Instant.now());
        matchRepository.save(match);

        log.info("Match {} ended by user {}", matchId, userId);

        // Publish match ended event
        eventPublisher.publishMatchEnded(match, userId);
    }

    /**
     * Map Match entity to MatchResponse DTO.
     */
    private MatchResponse mapToMatchResponse(Match match, UUID currentUserId) {
        UUID otherUserId = match.getOtherUserId(currentUserId);

        // Get other user's info
        MatchResponse.MatchedUserInfo userInfo = getMatchedUserInfo(otherUserId);

        return new MatchResponse(match.getId(), userInfo, match.getMatchedAt());
    }

    /**
     * Get matched user info.
     */
    private MatchResponse.MatchedUserInfo getMatchedUserInfo(UUID userId) {
        try {
            var user = userServiceClient.getUserById(userId);
            String name = (user.firstName() != null ? user.firstName() : "") +
                    (user.lastName() != null ? " " + user.lastName() : "");

            return new MatchResponse.MatchedUserInfo(
                    user.id(),
                    name.trim().isEmpty() ? user.username() : name.trim(),
                    user.profilePictureUrl(),
                    null, // Last message would come from chat-service
                    null,
                    0
            );
        } catch (Exception e) {
            log.warn("Failed to get user info for {}: {}", userId, e.getMessage());
            return new MatchResponse.MatchedUserInfo(
                    userId, "Unknown User", null, null, null, 0);
        }
    }

    /**
     * Map Match entity to MatchDetailResponse DTO.
     */
    private MatchDetailResponse mapToMatchDetailResponse(Match match) {
        var user1Info = getUserInfo(match.getUser1Id());
        var user2Info = getUserInfo(match.getUser2Id());

        BigDecimal score = match.getMatchScore() != null ?
                match.getMatchScore().getScore() : BigDecimal.ZERO;

        Map<String, Object> factors = match.getMatchScore() != null ?
                match.getMatchScore().getFactors() : new HashMap<>();

        return new MatchDetailResponse(
                match.getId(),
                user1Info,
                user2Info,
                score,
                factors,
                match.getMatchedAt()
        );
    }

    /**
     * Get basic user info for match details.
     */
    private MatchDetailResponse.UserInfo getUserInfo(UUID userId) {
        try {
            var user = userServiceClient.getUserById(userId);
            String name = (user.firstName() != null ? user.firstName() : "") +
                    (user.lastName() != null ? " " + user.lastName() : "");
            return new MatchDetailResponse.UserInfo(
                    user.id(),
                    name.trim().isEmpty() ? user.username() : name.trim()
            );
        } catch (Exception e) {
            log.warn("Failed to get user info for {}: {}", userId, e.getMessage());
            return new MatchDetailResponse.UserInfo(userId, "Unknown User");
        }
    }
}
