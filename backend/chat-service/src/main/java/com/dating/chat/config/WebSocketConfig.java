package com.dating.chat.config;

import com.dating.chat.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration
 *
 * Configures WebSocket endpoints and message broker for real-time chat.
 *
 * STOMP DESTINATIONS:
 * - /app/** - Messages FROM client TO server (application destinations)
 * - /topic/** - Broadcast messages (one-to-many)
 * - /queue/** - User-specific messages (one-to-one)
 * - /user/** - User-specific queues (Spring converts to /queue/user-{userId})
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * Register STOMP endpoints that clients connect to.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Main WebSocket endpoint with SockJS fallback
        // If WebSocket fails, falls back to HTTP polling
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // TODO: Restrict in production
                .withSockJS();

        // Pure WebSocket endpoint without SockJS (for native clients)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    /**
     * Configure the message broker.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple in-memory broker for /topic and /queue destinations
        // For production scaling: Use RabbitMQ as external broker
        // registry.enableStompBrokerRelay("/topic", "/queue")
        //     .setRelayHost("localhost")
        //     .setRelayPort(61613);
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages FROM client TO server
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific messages
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Configure client inbound channel (messages FROM client).
     * Add authentication interceptor to validate JWT on CONNECT.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
