package com.dating.user.config;

import com.dating.user.repository.UserRepository;
import com.dating.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Cache warming component for User Service.
 * Pre-loads frequently accessed user profiles on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    // Maximum number of users to warm cache for
    private static final int MAX_USERS_TO_WARM = 1000;

    // Consider users active if they logged in within last 7 days
    private static final int ACTIVE_DAYS_THRESHOLD = 7;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting cache warming for User Service...");
        warmCaches();
    }

    @Async
    public void warmCaches() {
        try {
            Instant threshold = Instant.now().minus(ACTIVE_DAYS_THRESHOLD, ChronoUnit.DAYS);

            // Get recently active users
            var activeUsers = userRepository.findByLastLoginAtAfterOrderByLastLoginAtDesc(
                    threshold, PageRequest.of(0, MAX_USERS_TO_WARM));

            log.info("Found {} recently active users to warm cache for", activeUsers.getContent().size());

            // Warm user profile cache
            int warmedProfiles = 0;
            for (var user : activeUsers.getContent()) {
                try {
                    // This will populate the cache via @Cacheable
                    userService.getUserById(user.getId());
                    warmedProfiles++;
                } catch (Exception e) {
                    log.debug("Failed to warm cache for user {}: {}", user.getId(), e.getMessage());
                }
            }

            log.info("Cache warming completed. Warmed profile cache for {} users", warmedProfiles);

        } catch (Exception e) {
            log.error("Cache warming failed: {}", e.getMessage(), e);
        }
    }
}
