package com.dating.chat.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages WebSocket sessions for online users.
 * Tracks which users are currently connected.
 */
@Component
@Slf4j
public class WebSocketSessionManager {

    /**
     * Map of user ID to set of session IDs.
     * A user can have multiple sessions (multiple devices/tabs).
     */
    private final Map<UUID, Set<String>> userSessions = new ConcurrentHashMap<>();

    /**
     * Map of session ID to user ID.
     */
    private final Map<String, UUID> sessionUsers = new ConcurrentHashMap<>();

    /**
     * Register a WebSocket session for a user.
     *
     * @param userId User ID
     * @param sessionId WebSocket session ID
     */
    public void registerSession(UUID userId, String sessionId) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
        sessionUsers.put(sessionId, userId);

        log.info("Registered WebSocket session {} for user {}", sessionId, userId);
        log.debug("User {} now has {} active sessions", userId, userSessions.get(userId).size());
    }

    /**
     * Remove a WebSocket session.
     *
     * @param sessionId WebSocket session ID
     */
    public void removeSession(String sessionId) {
        UUID userId = sessionUsers.remove(sessionId);

        if (userId != null) {
            Set<String> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                    log.info("User {} is now offline (no active sessions)", userId);
                } else {
                    log.debug("Removed session {} for user {}, {} sessions remaining",
                            sessionId, userId, sessions.size());
                }
            }
        }
    }

    /**
     * Check if a user is online (has at least one active session).
     *
     * @param userId User ID
     * @return true if user is online
     */
    public boolean isUserOnline(UUID userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Get all session IDs for a user.
     *
     * @param userId User ID
     * @return Set of session IDs, or empty set if user not online
     */
    public Set<String> getUserSessions(UUID userId) {
        return userSessions.getOrDefault(userId, Set.of());
    }

    /**
     * Get user ID for a session.
     *
     * @param sessionId Session ID
     * @return User ID, or null if session not found
     */
    public UUID getUserId(String sessionId) {
        return sessionUsers.get(sessionId);
    }

    /**
     * Get number of online users.
     *
     * @return Count of online users
     */
    public int getOnlineUserCount() {
        return userSessions.size();
    }

    /**
     * Get total number of active sessions.
     *
     * @return Count of sessions
     */
    public int getActiveSessionCount() {
        return sessionUsers.size();
    }
}
