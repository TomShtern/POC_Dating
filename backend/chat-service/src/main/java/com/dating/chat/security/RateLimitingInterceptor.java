package com.dating.chat.security;

import com.dating.chat.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * WebSocket Rate Limiting Interceptor
 *
 * Enforces message rate limits on all WebSocket messages.
 * Rejects messages that exceed the configured rate limit.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingInterceptor implements ChannelInterceptor {

    private final RateLimitService rateLimitService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command == null || command != StompCommand.SEND) {
            return message;
        }

        // Get authenticated user
        if (accessor.getUser() == null) {
            return message;
        }

        String userId = accessor.getUser().getName();
        String destination = extractDestination(accessor);

        // Check rate limit
        if (!rateLimitService.isAllowed(userId, destination)) {
            log.warn("Rate limit exceeded: userId={}, destination={}", userId, destination);
            // Return null to reject the message
            return null;
        }

        return message;
    }

    private String extractDestination(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return "unknown";
        }
        if (destination.startsWith("/app/")) {
            return destination.substring(5);
        }
        return destination;
    }
}
