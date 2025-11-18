package com.dating.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket Error Handler
 *
 * Handles connection failures, disconnections, and protocol errors.
 *
 * RESPONSIBILITIES:
 * - Detect WebSocket session disconnections
 * - Handle STOMP protocol errors
 * - Cleanup session resources (typing indicators, online status)
 * - Log and monitor connection health
 * - Trigger reconnection logic where needed
 *
 * EVENTS HANDLED:
 * - SessionDisconnectEvent: Client disconnect (intentional or network failure)
 * - STOMP CONNECT errors: Authentication or broker connection failures
 * - Heartbeat timeout: Dead connection detection
 */
@Component
@Slf4j
public class WebSocketErrorHandler {

    /**
     * Handle WebSocket session disconnection events.
     *
     * Called when:
     * - Client disconnects intentionally (close button)
     * - Network connection drops
     * - Server shutdown/restart
     * - STOMP broker relay connection lost
     * - Session timeout
     *
     * IMPORTANT: This is called for ANY WebSocket disconnect,
     * not just errors. Check event details for disconnect reason.
     */
    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        
        log.warn("WebSocket session disconnected: sessionId={}, timestamp={}",
                sessionId, System.currentTimeMillis());

        // Attempt to get disconnection reason
        if (event.getCloseStatus() != null) {
            log.warn("  Close code: {}, Reason: {}",
                    event.getCloseStatus().getCode(),
                    event.getCloseStatus().getReason());
        }

        // Cleanup session resources
        try {
            cleanupSession(sessionId);
        } catch (Exception e) {
            log.error("Error during session cleanup for sessionId={}", sessionId, e);
        }
    }

    /**
     * Handle STOMP protocol errors.
     *
     * Called when STOMP handshake or message processing fails.
     * Can be invoked manually from message handlers.
     *
     * COMMON ERRORS:
     * - ConnectException: Cannot reach STOMP broker
     * - LoginException: Authentication credentials invalid
     * - SocketTimeoutException: Broker not responding (heartbeat timeout)
     * - IOException: Network issues
     */
    public void handleStompError(String sessionId, String message, Throwable cause) {
        log.error("STOMP error in session {}: {}", sessionId, message);

        if (cause instanceof java.net.ConnectException) {
            log.error("Failed to connect to STOMP broker. Verify:");
            log.error("  - RabbitMQ STOMP plugin enabled: rabbitmq-plugins enable rabbitmq_stomp");
            log.error("  - STOMP port exposed: docker-compose.yml port 61613");
            log.error("  - RABBITMQ_STOMP_HOST configured correctly");
            log.error("  - Network connectivity between services");
        } else if (cause instanceof javax.security.auth.login.LoginException) {
            log.error("STOMP authentication failed. Verify:");
            log.error("  - RABBITMQ_STOMP_CLIENT_LOGIN/PASSCODE are correct");
            log.error("  - RabbitMQ user exists and has permissions");
        } else if (cause instanceof java.net.SocketTimeoutException) {
            log.error("STOMP connection timeout. Verify:");
            log.error("  - Network connectivity to RabbitMQ");
            log.error("  - RabbitMQ STOMP broker is running");
            log.error("  - Heartbeat interval not too aggressive");
        } else if (cause instanceof java.io.IOException) {
            log.error("STOMP I/O error: {}", cause.getMessage());
        } else {
            log.error("Unexpected STOMP error", cause);
        }
    }

    /**
     * Cleanup session resources on disconnect.
     *
     * CLEANUP TASKS:
     * - Remove from online users cache (Redis)
     * - Clear typing indicators
     * - Publish user.offline event
     * - Mark last_active timestamp
     * - Cancel any pending operations
     *
     * IMPLEMENTATION NOTE:
     * This is a placeholder. Actual implementation depends on:
     * - Redis cache structure
     * - RabbitMQ event publishing
     * - Database schema for user status
     *
     * TYPICAL FLOW:
     * sessionId --> userId --> Cache/DB cleanup --> Event publishing
     */
    private void cleanupSession(String sessionId) {
        log.debug("Cleaning up session: {}", sessionId);

        try {
            // TODO: Implement actual cleanup
            // 1. Get userId from sessionId mapping (Redis)
            // 2. Remove from online users set (Redis)
            // 3. Clear typing indicators (Redis)
            // 4. Update user.last_active (Database)
            // 5. Publish UserOfflineEvent (RabbitMQ)
            // 6. Cleanup subscription mappings

            log.debug("Session cleanup completed for: {}", sessionId);
        } catch (Exception e) {
            log.error("Cleanup failed for session: {}", sessionId, e);
            // Continue cleanup even if one step fails
        }
    }

    /**
     * Handle WebSocket connection errors during negotiation.
     *
     * Called during STOMP CONNECT frame processing.
     * If this throws an exception, the connection is rejected.
     */
    public void handleConnectionError(String sessionId, Throwable cause) {
        log.error("WebSocket connection error for sessionId={}: {}", sessionId, cause.getMessage());

        if (cause instanceof org.springframework.security.access.AccessDeniedException) {
            log.warn("Access denied for sessionId: {} - Invalid JWT or missing auth", sessionId);
        } else if (cause instanceof IllegalArgumentException) {
            log.warn("Invalid STOMP frame or headers: {}", cause.getMessage());
        } else {
            log.error("Unexpected connection error", cause);
        }
    }

    /**
     * Handle message processing errors.
     *
     * Called when @MessageMapping handler throws exception.
     */
    public void handleMessageError(String sessionId, String destination, Throwable cause) {
        log.error("Message processing error for sessionId={}, destination={}: {}",
                sessionId, destination, cause.getMessage());

        if (cause instanceof IllegalArgumentException) {
            log.warn("Invalid message format or validation failed: {}", cause.getMessage());
        } else if (cause instanceof org.springframework.security.access.AccessDeniedException) {
            log.warn("Access denied to destination: {}", destination);
        } else {
            log.error("Unexpected message error", cause);
        }
    }
}
