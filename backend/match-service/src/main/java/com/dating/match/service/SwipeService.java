package com.dating.match.service;

import com.dating.common.constant.SwipeType;
import com.dating.match.client.UserServiceClient;
import com.dating.match.config.CacheConfig;
import com.dating.match.dto.request.SwipeRequest;
import com.dating.match.dto.response.SwipeResponse;
import com.dating.match.event.MatchEventPublisher;
import com.dating.match.exception.DuplicateSwipeException;
import com.dating.match.exception.InvalidSwipeException;
import com.dating.match.model.Match;
import com.dating.match.model.MatchScore;
import com.dating.match.model.Swipe;
import com.dating.match.repository.MatchRepository;
import com.dating.match.repository.SwipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling swipe operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SwipeService {

    private final SwipeRepository swipeRepository;
    private final MatchRepository matchRepository;
    private final UserServiceClient userServiceClient;
    private final MatchEventPublisher eventPublisher;

    /**
     * Record a swipe action and check for mutual match.
     *
     * @param userId Current user ID
     * @param request Swipe request
     * @return Swipe response with match information if applicable
     */
    @CacheEvict(value = CacheConfig.FEED_CACHE, key = "#userId")
    @Transactional
    public SwipeResponse recordSwipe(UUID userId, SwipeRequest request) {
        log.debug("Recording swipe from {} to {} with action {}",
                userId, request.targetUserId(), request.action());

        // Validate swipe
        validateSwipe(userId, request.targetUserId());

        // Check for existing swipe
        if (swipeRepository.existsByUserIdAndTargetUserId(userId, request.targetUserId())) {
            throw new DuplicateSwipeException("You have already swiped on this user");
        }

        // Create swipe
        Swipe swipe = Swipe.builder()
                .userId(userId)
                .targetUserId(request.targetUserId())
                .action(request.action())
                .build();

        swipe = swipeRepository.save(swipe);
        log.info("Swipe recorded: {} -> {} ({})", userId, request.targetUserId(), request.action());

        // Check for mutual match if this is a positive swipe
        if (request.action().canMatch()) {
            Optional<Swipe> mutualSwipe = swipeRepository.findMutualLike(
                    request.targetUserId(), userId);

            if (mutualSwipe.isPresent()) {
                // Create match
                Match match = createMatch(userId, request.targetUserId());
                log.info("Match created between {} and {}: {}",
                        userId, request.targetUserId(), match.getId());

                return SwipeResponse.withMatch(
                        swipe.getId(),
                        userId,
                        request.targetUserId(),
                        request.action(),
                        match.getId(),
                        match.getMatchedAt(),
                        swipe.getCreatedAt());
            }
        }

        return SwipeResponse.noMatch(
                swipe.getId(),
                userId,
                request.targetUserId(),
                request.action(),
                swipe.getCreatedAt());
    }

    /**
     * Validate swipe constraints.
     */
    private void validateSwipe(UUID userId, UUID targetUserId) {
        // Cannot swipe on yourself
        if (userId.equals(targetUserId)) {
            throw new InvalidSwipeException("Cannot swipe on yourself");
        }

        // Check if target user exists and is active
        try {
            var targetUser = userServiceClient.getUserById(targetUserId);
            if (!"ACTIVE".equals(targetUser.status())) {
                throw new InvalidSwipeException("Target user is not active");
            }
        } catch (Exception e) {
            log.warn("Failed to verify target user: {}", e.getMessage());
            // Continue with swipe - user service might be down
        }
    }

    /**
     * Create a match between two users.
     */
    private Match createMatch(UUID userId, UUID targetUserId) {
        // Ensure user1Id < user2Id for consistent ordering
        UUID user1Id = userId.compareTo(targetUserId) < 0 ? userId : targetUserId;
        UUID user2Id = userId.compareTo(targetUserId) < 0 ? targetUserId : userId;

        // Check if match already exists
        if (matchRepository.existsActiveMatchBetweenUsers(user1Id, user2Id)) {
            return matchRepository.findActiveMatchBetweenUsers(user1Id, user2Id).orElseThrow();
        }

        // Create match
        Match match = Match.builder()
                .user1Id(user1Id)
                .user2Id(user2Id)
                .build();

        match = matchRepository.save(match);

        // Create initial match score
        MatchScore matchScore = MatchScore.builder()
                .match(match)
                .score(BigDecimal.valueOf(75)) // Default score
                .factors(createDefaultFactors())
                .build();

        match.setMatchScore(matchScore);
        match = matchRepository.save(match);

        // Publish match created event
        eventPublisher.publishMatchCreated(match, getUserName(user1Id), getUserName(user2Id));

        return match;
    }

    /**
     * Create default score factors.
     */
    private Map<String, Object> createDefaultFactors() {
        Map<String, Object> factors = new HashMap<>();
        factors.put("interestMatch", 30);
        factors.put("ageCompatibility", 25);
        factors.put("preferenceAlignment", 20);
        return factors;
    }

    /**
     * Get user's display name.
     */
    private String getUserName(UUID userId) {
        try {
            var user = userServiceClient.getUserById(userId);
            return user.firstName() != null ? user.firstName() : user.username();
        } catch (Exception e) {
            log.warn("Failed to get user name for {}: {}", userId, e.getMessage());
            return "User";
        }
    }
}
