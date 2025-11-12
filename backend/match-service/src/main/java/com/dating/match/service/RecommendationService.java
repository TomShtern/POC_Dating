package com.dating.match.service;

import com.dating.match.client.UserServiceClient;
import com.dating.match.dto.UserSummaryDTO;
import com.dating.match.repository.MatchRepository;
import com.dating.match.repository.SwipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for profile recommendations
 *
 * PURPOSE: Generate personalized feed of potential matches
 *
 * RESPONSIBILITIES:
 * - Get list of users to exclude (already swiped/matched)
 * - Fetch recommendations from user-service
 * - Return next profile for swiping
 * - Apply basic filtering logic
 *
 * RECOMMENDATION ALGORITHM (Basic POC):
 * 1. Exclude users already swiped on (any direction)
 * 2. Exclude users already matched with
 * 3. Exclude current user (cannot swipe on self)
 * 4. Fetch candidates from user-service
 * 5. User-service applies preference filters (age, location, interests)
 * 6. Return top N candidates
 *
 * WHY DELEGATE TO USER-SERVICE:
 * - User-service has access to user profiles and preferences
 * - Match-service only knows swipe/match history
 * - Separation of concerns (user data vs. matching logic)
 * - User-service can apply complex filtering without exposing data
 *
 * FUTURE ENHANCEMENTS:
 * - Machine learning recommendations
 * - Collaborative filtering (users similar to you liked...)
 * - Elo-based scoring
 * - Location-based ranking
 * - Trending/featured profiles
 *
 * CACHING:
 * - Consider caching recommendation feed in Redis
 * - TTL: 1 hour (balance freshness vs. performance)
 * - Invalidate on preference change
 * - Pre-generate for active users
 *
 * ALTERNATIVES:
 * - Generate all recommendations in match-service: Need full user data
 * - Store recommendations in database: Stale data issues
 * - Real-time calculation: Too slow for large user base
 *
 * RATIONALE:
 * - Hybrid approach: Match-service filters, user-service recommends
 * - Keeps services loosely coupled
 * - Allows independent scaling and optimization
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final SwipeRepository swipeRepository;
    private final MatchRepository matchRepository;
    private final UserServiceClient userServiceClient;

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 20;

    /**
     * Get next profile for user to swipe on
     *
     * @param userId User requesting next profile (from JWT)
     * @return Next user profile to display
     */
    @Transactional(readOnly = true)
    public UserSummaryDTO getNextProfile(Long userId) {
        log.info("Getting next profile for user: {}", userId);

        List<UserSummaryDTO> recommendations = getRecommendations(userId, 1);

        if (recommendations.isEmpty()) {
            log.info("No more profiles available for user: {}", userId);
            return null;
        }

        return recommendations.get(0);
    }

    /**
     * Get multiple profile recommendations
     *
     * @param userId User requesting recommendations (from JWT)
     * @param limit Max number of profiles to return
     * @return List of recommended user profiles
     */
    @Transactional(readOnly = true)
    public List<UserSummaryDTO> getRecommendations(Long userId, Integer limit) {
        log.info("Getting {} recommendations for user: {}", limit, userId);

        // Get users to exclude (already swiped or matched)
        List<Long> excludeUserIds = getExcludedUserIds(userId);
        log.debug("Excluding {} users from recommendations", excludeUserIds.size());

        // Fetch recommendations from user-service
        try {
            List<UserSummaryDTO> recommendations = userServiceClient.getRecommendations(
                userId,
                limit != null ? limit : DEFAULT_RECOMMENDATION_LIMIT,
                excludeUserIds
            );

            log.info("Returning {} recommendations for user: {}", recommendations.size(), userId);
            return recommendations;

        } catch (Exception e) {
            log.error("Error fetching recommendations from user-service: {}", e.getMessage());
            // Return empty list instead of throwing exception
            return new ArrayList<>();
        }
    }

    /**
     * Get list of user IDs to exclude from recommendations
     * Includes:
     * - Users already swiped on (like or pass)
     * - Users already matched with
     * - Current user (self)
     *
     * @param userId Current user ID
     * @return List of user IDs to exclude
     */
    private List<Long> getExcludedUserIds(Long userId) {
        // Get all swiped user IDs
        List<Long> swipedUserIds = swipeRepository.findSwipedUserIds(userId);

        // Get matched user IDs
        List<Long> matchedUserIds = matchRepository.findActiveMatchesByUserId(userId)
            .stream()
            .map(match -> match.getOtherUserId(userId))
            .collect(Collectors.toList());

        // Combine and deduplicate
        List<Long> excludedIds = new ArrayList<>(swipedUserIds);
        excludedIds.addAll(matchedUserIds);
        excludedIds.add(userId); // Exclude self

        return excludedIds.stream()
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Get users who liked current user (premium feature)
     * Shows profiles of users who swiped right on you
     *
     * @param userId Current user ID
     * @return List of users who liked current user
     */
    @Transactional(readOnly = true)
    public List<UserSummaryDTO> getUsersWhoLikedMe(Long userId) {
        log.info("Getting users who liked user: {}", userId);

        // Get user IDs who liked current user
        List<Long> likerIds = swipeRepository.findUsersWhoLiked(userId);

        if (likerIds.isEmpty()) {
            log.debug("No users have liked user: {}", userId);
            return new ArrayList<>();
        }

        // Fetch user profiles
        try {
            return userServiceClient.getUserSummaries(likerIds);
        } catch (Exception e) {
            log.error("Error fetching user profiles: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
