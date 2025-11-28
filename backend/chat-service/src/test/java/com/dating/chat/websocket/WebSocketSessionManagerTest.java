package com.dating.chat.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WebSocketSessionManager.
 */
class WebSocketSessionManagerTest {

    private WebSocketSessionManager sessionManager;
    private UUID userId;
    private String sessionId;

    @BeforeEach
    void setUp() {
        sessionManager = new WebSocketSessionManager();
        userId = UUID.randomUUID();
        sessionId = "session-" + UUID.randomUUID();
    }

    @Test
    void testRegisterSession_Success() {
        // Act
        sessionManager.registerSession(userId, sessionId);

        // Assert
        assertTrue(sessionManager.isUserOnline(userId));
        assertEquals(userId, sessionManager.getUserId(sessionId));
        assertEquals(1, sessionManager.getOnlineUserCount());
        assertEquals(1, sessionManager.getActiveSessionCount());
    }

    @Test
    void testRegisterMultipleSessions_SameUser() {
        // Arrange
        String sessionId2 = "session-" + UUID.randomUUID();

        // Act
        sessionManager.registerSession(userId, sessionId);
        sessionManager.registerSession(userId, sessionId2);

        // Assert
        assertTrue(sessionManager.isUserOnline(userId));
        Set<String> sessions = sessionManager.getUserSessions(userId);
        assertEquals(2, sessions.size());
        assertTrue(sessions.contains(sessionId));
        assertTrue(sessions.contains(sessionId2));
        assertEquals(1, sessionManager.getOnlineUserCount());
        assertEquals(2, sessionManager.getActiveSessionCount());
    }

    @Test
    void testRemoveSession_Success() {
        // Arrange
        sessionManager.registerSession(userId, sessionId);

        // Act
        sessionManager.removeSession(sessionId);

        // Assert
        assertFalse(sessionManager.isUserOnline(userId));
        assertNull(sessionManager.getUserId(sessionId));
        assertEquals(0, sessionManager.getOnlineUserCount());
        assertEquals(0, sessionManager.getActiveSessionCount());
    }

    @Test
    void testRemoveSession_PartialRemoval() {
        // Arrange
        String sessionId2 = "session-" + UUID.randomUUID();
        sessionManager.registerSession(userId, sessionId);
        sessionManager.registerSession(userId, sessionId2);

        // Act
        sessionManager.removeSession(sessionId);

        // Assert
        assertTrue(sessionManager.isUserOnline(userId));
        Set<String> sessions = sessionManager.getUserSessions(userId);
        assertEquals(1, sessions.size());
        assertTrue(sessions.contains(sessionId2));
    }

    @Test
    void testRemoveSession_NonExistent() {
        // Act - should not throw
        sessionManager.removeSession("non-existent");

        // Assert
        assertEquals(0, sessionManager.getOnlineUserCount());
    }

    @Test
    void testIsUserOnline_NotOnline() {
        // Assert
        assertFalse(sessionManager.isUserOnline(UUID.randomUUID()));
    }

    @Test
    void testGetUserSessions_NotRegistered() {
        // Act
        Set<String> sessions = sessionManager.getUserSessions(UUID.randomUUID());

        // Assert
        assertNotNull(sessions);
        assertTrue(sessions.isEmpty());
    }

    @Test
    void testGetUserId_NotRegistered() {
        // Assert
        assertNull(sessionManager.getUserId("non-existent"));
    }

    @Test
    void testMultipleUsers() {
        // Arrange
        UUID userId2 = UUID.randomUUID();
        String sessionId2 = "session-" + UUID.randomUUID();

        // Act
        sessionManager.registerSession(userId, sessionId);
        sessionManager.registerSession(userId2, sessionId2);

        // Assert
        assertEquals(2, sessionManager.getOnlineUserCount());
        assertEquals(2, sessionManager.getActiveSessionCount());
        assertTrue(sessionManager.isUserOnline(userId));
        assertTrue(sessionManager.isUserOnline(userId2));
    }
}
