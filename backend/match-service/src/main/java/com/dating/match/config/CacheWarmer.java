package com.dating.match.config;

import com.dating.match.repository.MatchRepository;
import com.dating.match.repository.SwipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Cache warming component for Match Service.
 * Pre-loads frequently accessed data on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmer implements ApplicationRunner {

    private final MatchRepository matchRepository;
    private final SwipeRepository swipeRepository;
    private final CacheManager cacheManager;

    // Maximum number of users to warm cache for
    private static final int MAX_USERS_TO_WARM = 1000;

    // Consider users active if they had activity in last 7 days
    private static final int ACTIVE_DAYS_THRESHOLD = 7;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting cache warming for Match Service...");
        warmCaches();
    }

    @Async
    public void warmCaches() {
        try {
            Instant threshold = Instant.now().minus(ACTIVE_DAYS_THRESHOLD, ChronoUnit.DAYS);

            // Get recently active users (users who swiped recently)
            Set<UUID> activeUserIds = new HashSet<>();

            swipeRepository.findDistinctUserIdsByCreatedAtAfter(threshold, PageRequest.of(0, MAX_USERS_TO_WARM))
                    .forEach(activeUserIds::add);

            log.info("Found {} recently active users to warm cache for", activeUserIds.size());

            // Warm matches cache for active users
            int warmedMatches = 0;
            for (UUID userId : activeUserIds) {
                try {
                    // Load matches into cache
                    matchRepository.findByUser1IdOrUser2IdOrderByMatchedAtDesc(userId, userId);
                    warmedMatches++;
                } catch (Exception e) {
                    log.debug("Failed to warm cache for user {}: {}", userId, e.getMessage());
                }
            }

            log.info("Cache warming completed. Warmed matches cache for {} users", warmedMatches);

        } catch (Exception e) {
            log.error("Cache warming failed: {}", e.getMessage(), e);
        }
    }
}
