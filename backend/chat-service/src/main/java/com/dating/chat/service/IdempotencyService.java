package com.dating.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Idempotency Service
 *
 * Prevents duplicate message processing using Redis-based idempotency keys.
 * Each message can have a client-generated idempotency key that ensures
 * the message is only processed once.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    /**
     * Check if an operation has already been processed.
     *
     * @param userId The user ID
     * @param idempotencyKey The client-provided idempotency key
     * @return true if this is a new operation (should be processed), false if duplicate
     */
    public boolean checkAndSet(String userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            return true; // No idempotency key provided, allow
        }

        // Validate key format to prevent injection and DoS
        if (idempotencyKey.length() > 64) {
            log.warn("Idempotency key too long: userId={}, length={}", userId, idempotencyKey.length());
            throw new IllegalArgumentException("Idempotency key exceeds maximum length of 64 characters");
        }

        // Only allow alphanumeric, dash, and underscore characters
        if (!idempotencyKey.matches("^[a-zA-Z0-9_-]+$")) {
            log.warn("Invalid idempotency key format: userId={}", userId);
            throw new IllegalArgumentException("Invalid idempotency key format");
        }

        String key = buildRedisKey(userId, idempotencyKey);

        try {
            Boolean wasSet = redisTemplate.opsForValue().setIfAbsent(key, "1", DEFAULT_TTL);
            boolean isNew = wasSet != null && wasSet;

            if (!isNew) {
                log.info("Duplicate operation detected: userId={}, key={}", userId, idempotencyKey);
            }

            return isNew;
        } catch (Exception e) {
            log.error("Error checking idempotency: userId={}, key={}", userId, idempotencyKey, e);
            return true; // Fail open
        }
    }

    /**
     * Generate a unique idempotency key.
     */
    public String generateKey() {
        return UUID.randomUUID().toString();
    }

    /**
     * Clear an idempotency key (for rollback scenarios).
     */
    public void clear(String userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            return;
        }

        String key = buildRedisKey(userId, idempotencyKey);
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Error clearing idempotency key: {}", key, e);
        }
    }

    private String buildRedisKey(String userId, String idempotencyKey) {
        return IDEMPOTENCY_KEY_PREFIX + userId + ":" + idempotencyKey;
    }
}
