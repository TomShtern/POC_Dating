package com.dating.match.service;

import com.dating.match.client.UserServiceClient;
import com.dating.match.dto.SwipeRequest;
import com.dating.match.dto.SwipeResponse;
import com.dating.match.entity.Match;
import com.dating.match.entity.Swipe;
import com.dating.match.entity.SwipeType;
import com.dating.match.repository.MatchRepository;
import com.dating.match.repository.SwipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for swipe operations
 *
 * PURPOSE: Handle swipe actions and detect mutual matches
 *
 * RESPONSIBILITIES:
 * - Validate swipe requests
 * - Record swipes in database
 * - Detect mutual matches (bidirectional likes)
 * - Create match records when mutual like detected
 * - Return swipe response with match status
 *
 * MATCHING ALGORITHM:
 * 1. User A swipes right on User B
 * 2. Check if User B previously swiped right on User A
 * 3. If yes: Create Match record, return isMatch=true
 * 4. If no: Just record swipe, return isMatch=false
 *
 * VALIDATION:
 * - User cannot swipe on themselves
 * - Target user must exist and be active
 * - Cannot swipe twice on same user (duplicate check)
 *
 * TRANSACTIONALITY:
 * - @Transactional ensures swipe + match creation is atomic
 * - If match creation fails, swipe is rolled back
 * - Prevents data inconsistency
 *
 * ALTERNATIVES:
 * - Async match detection: Slower feedback, worse UX
 * - Event-driven: More complex, same result
 * - Batch processing: Terrible UX (no immediate feedback)
 *
 * RATIONALE:
 * - Real-time match detection is expected in dating apps
 * - Synchronous approach is simpler and more reliable
 * - Transaction guarantees data consistency
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SwipeService {

    private final SwipeRepository swipeRepository;
    private final MatchRepository matchRepository;
    private final UserServiceClient userServiceClient;

    /**
     * Record a swipe action
     *
     * @param userId User performing the swipe (from JWT)
     * @param request Swipe request with target user and type
     * @return Swipe response with match status
     */
    @Transactional
    public SwipeResponse recordSwipe(Long userId, SwipeRequest request) {
        log.info("Recording swipe: user {} -> user {} ({})",
            userId, request.getTargetUserId(), request.getSwipeType());

        // Validation
        validateSwipe(userId, request);

        // Check if swipe already exists
        if (swipeRepository.existsByUserIdAndTargetUserId(userId, request.getTargetUserId())) {
            throw new IllegalStateException("User already swiped on target user");
        }

        // Create and save swipe
        Swipe swipe = Swipe.builder()
            .userId(userId)
            .targetUserId(request.getTargetUserId())
            .swipeType(request.getSwipeType())
            .build();

        swipe = swipeRepository.save(swipe);
        log.debug("Swipe saved with ID: {}", swipe.getId());

        // Check for mutual match (only if current swipe is LIKE)
        boolean isMatch = false;
        Long matchId = null;

        if (request.getSwipeType() == SwipeType.LIKE) {
            Optional<Match> existingMatch = checkAndCreateMatch(userId, request.getTargetUserId());
            if (existingMatch.isPresent()) {
                isMatch = true;
                matchId = existingMatch.get().getId();
                log.info("Match created! User {} and User {}", userId, request.getTargetUserId());
            }
        }

        // Build response
        return SwipeResponse.builder()
            .swipeId(swipe.getId())
            .userId(userId)
            .targetUserId(request.getTargetUserId())
            .swipeType(request.getSwipeType())
            .timestamp(swipe.getTimestamp())
            .isMatch(isMatch)
            .matchId(matchId)
            .build();
    }

    /**
     * Check if target user liked current user, and create match if so
     *
     * @param userId Current user ID
     * @param targetUserId Target user ID
     * @return Optional of Match if mutual like exists
     */
    private Optional<Match> checkAndCreateMatch(Long userId, Long targetUserId) {
        // Check if target user previously liked current user
        Optional<Swipe> reverseSwipe = swipeRepository.findByUserIdAndTargetUserIdAndSwipeType(
            targetUserId, userId, SwipeType.LIKE
        );

        if (reverseSwipe.isPresent()) {
            // Mutual like detected - check if match already exists
            if (!matchRepository.existsActiveMatch(userId, targetUserId)) {
                // Create new match
                Match match = Match.createMatch(userId, targetUserId);
                match = matchRepository.save(match);
                log.info("Match created: ID={}, User1={}, User2={}",
                    match.getId(), match.getUser1Id(), match.getUser2Id());
                return Optional.of(match);
            }
        }

        return Optional.empty();
    }

    /**
     * Validate swipe request
     *
     * @param userId User performing swipe
     * @param request Swipe request
     */
    private void validateSwipe(Long userId, SwipeRequest request) {
        // Cannot swipe on yourself
        if (userId.equals(request.getTargetUserId())) {
            throw new IllegalArgumentException("Cannot swipe on yourself");
        }

        // Check if target user exists and is active
        try {
            Boolean exists = userServiceClient.userExists(request.getTargetUserId());
            if (!Boolean.TRUE.equals(exists)) {
                throw new IllegalArgumentException("Target user does not exist or is inactive");
            }
        } catch (Exception e) {
            log.error("Error validating target user: {}", e.getMessage());
            throw new IllegalStateException("Unable to validate target user", e);
        }
    }

    /**
     * Get swipe between users (if exists)
     *
     * @param userId User ID
     * @param targetUserId Target user ID
     * @return Optional of Swipe
     */
    public Optional<Swipe> getSwipe(Long userId, Long targetUserId) {
        return swipeRepository.findByUserIdAndTargetUserId(userId, targetUserId);
    }

    /**
     * Check if user already swiped on target
     *
     * @param userId User ID
     * @param targetUserId Target user ID
     * @return true if swipe exists
     */
    public boolean hasUserSwipedOnTarget(Long userId, Long targetUserId) {
        return swipeRepository.existsByUserIdAndTargetUserId(userId, targetUserId);
    }
}
