package com.dating.recommendation.config;

import com.dating.recommendation.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Cache warming component for Recommendation Service.
 * Pre-loads recommendations for active users on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmer implements ApplicationRunner {

    private final RecommendationRepository recommendationRepository;

    // Maximum number of users to warm cache for
    private static final int MAX_USERS_TO_WARM = 500;

    // Consider recommendations valid if created within last 24 hours
    private static final int RECENT_HOURS_THRESHOLD = 24;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting cache warming for Recommendation Service...");
        warmCaches();
    }

    @Async
    public void warmCaches() {
        try {
            Instant threshold = Instant.now().minus(RECENT_HOURS_THRESHOLD, ChronoUnit.HOURS);

            // Get users with recent recommendations
            Set<UUID> usersWithRecommendations = new HashSet<>();

            recommendationRepository.findDistinctUserIdsWithRecentRecommendations(
                    threshold, PageRequest.of(0, MAX_USERS_TO_WARM))
                    .forEach(usersWithRecommendations::add);

            log.info("Found {} users with recent recommendations to warm cache for",
                    usersWithRecommendations.size());

            // Warm recommendations cache
            int warmedRecommendations = 0;
            for (UUID userId : usersWithRecommendations) {
                try {
                    // Load recommendations into cache
                    recommendationRepository.findByUserIdAndExpiresAtAfterOrderByScoreDesc(
                            userId, Instant.now(), PageRequest.of(0, 50));
                    warmedRecommendations++;
                } catch (Exception e) {
                    log.debug("Failed to warm cache for user {}: {}", userId, e.getMessage());
                }
            }

            log.info("Cache warming completed. Warmed recommendations cache for {} users",
                    warmedRecommendations);

        } catch (Exception e) {
            log.error("Cache warming failed: {}", e.getMessage(), e);
        }
    }
}
