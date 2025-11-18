package com.dating.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Rate Limiting Service
 *
 * Implements Redis-based sliding window rate limiting for WebSocket messages.
 * Prevents users from sending more than configured messages per second.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.ratelimit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.ratelimit.messages-per-second:10}")
    private int messagesPerSecond;

    @Value("${app.ratelimit.window-seconds:1}")
    private int windowSeconds;

    @Value("${app.ratelimit.burst-multiplier:1.5}")
    private double burstMultiplier;

    /**
     * Check if user is allowed to send a message.
     *
     * @param userId The user ID
     * @param destination The message destination (e.g., "chat.send")
     * @return true if allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String userId, String destination) {
        if (!rateLimitEnabled) {
            return true;
        }

        String key = buildRedisKey(userId, destination);
        long now = System.currentTimeMillis();
        long windowStartMs = now - (windowSeconds * 1000L);

        try {
            // Remove old entries outside the sliding window
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStartMs);

            // Count entries within the window
            Long count = redisTemplate.opsForZSet().zCard(key);
            int currentCount = count != null ? count.intValue() : 0;

            // Calculate allowed limit
            int allowedLimit = calculateLimit(destination);

            if (currentCount >= allowedLimit) {
                log.warn("Rate limit exceeded: userId={}, destination={}, count={}, limit={}",
                        userId, destination, currentCount, allowedLimit);
                return false;
            }

            // Add new entry
            String member = now + ":" + UUID.randomUUID();
            redisTemplate.opsForZSet().add(key, member, now);
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds + 1));

            return true;
        } catch (Exception e) {
            log.error("Error checking rate limit: userId={}, destination={}", userId, destination, e);
            return true; // Fail open
        }
    }

    /**
     * Get remaining quota for a user.
     */
    public int getRemainingQuota(String userId, String destination) {
        if (!rateLimitEnabled) {
            return Integer.MAX_VALUE;
        }

        String key = buildRedisKey(userId, destination);
        long now = System.currentTimeMillis();
        long windowStartMs = now - (windowSeconds * 1000L);

        try {
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStartMs);
            Long count = redisTemplate.opsForZSet().zCard(key);
            int currentCount = count != null ? count.intValue() : 0;
            int allowedLimit = calculateLimit(destination);

            return Math.max(0, allowedLimit - currentCount);
        } catch (Exception e) {
            log.error("Error getting remaining quota: {}", e.getMessage());
            return 0;
        }
    }

    private String buildRedisKey(String userId, String destination) {
        return String.format("rate-limit:%s:%s", userId, destination);
    }

    private int calculateLimit(String destination) {
        if ("chat.send".equals(destination)) {
            return (int) (messagesPerSecond * burstMultiplier);
        }
        if ("chat.typing".equals(destination)) {
            return (int) (messagesPerSecond * burstMultiplier * 2);
        }
        if ("chat.read".equals(destination)) {
            return (int) (messagesPerSecond * burstMultiplier * 5);
        }
        return (int) (messagesPerSecond * burstMultiplier);
    }
}
