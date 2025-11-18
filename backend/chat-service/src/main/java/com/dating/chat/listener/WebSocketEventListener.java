package com.dating.chat.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/**
 * WebSocket Event Listener
 *
 * Handles WebSocket lifecycle events (connect, disconnect, subscribe).
 *
 * USE CASES:
 * - Track online users
 * - Clean up on disconnect
 * - Log for debugging
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle WebSocket connection event.
     */
    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getUser() != null ? accessor.getUser().getName() : "unknown";
        String sessionId = accessor.getSessionId();

        log.info("WebSocket connected: userId={}, sessionId={}", userId, sessionId);

        // Future: Track user as online in Redis
        // presenceService.setOnline(userId, sessionId);
    }

    /**
     * Handle WebSocket disconnection event.
     */
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getUser() != null ? accessor.getUser().getName() : "unknown";
        String sessionId = accessor.getSessionId();

        log.info("WebSocket disconnected: userId={}, sessionId={}", userId, sessionId);

        // Future: Track user as offline in Redis
        // presenceService.setOffline(userId, sessionId);
    }

    /**
     * Handle subscription event.
     */
    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getUser() != null ? accessor.getUser().getName() : "unknown";
        String destination = accessor.getDestination();

        log.debug("WebSocket subscribe: userId={}, destination={}", userId, destination);
    }

    /**
     * Handle unsubscription event.
     */
    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getUser() != null ? accessor.getUser().getName() : "unknown";
        String subscriptionId = accessor.getSubscriptionId();

        log.debug("WebSocket unsubscribe: userId={}, subscriptionId={}", userId, subscriptionId);
    }
}
