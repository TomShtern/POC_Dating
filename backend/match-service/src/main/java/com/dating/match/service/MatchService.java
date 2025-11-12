package com.dating.match.service;

import com.dating.match.client.UserServiceClient;
import com.dating.match.dto.MatchDTO;
import com.dating.match.dto.UserSummaryDTO;
import com.dating.match.entity.Match;
import com.dating.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for match operations
 *
 * PURPOSE: Manage matches between users
 *
 * RESPONSIBILITIES:
 * - Get user's matches with profile data
 * - Unmatch users (deactivate match)
 * - Get match details
 * - Count user's active matches
 *
 * DATA ENRICHMENT:
 * - Fetches user profiles from user-service via Feign
 * - Batch loading for performance (avoid N+1 queries)
 * - Returns MatchDTO with user summaries
 *
 * UNMATCH BEHAVIOR:
 * - Soft delete (sets isActive=false)
 * - Preserves data for analytics
 * - Can be hard-deleted by admin if needed
 * - Both users lose access to chat
 *
 * PERFORMANCE:
 * - Batch load user profiles (single Feign call)
 * - Consider caching match list in Redis
 * - Index on (user1Id, user2Id, isActive)
 *
 * ALTERNATIVES:
 * - Denormalize user data in Match table: Stale data issues
 * - Lazy load profiles: N+1 query problem
 * - Event-driven sync: More complex, eventual consistency
 *
 * RATIONALE:
 * - Real-time user data ensures accuracy
 * - Batch loading is efficient
 * - Soft delete preserves analytics data
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserServiceClient userServiceClient;

    /**
     * Get all active matches for a user
     *
     * @param userId User ID (from JWT)
     * @return List of matches with user profiles
     */
    @Transactional(readOnly = true)
    public List<MatchDTO> getUserMatches(Long userId) {
        log.info("Getting matches for user: {}", userId);

        // Get all active matches
        List<Match> matches = matchRepository.findActiveMatchesByUserId(userId);

        if (matches.isEmpty()) {
            log.debug("No matches found for user: {}", userId);
            return new ArrayList<>();
        }

        // Extract matched user IDs
        List<Long> matchedUserIds = matches.stream()
            .map(match -> match.getOtherUserId(userId))
            .collect(Collectors.toList());

        log.debug("Found {} matches for user: {}", matchedUserIds.size(), userId);

        // Batch fetch user profiles
        Map<Long, UserSummaryDTO> userProfiles;
        try {
            List<UserSummaryDTO> profiles = userServiceClient.getUserSummaries(matchedUserIds);
            userProfiles = profiles.stream()
                .collect(Collectors.toMap(UserSummaryDTO::getUserId, profile -> profile));
        } catch (Exception e) {
            log.error("Error fetching user profiles: {}", e.getMessage());
            throw new IllegalStateException("Unable to fetch user profiles", e);
        }

        // Build MatchDTOs
        return matches.stream()
            .map(match -> {
                Long matchedUserId = match.getOtherUserId(userId);
                UserSummaryDTO userProfile = userProfiles.get(matchedUserId);

                return MatchDTO.builder()
                    .matchId(match.getId())
                    .matchedUser(userProfile)
                    .matchedAt(match.getMatchedAt())
                    .isActive(match.getIsActive())
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Get match details by ID
     *
     * @param matchId Match ID
     * @param userId User requesting match (for authorization)
     * @return Match details with user profile
     */
    @Transactional(readOnly = true)
    public MatchDTO getMatchById(Long matchId, Long userId) {
        log.info("Getting match details: matchId={}, userId={}", matchId, userId);

        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));

        // Verify user is part of this match
        if (!match.includesUser(userId)) {
            throw new IllegalArgumentException("User is not part of this match");
        }

        // Fetch other user's profile
        Long otherUserId = match.getOtherUserId(userId);
        UserSummaryDTO userProfile;
        try {
            userProfile = userServiceClient.getUserSummary(otherUserId);
        } catch (Exception e) {
            log.error("Error fetching user profile: {}", e.getMessage());
            throw new IllegalStateException("Unable to fetch user profile", e);
        }

        return MatchDTO.builder()
            .matchId(match.getId())
            .matchedUser(userProfile)
            .matchedAt(match.getMatchedAt())
            .isActive(match.getIsActive())
            .build();
    }

    /**
     * Unmatch users (deactivate match)
     *
     * @param matchId Match ID
     * @param userId User requesting unmatch (for authorization)
     */
    @Transactional
    public void unmatch(Long matchId, Long userId) {
        log.info("Unmatching: matchId={}, userId={}", matchId, userId);

        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));

        // Verify user is part of this match
        if (!match.includesUser(userId)) {
            throw new IllegalArgumentException("User is not part of this match");
        }

        // Check if already inactive
        if (!match.getIsActive()) {
            log.warn("Match already inactive: {}", matchId);
            return;
        }

        // Deactivate match (soft delete)
        match.setIsActive(false);
        matchRepository.save(match);

        log.info("Match deactivated: {}", matchId);
        // TODO: Publish match:ended event to notify chat service
    }

    /**
     * Count active matches for a user
     *
     * @param userId User ID
     * @return Number of active matches
     */
    @Transactional(readOnly = true)
    public long countUserMatches(Long userId) {
        return matchRepository.countActiveMatchesByUserId(userId);
    }

    /**
     * Check if users are matched
     *
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return true if active match exists
     */
    @Transactional(readOnly = true)
    public boolean areUsersMatched(Long userId1, Long userId2) {
        return matchRepository.existsActiveMatch(userId1, userId2);
    }
}
