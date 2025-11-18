package com.dating.chat.config;

import com.dating.chat.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration with RabbitMQ STOMP Broker Relay Support
 *
 * ARCHITECTURE:
 * - Development: Simple in-memory broker (single instance)
 * - Production: RabbitMQ STOMP broker relay (distributed, multi-instance)
 *
 * STOMP DESTINATIONS:
 * - /app/** - Messages FROM client TO server (@MessageMapping)
 * - /topic/** - Broadcast messages (one-to-many)
 * - /queue/** - User-specific messages (one-to-one)
 * - /user/** - User-specific queues (Spring converts to /queue/user-{userId})
 *
 * MESSAGE FLOW (Production with STOMP relay):
 * Client1 --ws--> [Chat Service 1] --STOMP--> [RabbitMQ:61613] <--STOMP-- [Chat Service 2] <--ws-- Client2
 *
 * KEY ADVANTAGES OF STOMP RELAY:
 * - Horizontal scaling: Multiple service instances share message distribution
 * - Message routing: RabbitMQ handles routing, not in-memory within service
 * - Persistent broker: Disconnected clients don't lose messages
 * - Load balancing: Messages distributed across connected instances
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    // --- ENDPOINT CONFIGURATION ---
    @Value("${app.websocket.allowed-origins:http://localhost:8090,http://localhost:3000}")
    private String allowedOrigins;

    // --- BROKER TYPE (simple or stomp) ---
    @Value("${app.websocket.broker-type:simple}")
    private String brokerType;

    // --- STOMP RELAY CONNECTION PARAMETERS ---
    @Value("${app.rabbitmq.stomp.host:localhost}")
    private String stompHost;

    @Value("${app.rabbitmq.stomp.port:61613}")
    private int stompPort;

    @Value("${app.rabbitmq.stomp.client-login:guest}")
    private String stompClientLogin;

    @Value("${app.rabbitmq.stomp.client-passcode:guest}")
    private String stompClientPasscode;

    @Value("${app.rabbitmq.stomp.system-login:}")
    private String stompSystemLogin;

    @Value("${app.rabbitmq.stomp.system-passcode:}")
    private String stompSystemPasscode;

    // --- CONNECTION POOL & HEARTBEAT ---
    @Value("${app.rabbitmq.stomp.connection-pool-size:5}")
    private int connectionPoolSize;

    @Value("${app.rabbitmq.stomp.heartbeat-interval:60000}")
    private long heartbeatInterval;

    @Value("${app.rabbitmq.stomp.heartbeat-tolerance:180000}")
    private long heartbeatTolerance;

    @Value("${app.rabbitmq.stomp.reconnect-delay:5000}")
    private long reconnectDelay;

    /**
     * Register STOMP endpoints that clients connect to.
     *
     * Two endpoints:
     * 1. /ws with SockJS - Fallback to HTTP if WebSocket unavailable
     * 2. /ws without SockJS - Pure WebSocket for native clients
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = allowedOrigins.split(",");

        // SockJS endpoint with fallback support
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins)
                .withSockJS()
                .setDisconnectDelay(10000)        // Wait before closing connection
                .setHeartbeatTime(25000)          // Client-side heartbeat every 25s
                .setHttpMessageCacheSize(1000)
                .setSessionCookieNeeded(true);

        // Pure WebSocket endpoint (no fallback)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins);

        log.info("WebSocket endpoints registered for origins: {}", String.join(",", origins));
    }

    /**
     * Configure the message broker.
     *
     * DECISION LOGIC:
     * - "simple": In-memory broker (development, single instance)
     * - "stomp": RabbitMQ STOMP relay (production, multi-instance)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if ("stomp".equalsIgnoreCase(brokerType)) {
            configureStompBrokerRelay(registry);
        } else {
            configureSimpleBroker(registry);
        }

        // Destination prefix for application (server) destinations
        registry.setApplicationDestinationPrefixes("/app");

        // User destination prefix (for @SendToUser)
        registry.setUserDestinationPrefix("/user");

        // Expire user registrations after this time
        registry.setTimeToLiveForUserDestinations(3600000); // 1 hour
    }

    /**
     * PRODUCTION: RabbitMQ STOMP Broker Relay Configuration
     *
     * Enables horizontal scaling by routing all messages through RabbitMQ STOMP broker.
     *
     * HOW IT WORKS:
     * 1. Service connects to RabbitMQ STOMP port (61613) as a STOMP client
     * 2. All /topic and /queue destinations are handled by RabbitMQ
     * 3. Messages from any connected client route through RabbitMQ
     * 4. Multiple service instances see same message distribution
     *
     * CONNECTION FLOW:
     * WebSocket Client ← → Chat Service ← → RabbitMQ STOMP Broker (61613)
     *                      ↑                        ↑
     *                      └────────────────────────┘
     *                       (STOMP protocol, TCP)
     */
    private void configureStompBrokerRelay(MessageBrokerRegistry registry) {
        log.info("Configuring RabbitMQ STOMP Broker Relay: {}:{}", stompHost, stompPort);

        StompBrokerRelayMessageHandler relay = registry.enableStompBrokerRelay("/topic", "/queue")
                // --- CONNECTION PARAMETERS ---
                .setRelayHost(stompHost)
                .setRelayPort(stompPort)
                .setClientLogin(stompClientLogin)
                .setClientPasscode(stompClientPasscode)
                .setSystemLogin(stompSystemLogin)
                .setSystemPasscode(stompSystemPasscode)
                
                // --- HEARTBEAT CONFIGURATION ---
                // These values are negotiated in STOMP CONNECT frame
                .setSystemHeartbeatSendInterval(heartbeatInterval)      // Send heartbeat every 60s
                .setSystemHeartbeatReceiveInterval(heartbeatInterval)   // Expect heartbeat every 60s
                
                // --- VIRTUAL HOST ---
                // RabbitMQ vhost to use (default is /)
                .setVirtualHost("/")
                
                // --- BUFFER CONFIGURATION ---
                // Buffer sizes for TCP connections
                .setBufferSize(65536)              // 64 KB send buffer per connection
                .setReceiveBufferSize(65536);      // 64 KB receive buffer per connection

        log.info("STOMP Relay Configuration:");
        log.info("  Host: {}", stompHost);
        log.info("  Port: {}", stompPort);
        log.info("  Connection Pool Size: {}", connectionPoolSize);
        log.info("  Heartbeat Interval: {} ms", heartbeatInterval);
        log.info("  Heartbeat Tolerance: {} ms", heartbeatTolerance);
        log.info("  Reconnect Delay: {} ms", reconnectDelay);
    }

    /**
     * DEVELOPMENT: Simple In-Memory Broker
     *
     * Lightweight broker for local development and single-instance deployments.
     *
     * LIMITATIONS:
     * - Messages only available to connected clients in THIS instance
     * - No persistence (messages lost on shutdown)
     * - Not suitable for multi-instance setups
     * - Perfect for rapid development and testing
     */
    private void configureSimpleBroker(MessageBrokerRegistry registry) {
        log.info("Configuring simple in-memory message broker (development mode)");

        registry.enableSimpleBroker("/topic", "/queue");
    }

    /**
     * Configure client inbound channel (messages FROM client TO server).
     *
     * FLOW: Client --STOMP--> Inbound Channel --[Auth]-> Handler --> Broker
     *
     * Thread Pool:
     * - corePoolSize: Initial thread count
     * - maxPoolSize: Maximum threads under load
     * - queueCapacity: Queue size for pending tasks
     * - keepAliveSeconds: Thread idle timeout
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration
                .interceptors(webSocketAuthInterceptor)
                .corePoolSize(8)                   // Start with 8 threads
                .maxPoolSize(32)                   // Max 32 threads
                .queueCapacity(1000)               // Queue size for tasks
                .keepAliveSeconds(60)              // Idle timeout
                .allowCoreThreadTimeOut(true);     // Allow core threads to timeout

        log.info("Client inbound channel configured: corePoolSize=8, maxPoolSize=32");
    }

    /**
     * Configure client outbound channel (messages TO client FROM server).
     *
     * FLOW: Broker --> Outbound Channel --> Client --STOMP-->
     *
     * Same thread pool configuration as inbound for symmetrical load handling.
     */
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration
                .corePoolSize(8)                   // Start with 8 threads
                .maxPoolSize(32)                   // Max 32 threads
                .queueCapacity(1000)               // Queue size for tasks
                .keepAliveSeconds(60)              // Idle timeout
                .allowCoreThreadTimeOut(true);     // Allow core threads to timeout

        log.info("Client outbound channel configured: corePoolSize=8, maxPoolSize=32");
    }
}
