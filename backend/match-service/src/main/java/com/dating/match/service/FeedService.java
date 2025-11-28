package com.dating.match.service;

import com.dating.match.client.RecommendationServiceClient;
import com.dating.match.client.UserServiceClient;
import com.dating.match.config.CacheConfig;
import com.dating.match.dto.response.FeedResponse;
import com.dating.match.repository.SwipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating the swiping feed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final SwipeRepository swipeRepository;
    private final UserServiceClient userServiceClient;
    private final RecommendationServiceClient recommendationServiceClient;

    /**
     * Get the swiping feed for a user.
     *
     * @param userId User ID
     * @param limit Number of users to return
     * @param offset Pagination offset
     * @return Feed response with users
     */
    @Cacheable(value = CacheConfig.FEED_CACHE, key = "#userId + '_' + #limit + '_' + #offset")
    @Transactional(readOnly = true)
    public FeedResponse getFeed(UUID userId, int limit, int offset) {
        log.debug("Getting feed for user {} with limit {} and offset {}", userId, limit, offset);

        // Get users already swiped on
        List<UUID> swipedUserIds = swipeRepository.findSwipedUserIdsByUserId(userId);
        Set<UUID> excludeSet = new HashSet<>(swipedUserIds);
        excludeSet.add(userId); // Exclude self

        // Get recommendations from recommendation service
        List<FeedResponse.FeedUserInfo> feedUsers = new ArrayList<>();
        long total = 0;
        boolean hasMore = false;

        try {
            var recommendations = recommendationServiceClient.getRecommendations(userId, limit + offset + 10);

            // Filter out already swiped users and map to feed format
            List<RecommendationServiceClient.RecommendedUser> filteredRecs = recommendations.recommendations()
                    .stream()
                    .filter(rec -> !excludeSet.contains(rec.id()))
                    .collect(Collectors.toList());

            total = filteredRecs.size();

            // Apply pagination
            int start = Math.min(offset, filteredRecs.size());
            int end = Math.min(start + limit, filteredRecs.size());

            for (int i = start; i < end; i++) {
                var rec = filteredRecs.get(i);
                FeedResponse.FeedUserInfo userInfo = getUserInfoForFeed(rec.id(), rec.score());
                if (userInfo != null) {
                    feedUsers.add(userInfo);
                }
            }

            hasMore = end < filteredRecs.size();

        } catch (Exception e) {
            log.warn("Failed to get recommendations, using fallback: {}", e.getMessage());
            // Fallback: return empty feed
            // In production, we might have a local fallback algorithm
        }

        return new FeedResponse(feedUsers, total, hasMore);
    }

    /**
     * Get user info for feed display.
     */
    private FeedResponse.FeedUserInfo getUserInfoForFeed(UUID userId, int compatibilityScore) {
        try {
            var user = userServiceClient.getUserById(userId);

            String name = (user.firstName() != null ? user.firstName() : "") +
                    (user.lastName() != null ? " " + user.lastName() : "");

            return new FeedResponse.FeedUserInfo(
                    user.id(),
                    name.trim().isEmpty() ? user.username() : name.trim(),
                    user.age(),
                    user.profilePictureUrl(),
                    user.bio(),
                    compatibilityScore
            );
        } catch (Exception e) {
            log.warn("Failed to get user info for {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Calculate age from date of birth.
     */
    private int calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return 0;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
