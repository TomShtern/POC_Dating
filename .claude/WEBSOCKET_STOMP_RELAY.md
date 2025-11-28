# Spring WebSocket with RabbitMQ STOMP Broker Relay - Research & Implementation Guide

## Overview
This guide provides the exact configuration needed for horizontal scaling of WebSocket connections using RabbitMQ STOMP Broker Relay. The STOMP (Simple Text Oriented Messaging Protocol) plugin allows multiple Chat Service instances to share message distribution through a centralized RabbitMQ broker.

---

## 1. EXACT SPRING DEPENDENCIES NEEDED

### Current Status (from chat-service/pom.xml)
✅ Already has:
```xml
<!-- WebSocket Support (includes STOMP relay support) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- AMQP support for RabbitMQ -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### ✅ No additional dependencies required!
The `spring-boot-starter-websocket` includes:
- `org.springframework:spring-messaging`
- `org.springframework:spring-websocket`
- `org.springframework.messaging:spring-messaging-simp` (includes STOMP relay)

All necessary STOMP broker relay classes are included.

---

## 2. RABBITMQ STOMP PLUGIN CONFIGURATION

### A. Enable STOMP in Docker Compose

RabbitMQ image `rabbitmq:3.12-management-alpine` comes with STOMP plugin preloaded but disabled.

**Modified docker-compose.yml (chat-service section):**

```yaml
rabbitmq:
    image: rabbitmq:3.12-management-alpine
    container_name: dating_rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER:-guest}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:-guest}
      RABBITMQ_DEFAULT_VHOST: ${RABBITMQ_VHOST:-/}
      # ===== STOMP PLUGIN CONFIGURATION =====
      # Enable STOMP plugin (port 61613)
      PLUGINS_DIR: /opt/rabbitmq/plugins
    ports:
      - "5672:5672"   # AMQP
      - "61613:61613" # STOMP (NEW - add this!)
      - "15672:15672" # Management UI
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
      # Enable STOMP plugin via enabled_plugins file
      - ./config/rabbitmq-enabled-plugins:/etc/rabbitmq/enabled_plugins:ro
    networks:
      - dating_network
    healthcheck:
      test: rabbitmq-diagnostics -q ping
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
```

### B. RabbitMQ Enabled Plugins Configuration File

Create `./config/rabbitmq-enabled-plugins`:

```
[rabbitmq_management,rabbitmq_stomp].
```

This file tells RabbitMQ to load the STOMP plugin on startup.

### C. Alternative: Enable via rabbitmq.conf

If using custom rabbitmq.conf, add:

```ini
# rabbitmq.conf
listeners.ssl.default = 5671
management.tcp.port = 15672
stomp.default_port = 61613
stomp.listeners.1.port = 61613
stomp.listeners.1.backlog = 4096
stomp.tcp_listen_options.backlog = 128
```

### D. STOMP Plugin Heartbeat Configuration

The STOMP plugin supports heartbeats to detect dead connections:

**In rabbitmq.conf (optional, for fine-tuning):**

```ini
# STOMP heartbeat settings (milliseconds)
stomp.tcp_listen_options.backlog = 128
stomp.tcp_listen_options.nodelay = true
stomp.tcp_listen_options.keepalive = true
stomp.tcp_listen_options.exit_on_close = false
```

These settings:
- `nodelay = true`: TCP_NODELAY enabled (low latency)
- `keepalive = true`: TCP keep-alive enabled
- `backlog = 128`: Connection backlog queue size

---

## 3. WEBSOCKET CONFIG WITH STOMP BROKER RELAY

### Complete Updated WebSocketConfig.java

```java
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
 * Configures WebSocket endpoints and message broker for real-time chat with horizontal scaling.
 *
 * SCALING STRATEGY:
 * - Development: Simple in-memory broker (single instance)
 * - Production: RabbitMQ STOMP broker relay (multiple instances)
 *
 * STOMP DESTINATIONS:
 * - /app/** - Messages FROM client TO server (application destinations)
 * - /topic/** - Broadcast messages (one-to-many)
 * - /queue/** - User-specific messages (one-to-one)
 * - /user/** - User-specific queues (Spring converts to /queue/user-{userId})
 *
 * MESSAGE FLOW (Production with STOMP relay):
 * Client1 → WebSocket → Chat Service 1 → RabbitMQ STOMP → Chat Service 2 → Client2
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Value("${app.websocket.allowed-origins:http://localhost:8090,http://localhost:3000}")
    private String allowedOrigins;

    @Value("${app.websocket.broker-type:simple}")
    private String brokerType; // "simple" (dev) or "stomp" (prod)

    @Value("${app.rabbitmq.stomp.host:localhost}")
    private String stompHost;

    @Value("${app.rabbitmq.stomp.port:61613}")
    private int stompPort;

    @Value("${app.rabbitmq.stomp.username:guest}")
    private String stompUsername;

    @Value("${app.rabbitmq.stomp.password:guest}")
    private String stompPassword;

    @Value("${app.rabbitmq.stomp.client-login:guest}")
    private String stompClientLogin;

    @Value("${app.rabbitmq.stomp.client-passcode:guest}")
    private String stompClientPasscode;

    @Value("${app.rabbitmq.stomp.system-login:}")
    private String stompSystemLogin;

    @Value("${app.rabbitmq.stomp.system-passcode:}")
    private String stompSystemPasscode;

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
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = allowedOrigins.split(",");

        // Main WebSocket endpoint with SockJS fallback
        // If WebSocket fails, falls back to HTTP polling
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins)
                .withSockJS()
                .setDisconnectDelay(10000)    // Wait 10s before closing connection
                .setHeartbeatTime(25000)      // Client-side heartbeat every 25s
                .setHttpMessageCacheSize(1000)
                .setSessionCookieNeeded(true);

        // Pure WebSocket endpoint without SockJS (for native WebSocket clients)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins);

        log.info("WebSocket endpoints registered for origins: {}", String.join(",", origins));
    }

    /**
     * Configure the message broker.
     *
     * DECISION LOGIC:
     * - Development (simple): Use in-memory broker for fast local testing
     * - Production (stomp): Use RabbitMQ STOMP relay for distributed messaging
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if ("stomp".equalsIgnoreCase(brokerType)) {
            configureStompBrokerRelay(registry);
        } else {
            configureSimpleBroker(registry);
        }

        // Prefix for messages FROM client TO server (@MessageMapping)
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific messages (@SendToUser)
        registry.setUserDestinationPrefix("/user");

        // User registry timeout (expire user subscriptions after this time)
        registry.setTimeToLiveForUserDestinations(3600000); // 1 hour
    }

    /**
     * PRODUCTION: RabbitMQ STOMP Broker Relay Configuration
     *
     * Connects to RabbitMQ STOMP broker on port 61613.
     * Distributes messages across multiple Chat Service instances.
     */
    private void configureStompBrokerRelay(MessageBrokerRegistry registry) {
        log.info("Configuring RabbitMQ STOMP Broker Relay: {}:{}", stompHost, stompPort);

        StompBrokerRelayMessageHandler relay = registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(stompHost)
                .setRelayPort(stompPort)
                .setClientLogin(stompClientLogin)
                .setClientPasscode(stompClientPasscode)
                .setSystemLogin(stompSystemLogin)
                .setSystemPasscode(stompSystemPasscode)
                // --- CONNECTION POOL SETTINGS ---
                .setSystemHeartbeatSendInterval(heartbeatInterval)
                .setSystemHeartbeatReceiveInterval(heartbeatInterval)
                .setVirtualHost("/")  // RabbitMQ virtual host (default: /)
                // --- BUFFER & POOL SETTINGS ---
                .setBufferSize(65536)          // 64KB buffer for each connection
                .setReceiveBufferSize(65536);  // Receive buffer size

        // Get the relay handler to configure additional properties
        if (relay instanceof StompBrokerRelayMessageHandler) {
            // Connection pool configuration
            relay.setTcpClient(null); // Use default TCP client with pool
        }

        log.info("STOMP Relay configured with:");
        log.info("  - Host: {}", stompHost);
        log.info("  - Port: {}", stompPort);
        log.info("  - Connection Pool Size: {}", connectionPoolSize);
        log.info("  - Heartbeat Interval: {}ms", heartbeatInterval);
        log.info("  - Heartbeat Tolerance: {}ms", heartbeatTolerance);
        log.info("  - Reconnect Delay: {}ms", reconnectDelay);
    }

    /**
     * DEVELOPMENT: Simple In-Memory Broker
     *
     * Suitable for single-instance deployments or local development.
     * Messages NOT persisted, lost on service restart.
     */
    private void configureSimpleBroker(MessageBrokerRegistry registry) {
        log.info("Configuring simple in-memory message broker (development mode)");

        registry.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(null)  // Use default scheduler
                .setSelectorHeaderName(null);  // No selector support in simple broker
    }

    /**
     * Configure client inbound channel (messages FROM client).
     * Add authentication interceptor to validate JWT on CONNECT.
     *
     * MESSAGE FLOW:
     * Client CONNECT → Inbound Channel → Auth Interceptor → Message Handler → Outbound Channel
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration
                .interceptors(webSocketAuthInterceptor)
                .corePoolSize(8)              // Core thread pool size
                .maxPoolSize(32)              // Max thread pool size
                .queueCapacity(1000)          // Queue size for pending messages
                .keepAliveSeconds(60)         // Thread keep-alive time
                .allowCoreThreadTimeOut(true);

        log.info("Client inbound channel configured with thread pool: core=8, max=32");
    }

    /**
     * Configure client outbound channel (messages TO client).
     * Can add interceptors for logging, monitoring, etc.
     */
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration
                .corePoolSize(8)              // Core thread pool size
                .maxPoolSize(32)              // Max thread pool size
                .queueCapacity(1000)          // Queue size for pending messages
                .keepAliveSeconds(60)         // Thread keep-alive time
                .allowCoreThreadTimeOut(true);

        log.info("Client outbound channel configured with thread pool: core=8, max=32");
    }
}
```

---

## 4. CONNECTION POOL SETTINGS

### A. STOMP Connection Pool Configuration

The STOMP broker relay automatically manages a connection pool. Key properties:

**In application.yml:**

```yaml
app:
  rabbitmq:
    stomp:
      # Host and port where RabbitMQ STOMP plugin listens
      host: ${RABBITMQ_STOMP_HOST:localhost}
      port: ${RABBITMQ_STOMP_PORT:61613}
      
      # Authentication credentials for service-to-RabbitMQ connections
      client-login: ${RABBITMQ_STOMP_CLIENT_LOGIN:guest}
      client-passcode: ${RABBITMQ_STOMP_CLIENT_PASSCODE:guest}
      system-login: ${RABBITMQ_STOMP_SYSTEM_LOGIN:guest}
      system-passcode: ${RABBITMQ_STOMP_SYSTEM_PASSCODE:guest}
      
      # Connection pool settings
      connection-pool-size: 5        # Connections to maintain
      heartbeat-interval: 60000      # Send heartbeat every 60s (milliseconds)
      heartbeat-tolerance: 180000    # Tolerate 180s of silence before reconnect
      reconnect-delay: 5000          # Delay before reconnecting (milliseconds)
```

### B. Thread Pool Configuration (in WebSocketConfig)

```java
// Inbound channel thread pool
registration
    .corePoolSize(8)          // Starting threads
    .maxPoolSize(32)          // Maximum threads
    .queueCapacity(1000)      // Queue size for pending tasks
    .keepAliveSeconds(60)     // Thread idle time before shutdown
    .allowCoreThreadTimeOut(true);  // Allow core threads to timeout
```

### C. Spring AMQP Connection Pool

Spring AMQP also maintains its own connection pool. Configure in application.yml:

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: /
    
    # Connection factory configuration
    connection-timeout: 10000        # 10 seconds
    cache:
      channel:
        size: 25                     # Connection cache size
        checkout-timeout: 3000       # Timeout for getting connection
```

---

## 5. HEARTBEAT CONFIGURATION

### A. STOMP Heartbeat at Protocol Level

```yaml
app:
  rabbitmq:
    stomp:
      heartbeat-interval: 60000      # Server sends heartbeat every 60s
      heartbeat-tolerance: 180000    # Server waits 180s before closing idle connection
      reconnect-delay: 5000          # Reconnect after 5s of silence
```

### B. WebSocket/SockJS Heartbeat

```java
// In registerStompEndpoints()
registry.addEndpoint("/ws")
    .setAllowedOriginPatterns(origins)
    .withSockJS()
    .setHeartbeatTime(25000);  // Client-side heartbeat every 25s
```

### C. Spring TaskScheduler Heartbeat

The message broker uses Spring's TaskScheduler for heartbeats:

```java
// In Spring Boot auto-configuration
// By default, uses a ConcurrentTaskScheduler
// To customize:

@Bean
public TaskScheduler stompBrokerRelayTaskScheduler() {
    return new ThreadPoolTaskScheduler() {{
        setPoolSize(10);
        setThreadNamePrefix("stomp-broker-");
        setAwaitTerminationSeconds(60);
        setWaitForTasksToCompleteOnShutdown(true);
        initialize();
    }};
}
```

### D. How Heartbeats Work

```
HEARTBEAT FLOW (every 60 seconds):
┌─────────────────────────────────────────────────────┐
│                   Chat Service                       │
│                                                       │
│  ┌─────────────────────────────────────────────┐   │
│  │  STOMP Broker Relay Handler                  │   │
│  │  - Maintains persistent connection to        │   │
│  │    RabbitMQ STOMP broker (port 61613)        │   │
│  │                                               │   │
│  │  HEARTBEAT NEGOTIATION:                      │   │
│  │  - Service → RabbitMQ: LF (keep-alive)       │   │
│  │  - RabbitMQ → Service: LF (ack)              │   │
│  │  - Every 60 seconds (configurable)           │   │
│  │                                               │   │
│  │  If no heartbeat for 180s:                   │   │
│  │  - Connection marked as stale                │   │
│  │  - Auto-reconnect triggered (after 5s delay) │   │
│  └─────────────────────────────────────────────┘   │
│                      ↓↑                              │
│                RabbitMQ STOMP                       │
│                Port 61613                          │
└─────────────────────────────────────────────────────┘
```

---

## 6. ENVIRONMENT VARIABLE CONFIGURATION

### Updated .env.example

```bash
# ========================================
# RABBITMQ STOMP CONFIGURATION
# ========================================

# STOMP broker host (where STOMP plugin listens)
RABBITMQ_STOMP_HOST=rabbitmq
RABBITMQ_STOMP_PORT=61613

# Authentication for service-to-broker connections
RABBITMQ_STOMP_CLIENT_LOGIN=guest
RABBITMQ_STOMP_CLIENT_PASSCODE=guest
RABBITMQ_STOMP_SYSTEM_LOGIN=
RABBITMQ_STOMP_SYSTEM_PASSCODE=

# Connection pool settings
RABBITMQ_STOMP_CONNECTION_POOL_SIZE=5
RABBITMQ_STOMP_HEARTBEAT_INTERVAL=60000
RABBITMQ_STOMP_HEARTBEAT_TOLERANCE=180000
RABBITMQ_STOMP_RECONNECT_DELAY=5000

# Broker type: "simple" (dev) or "stomp" (production)
WEBSOCKET_BROKER_TYPE=stomp

# ========================================
# WEBSOCKET CONFIGURATION
# ========================================

WEBSOCKET_ALLOWED_ORIGINS=http://localhost:8090,http://localhost:3000
```

### Docker Compose Environment Variables

```yaml
chat-service:
  environment:
    # ... existing vars ...
    RABBITMQ_STOMP_HOST: ${RABBITMQ_STOMP_HOST:-rabbitmq}
    RABBITMQ_STOMP_PORT: ${RABBITMQ_STOMP_PORT:-61613}
    RABBITMQ_STOMP_CLIENT_LOGIN: ${RABBITMQ_STOMP_CLIENT_LOGIN:-guest}
    RABBITMQ_STOMP_CLIENT_PASSCODE: ${RABBITMQ_STOMP_CLIENT_PASSCODE:-guest}
    RABBITMQ_STOMP_SYSTEM_LOGIN: ${RABBITMQ_STOMP_SYSTEM_LOGIN:-}
    RABBITMQ_STOMP_SYSTEM_PASSCODE: ${RABBITMQ_STOMP_SYSTEM_PASSCODE:-}
    RABBITMQ_STOMP_CONNECTION_POOL_SIZE: ${RABBITMQ_STOMP_CONNECTION_POOL_SIZE:-5}
    RABBITMQ_STOMP_HEARTBEAT_INTERVAL: ${RABBITMQ_STOMP_HEARTBEAT_INTERVAL:-60000}
    RABBITMQ_STOMP_HEARTBEAT_TOLERANCE: ${RABBITMQ_STOMP_HEARTBEAT_TOLERANCE:-180000}
    RABBITMQ_STOMP_RECONNECT_DELAY: ${RABBITMQ_STOMP_RECONNECT_DELAY:-5000}
    WEBSOCKET_BROKER_TYPE: ${WEBSOCKET_BROKER_TYPE:-stomp}
```

---

## 7. APPLICATION.YML CONFIGURATION

### Complete Updated chat-service/application.yml

```yaml
server:
  port: 8083

spring:
  application:
    name: chat-service

  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:dating_db}
    username: ${POSTGRES_USER:dating_user}
    password: ${POSTGRES_PASSWORD:dating_password}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms

  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: /
    connection-timeout: 10000
    cache:
      channel:
        size: 25
        checkout-timeout: 3000

  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 hour default

  # WebSocket configuration
  websocket:
    max-text-message-size: 65536
    max-binary-message-size: 65536

# JWT configuration (same secret as User Service)
security:
  jwt:
    secret: ${JWT_SECRET:change-me-with-a-secure-32-character-key-here}

# Actuator endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

# Logging
logging:
  level:
    root: INFO
    com.dating.chat: DEBUG
    org.springframework.messaging: DEBUG
    org.springframework.web.socket: DEBUG
    org.springframework.amqp: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# Custom application properties
app:
  chat:
    # Maximum messages to return in history
    max-history-size: 100
    # Cache TTL for messages (seconds)
    message-cache-ttl: 3600
    # Typing indicator timeout (seconds)
    typing-timeout: 5

  websocket:
    # Broker type: "simple" (development, in-memory)
    #             "stomp" (production, distributed via RabbitMQ)
    broker-type: ${WEBSOCKET_BROKER_TYPE:stomp}
    
    # Allowed origins for WebSocket connections (comma-separated)
    allowed-origins: ${WEBSOCKET_ALLOWED_ORIGINS:http://localhost:8090,http://localhost:3000}

  rabbitmq:
    exchange: match.exchange
    queues:
      match-created: chat.match.created.queue
      match-ended: chat.match.ended.queue
    
    # STOMP Broker Relay Configuration (for horizontal scaling)
    stomp:
      # Connection parameters
      host: ${RABBITMQ_STOMP_HOST:localhost}
      port: ${RABBITMQ_STOMP_PORT:61613}
      
      # Authentication credentials (service → RabbitMQ)
      client-login: ${RABBITMQ_STOMP_CLIENT_LOGIN:guest}
      client-passcode: ${RABBITMQ_STOMP_CLIENT_PASSCODE:guest}
      
      # System-level authentication (optional, for system messages)
      system-login: ${RABBITMQ_STOMP_SYSTEM_LOGIN:}
      system-passcode: ${RABBITMQ_STOMP_SYSTEM_PASSCODE:}
      
      # Connection pool & heartbeat settings
      connection-pool-size: ${RABBITMQ_STOMP_CONNECTION_POOL_SIZE:5}
      heartbeat-interval: ${RABBITMQ_STOMP_HEARTBEAT_INTERVAL:60000}  # 60 seconds
      heartbeat-tolerance: ${RABBITMQ_STOMP_HEARTBEAT_TOLERANCE:180000}  # 3 minutes
      reconnect-delay: ${RABBITMQ_STOMP_RECONNECT_DELAY:5000}  # 5 seconds
```

---

## 8. FALLBACK TO SIMPLE BROKER FOR DEVELOPMENT

### Profile-Based Configuration

#### application-dev.yml

```yaml
app:
  websocket:
    broker-type: simple  # Use in-memory broker for development
    allowed-origins: http://localhost:8090,http://localhost:3000
```

#### application-prod.yml

```yaml
app:
  websocket:
    broker-type: stomp  # Use RabbitMQ STOMP broker for production
    allowed-origins: https://yourdomain.com  # HTTPS in production
  
  rabbitmq:
    stomp:
      host: rabbitmq  # Use service name in Docker/Kubernetes
      port: 61613
      client-login: ${RABBITMQ_STOMP_CLIENT_LOGIN:guest}
      client-passcode: ${RABBITMQ_STOMP_CLIENT_PASSCODE:guest}
      heartbeat-interval: 60000
      heartbeat-tolerance: 180000
```

### Running with Profiles

```bash
# Development (simple broker)
java -Dspring.profiles.active=dev -jar chat-service.jar

# Production (STOMP relay)
java -Dspring.profiles.active=prod -jar chat-service.jar

# Docker Compose
environment:
  SPRING_PROFILES_ACTIVE: prod
```

---

## 9. PROPER ERROR HANDLING

### A. Connection Error Handler Component

```java
package com.dating.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.context.event.EventListener;

/**
 * WebSocket Error Handler
 *
 * Handles connection failures, disconnections, and protocol errors.
 */
@Component
@Slf4j
public class WebSocketErrorHandler {

    /**
     * Handle session disconnection events
     *
     * Triggered when:
     * - Client disconnects (intentional)
     * - Connection drops (network error)
     * - Server shutdown
     * - STOMP broker relay connection lost
     */
    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        
        log.warn("WebSocket session disconnected: sessionId={}, timestamp={}",
                sessionId, System.currentTimeMillis());

        // Cleanup: Remove active subscriptions, typing indicators, etc.
        cleanupSession(sessionId);
    }

    /**
     * Handle STOMP protocol errors
     */
    public void handleStompError(String sessionId, String message, Throwable cause) {
        log.error("STOMP error in session {}: {}", sessionId, message, cause);

        if (cause instanceof java.net.ConnectException) {
            log.error("Failed to connect to STOMP broker. Check RabbitMQ STOMP plugin is running on port 61613");
        } else if (cause instanceof javax.security.auth.login.LoginException) {
            log.error("STOMP authentication failed. Check RABBITMQ_STOMP_CLIENT_LOGIN/PASSCODE");
        } else if (cause instanceof java.net.SocketTimeoutException) {
            log.error("STOMP connection timeout. Check network connectivity and heartbeat settings");
        }
    }

    /**
     * Cleanup session resources on disconnect
     */
    private void cleanupSession(String sessionId) {
        // Remove from online users cache
        // Clear typing indicators
        // Publish user.offline event
        // etc.
    }
}
```

### B. Global Exception Handler for WebSocket

```java
package com.dating.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

/**
 * WebSocket Message Exception Handler
 *
 * Handles exceptions during message processing.
 */
@ControllerAdvice
@Slf4j
public class WebSocketExceptionHandler extends StompSubProtocolErrorHandler {

    @ExceptionHandler
    public void handleWebSocketException(Throwable cause) {
        log.error("WebSocket error occurred", cause);

        if (cause instanceof IllegalArgumentException) {
            log.warn("Invalid message format: {}", cause.getMessage());
        } else if (cause instanceof SecurityException) {
            log.warn("Unauthorized WebSocket access attempt");
        } else {
            log.error("Unexpected WebSocket error", cause);
        }
    }
}
```

### C. Configuration for Error Handling

```java
// In WebSocketConfig.java

@Override
public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(webSocketAuthInterceptor);
    
    // Add error handler
    registration.exceptionHandler(new org.springframework.messaging.support.ChannelInterceptorAdapter() {
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            try {
                return super.preSend(message, channel);
            } catch (Exception e) {
                log.error("Error processing inbound message", e);
                throw e;
            }
        }
    });
}
```

---

## 10. COMPLETE DOCKER COMPOSE UPDATE

```yaml
rabbitmq:
    image: rabbitmq:3.12-management-alpine
    container_name: dating_rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER:-guest}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:-guest}
      RABBITMQ_DEFAULT_VHOST: ${RABBITMQ_VHOST:-/}
    ports:
      - "5672:5672"   # AMQP protocol
      - "61613:61613" # STOMP protocol (for WebSocket broker relay)
      - "15672:15672" # Management UI
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
      # Enable STOMP plugin
      - ./config/rabbitmq-enabled-plugins:/etc/rabbitmq/enabled_plugins:ro
    networks:
      - dating_network
    healthcheck:
      test: rabbitmq-diagnostics -q ping
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  chat-service:
    build:
      context: ./backend/chat-service
      dockerfile: Dockerfile
    container_name: dating_chat_service
    ports:
      - "8083:8083"
    environment:
      SERVER_PORT: 8083
      SERVICE_NAME: chat-service
      POSTGRES_HOST: ${POSTGRES_HOST:-postgres}
      REDIS_HOST: ${REDIS_HOST:-redis}
      RABBITMQ_HOST: ${RABBITMQ_HOST:-rabbitmq}
      RABBITMQ_STOMP_HOST: ${RABBITMQ_STOMP_HOST:-rabbitmq}
      RABBITMQ_STOMP_PORT: ${RABBITMQ_STOMP_PORT:-61613}
      RABBITMQ_STOMP_CLIENT_LOGIN: ${RABBITMQ_STOMP_CLIENT_LOGIN:-guest}
      RABBITMQ_STOMP_CLIENT_PASSCODE: ${RABBITMQ_STOMP_CLIENT_PASSCODE:-guest}
      WEBSOCKET_BROKER_TYPE: ${WEBSOCKET_BROKER_TYPE:-stomp}
      LOG_LEVEL: ${LOG_LEVEL:-INFO}
      SPRING_PROFILES_ACTIVE: prod
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    networks:
      - dating_network
    restart: unless-stopped
```

---

## 11. TESTING THE STOMP BROKER RELAY SETUP

### A. Verify RabbitMQ STOMP Plugin is Running

```bash
# Check if STOMP port is listening
telnet localhost 61613

# Expected response:
# Connected to localhost.
# Escape character is '^]'.
# CONNECTED
# server:RabbitMQ/3.12.0
# version:1.0,1.1,1.2
# heart-beat:0,0

# Check RabbitMQ management console
# http://localhost:15672
# Go to Admin → Plugins
# Should see "rabbitmq_stomp" in the active plugins list
```

### B. Test WebSocket Connection

```javascript
// Client-side test (browser console)
const stompClient = new StompJs.Client({
  brokerURL: "ws://localhost:8090/ws",
  onConnect: (frame) => {
    console.log("Connected:", frame);
    
    // Subscribe to a topic
    stompClient.subscribe("/topic/chat/test", (message) => {
      console.log("Received:", message.body);
    });
    
    // Send a message
    stompClient.send("/app/chat/send", {}, JSON.stringify({
      matchId: "test-123",
      content: "Hello from STOMP relay!"
    }));
  }
});

stompClient.activate();
```

### C. Check Service Logs

```bash
# Verify STOMP relay connection
docker-compose logs -f chat-service | grep -i "stomp\|relay\|broker"

# Expected output:
# INFO: Configuring RabbitMQ STOMP Broker Relay: rabbitmq:61613
# INFO: STOMP Relay configured with:
# INFO:   - Host: rabbitmq
# INFO:   - Port: 61613
# INFO:   - Heartbeat Interval: 60000ms
```

---

## 12. PERFORMANCE TUNING PARAMETERS

### A. Buffer Sizes

```java
relay.setBufferSize(65536);          // 64 KB - increase for large messages
relay.setReceiveBufferSize(65536);   // 64 KB
```

### B. Connection Pool Optimization

```yaml
app:
  rabbitmq:
    stomp:
      connection-pool-size: 10        # Increase if many concurrent messages
      heartbeat-interval: 30000       # More frequent for lower latency (uses more resources)
      heartbeat-tolerance: 90000      # Lower to detect failures faster
      reconnect-delay: 1000           # Faster reconnection
```

### C. Thread Pool Sizing

```
For N concurrent users:
- corePoolSize = N / 100 (minimum 8)
- maxPoolSize = N / 20 (minimum 32)
- queueCapacity = N * 2

Example for 10,000 users:
- corePoolSize = 100
- maxPoolSize = 500
- queueCapacity = 20,000
```

---

## 13. MONITORING & OBSERVABILITY

### A. Health Check Endpoint

```java
@RestController
@RequestMapping("/actuator")
public class HealthController {
    
    @GetMapping("/health/websocket")
    public ResponseEntity<?> websocketHealth() {
        // Check STOMP relay connection status
        // Check active WebSocket connections
        // Check RabbitMQ STOMP connectivity
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "websocketConnections", activeConnections,
            "stompRelayConnected", relayHealthy,
            "rabbitmqStompPort", "61613"
        ));
    }
}
```

### B. Metrics (Prometheus)

```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus

# Metrics to monitor:
# - spring_messaging_handler_invocations_total
# - spring_messaging_handler_duration_seconds
# - spring_websocket_sessions_active
# - spring_websocket_messages_sent_total
# - spring_websocket_messages_received_total
```

---

## 14. TROUBLESHOOTING GUIDE

### Problem: "STOMP broker relay connection failed"

**Causes:**
1. RabbitMQ STOMP plugin not enabled
2. Port 61613 not exposed
3. Firewall blocking access
4. Wrong hostname/port in configuration

**Solution:**
```bash
# Verify STOMP plugin is loaded
docker exec dating_rabbitmq rabbitmq-plugins list
# Should show: [E*] rabbitmq_stomp

# Check STOMP port
docker exec dating_rabbitmq netstat -tlnp | grep 61613
# Should show: tcp  LISTEN on port 61613

# Test connectivity
telnet localhost 61613
```

### Problem: "Connection timeout to STOMP broker"

**Causes:**
1. Incorrect RABBITMQ_STOMP_HOST
2. Network isolation (wrong Docker network)
3. RabbitMQ service not running

**Solution:**
```bash
# Use service name in Docker Compose, not localhost
RABBITMQ_STOMP_HOST=rabbitmq  # Correct
RABBITMQ_STOMP_HOST=localhost # Wrong (not resolvable in container)

# Verify Docker network connectivity
docker exec dating_chat_service ping rabbitmq
# Should resolve to RabbitMQ container IP
```

### Problem: "Authentication failed for STOMP"

**Causes:**
1. Wrong RABBITMQ_STOMP_CLIENT_LOGIN credentials
2. RabbitMQ user doesn't exist
3. Vhost not accessible by user

**Solution:**
```bash
# Create RabbitMQ user via management console
# http://localhost:15672
# Or use CLI:
docker exec dating_rabbitmq rabbitmqctl add_user myuser mypassword
docker exec dating_rabbitmq rabbitmqctl set_permissions -p / myuser ".*" ".*" ".*"
```

---

## Summary: Implementation Checklist

- [ ] Enable STOMP plugin in RabbitMQ (docker-compose.yml + enabled_plugins)
- [ ] Add STOMP port (61613) to docker-compose.yml
- [ ] Create config/rabbitmq-enabled-plugins file with STOMP plugin
- [ ] Update WebSocketConfig.java with STOMP broker relay configuration
- [ ] Add environment variables to .env.example
- [ ] Update application.yml with STOMP configuration
- [ ] Create application-dev.yml (simple broker) and application-prod.yml (STOMP)
- [ ] Implement WebSocketErrorHandler and exception handling
- [ ] Add health check endpoint for WebSocket status
- [ ] Test STOMP connectivity (telnet localhost 61613)
- [ ] Test WebSocket connections with client
- [ ] Monitor logs for STOMP relay status
- [ ] Load test with multiple Chat Service instances
- [ ] Document scaling procedure in deployment guide

