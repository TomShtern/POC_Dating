package com.dating.chat.listener;

import com.dating.chat.security.StompPrincipal;
import com.dating.chat.service.PresenceService;
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
 * Integrates with PresenceService for online/offline tracking.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;

    /**
     * Handle WebSocket connection event.
     */
    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = "unknown";
        String username = "unknown";
        String sessionId = accessor.getSessionId();

        if (accessor.getUser() instanceof StompPrincipal principal) {
            userId = principal.userId();
            username = principal.username();
        } else if (accessor.getUser() != null) {
            userId = accessor.getUser().getName();
        }

        log.info("WebSocket connected: userId={}, sessionId={}", userId, sessionId);

        // Track user as online in Redis
        boolean wasOffline = presenceService.setOnline(userId, sessionId, username);
        if (wasOffline) {
            log.info("User came online: userId={}", userId);
        }
    }

    /**
     * Handle WebSocket disconnection event.
     */
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = "unknown";
        String sessionId = accessor.getSessionId();

        if (accessor.getUser() instanceof StompPrincipal principal) {
            userId = principal.userId();
        } else if (accessor.getUser() != null) {
            userId = accessor.getUser().getName();
        }

        // Track user as offline in Redis
        boolean fullyOffline = presenceService.setOffline(userId, sessionId);

        if (fullyOffline) {
            log.info("User went offline: userId={}", userId);
        } else {
            log.debug("Session removed, user still online: userId={}, sessions={}",
                    userId, presenceService.getSessionCount(userId));
        }
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
