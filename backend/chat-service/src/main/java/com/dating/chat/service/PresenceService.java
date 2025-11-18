package com.dating.chat.service;

import com.dating.chat.dto.websocket.PresenceChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * Presence Service - Redis-based online/offline status tracking.
 *
 * Tracks user presence with support for multiple concurrent sessions per user.
 *
 * REDIS KEY PATTERNS:
 * - presence:online - SET of all online user IDs
 * - presence:user:{userId}:sessions - SET of session IDs for a user
 * - presence:lastSeen:{userId} - STRING with last activity timestamp
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.chat.presence.ttl-seconds:1800}")
    private long presenceTtlSeconds;

    private static final String ONLINE_USERS_KEY = "presence:online";
    private static final String USER_SESSIONS_PREFIX = "presence:user:";
    private static final String LAST_SEEN_PREFIX = "presence:lastSeen:";
    private static final String SESSIONS_SUFFIX = ":sessions";

    /**
     * Mark a user as online when they connect.
     *
     * @return true if this was the user's first session (was offline before)
     */
    public boolean setOnline(String userId, String sessionId, String username) {
        try {
            boolean wasOnline = hasActiveSessions(userId);

            // Add session to user's session set
            String userSessionsKey = USER_SESSIONS_PREFIX + userId + SESSIONS_SUFFIX;
            redisTemplate.opsForSet().add(userSessionsKey, sessionId);
            redisTemplate.expire(userSessionsKey, Duration.ofSeconds(presenceTtlSeconds));

            // Add to global online set
            if (!wasOnline) {
                redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId);
            }

            // Update last seen
            String lastSeenKey = LAST_SEEN_PREFIX + userId;
            redisTemplate.opsForValue().set(lastSeenKey, Instant.now().toString(),
                    Duration.ofSeconds(presenceTtlSeconds));

            log.info("User online: userId={}, sessionId={}, wasAlreadyOnline={}", userId, sessionId, wasOnline);

            if (!wasOnline) {
                publishPresenceChange(userId, username, true);
            }

            return !wasOnline;
        } catch (Exception e) {
            log.error("Error setting user online: userId={}", userId, e);
            return false;
        }
    }

    /**
     * Mark a user as offline when they disconnect.
     *
     * @return true if this was the user's last session (now fully offline)
     */
    public boolean setOffline(String userId, String sessionId) {
        try {
            // Remove session
            String userSessionsKey = USER_SESSIONS_PREFIX + userId + SESSIONS_SUFFIX;
            redisTemplate.opsForSet().remove(userSessionsKey, sessionId);

            // Check if user still has sessions
            boolean hasMoreSessions = hasActiveSessions(userId);

            if (!hasMoreSessions) {
                redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId);
                log.info("User offline: userId={}", userId);
                publishPresenceChange(userId, "User", false);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("Error setting user offline: userId={}", userId, e);
            return false;
        }
    }

    /**
     * Check if a user is online.
     */
    public boolean isOnline(String userId) {
        try {
            Boolean isMember = redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId);
            return isMember != null && isMember;
        } catch (Exception e) {
            log.error("Error checking online status: userId={}", userId, e);
            return false;
        }
    }

    /**
     * Check if user has any active sessions.
     */
    public boolean hasActiveSessions(String userId) {
        try {
            String userSessionsKey = USER_SESSIONS_PREFIX + userId + SESSIONS_SUFFIX;
            Long count = redisTemplate.opsForSet().size(userSessionsKey);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get session count for a user.
     */
    public long getSessionCount(String userId) {
        try {
            String userSessionsKey = USER_SESSIONS_PREFIX + userId + SESSIONS_SUFFIX;
            Long count = redisTemplate.opsForSet().size(userSessionsKey);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get all online users.
     */
    public Set<String> getOnlineUsers() {
        try {
            Set<String> users = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
            return users != null ? users : Collections.emptySet();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /**
     * Get last seen timestamp for a user.
     */
    public Optional<Instant> getLastSeen(String userId) {
        try {
            String lastSeenKey = LAST_SEEN_PREFIX + userId;
            String value = redisTemplate.opsForValue().get(lastSeenKey);
            if (value != null) {
                return Optional.of(Instant.parse(value));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void publishPresenceChange(String userId, String username, boolean isOnline) {
        try {
            PresenceChangeEvent event = new PresenceChangeEvent(userId, username, isOnline, Instant.now());
            messagingTemplate.convertAndSend("/topic/presence", event);
            log.info("Presence change published: userId={}, isOnline={}", userId, isOnline);
        } catch (Exception e) {
            log.error("Error publishing presence change: userId={}", userId, e);
        }
    }
}
