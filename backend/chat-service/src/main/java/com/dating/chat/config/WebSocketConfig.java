package com.dating.chat.config;

import com.dating.chat.security.RateLimitingInterceptor;
import com.dating.chat.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration
 *
 * Configures WebSocket endpoints and message broker for real-time chat.
 * Supports both simple broker (development) and RabbitMQ STOMP relay (production).
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
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final RateLimitingInterceptor rateLimitingInterceptor;

    @Value("${app.websocket.allowed-origins:http://localhost:8090,http://localhost:3000}")
    private String allowedOrigins;

    @Value("${app.websocket.broker-type:simple}")
    private String brokerType;

    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitHost;

    @Value("${app.rabbitmq.stomp-port:61613}")
    private int rabbitStompPort;

    @Value("${spring.rabbitmq.username:guest}")
    private String rabbitUser;

    @Value("${spring.rabbitmq.password:guest}")
    private String rabbitPassword;

    /**
     * Register STOMP endpoints that clients connect to.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        // Main WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins)
                .withSockJS();

        // Pure WebSocket endpoint without SockJS (for native clients)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins);
    }

    /**
     * Configure the message broker.
     * Supports both simple broker and RabbitMQ STOMP relay.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if ("stomp".equalsIgnoreCase(brokerType)) {
            configureStompBrokerRelay(registry);
        } else {
            configureSimpleBroker(registry);
        }

        // Prefix for messages FROM client TO server
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific messages
 * WebSocket configuration using STOMP protocol.
 * Enables real-time messaging between users.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure message broker for routing messages.
     * Uses simple in-memory broker for this POC.
     * Production would use RabbitMQ STOMP broker for scalability.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple broker for subscriptions
        // /queue for point-to-point, /topic for broadcast
        registry.enableSimpleBroker("/queue", "/topic");

        // Prefix for messages from clients to server
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific destinations
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Configure simple in-memory broker (development).
     */
    private void configureSimpleBroker(MessageBrokerRegistry registry) {
        log.info("Configuring simple in-memory message broker");
        registry.enableSimpleBroker("/topic", "/queue");
    }

    /**
     * Configure RabbitMQ STOMP broker relay (production).
     * Enables horizontal scaling across multiple service instances.
     */
    private void configureStompBrokerRelay(MessageBrokerRegistry registry) {
        log.info("Configuring RabbitMQ STOMP broker relay: {}:{}", rabbitHost, rabbitStompPort);
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(rabbitHost)
                .setRelayPort(rabbitStompPort)
                .setClientLogin(rabbitUser)
                .setClientPasscode(rabbitPassword)
                .setSystemLogin(rabbitUser)
                .setSystemPasscode(rabbitPassword)
                .setSystemHeartbeatSendInterval(60000)
                .setSystemHeartbeatReceiveInterval(60000);
    }

    /**
     * Configure client inbound channel (messages FROM client).
     * Add interceptors for authentication and rate limiting.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor, rateLimitingInterceptor);
     * Register STOMP endpoints that clients connect to.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
