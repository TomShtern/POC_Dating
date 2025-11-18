package com.dating.chat.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

/**
 * WebSocket Authentication Interceptor
 *
 * Authenticates WebSocket connections using JWT token.
 *
 * HOW IT WORKS:
 * 1. Client connects with JWT in query param or header
 * 2. Interceptor validates JWT
 * 3. Sets Principal for the connection
 * 4. All subsequent messages have authenticated user
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        // Only authenticate on CONNECT command
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token == null) {
                log.warn("WebSocket connection attempt without token");
                throw new AuthenticationCredentialsNotFoundException("Missing authentication token");
            }

            try {
                // Validate JWT and extract user info
                Claims claims = tokenProvider.validateToken(token);
                String userId = claims.getSubject();
                String username = claims.get("username", String.class);

                // Set the authenticated user as Principal
                accessor.setUser(new StompPrincipal(userId, username));

                log.info("WebSocket authenticated: userId={}, username={}", userId, username);
            } catch (Exception e) {
                log.warn("WebSocket authentication failed: {}", e.getMessage());
                throw new AuthenticationCredentialsNotFoundException("Invalid token: " + e.getMessage());
            }
        }

        return message;
    }

    /**
     * Extract JWT token from STOMP headers.
     * Tries Authorization header first, then falls back to 'token' header.
     */
    private String extractToken(StompHeaderAccessor accessor) {
        // Try Authorization header first (standard)
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Fall back to 'token' header (for SockJS compatibility)
        return accessor.getFirstNativeHeader("token");
    }
}
