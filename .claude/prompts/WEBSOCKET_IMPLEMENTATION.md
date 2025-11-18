# WebSocket Real-Time Chat Implementation Prompt

## Context
You are implementing **real-time WebSocket communication** for the Chat Service in a POC Dating application. This enables instant messaging between matched users. You have **full internet access** to research Spring WebSocket, STOMP protocol, and real-time patterns.

**Scale:** 100-10K users (small scale - focus on correctness over performance)

**Technology:** Spring WebSocket with STOMP (Simple Text Oriented Messaging Protocol) - the standard Spring approach.

## ⚠️ CRITICAL: Code Quality Requirements

**WRITE CLEAN, MAINTAINABLE, MODULAR WEBSOCKET CODE.**

This is non-negotiable. Every component must be:
- **MODULAR** - Separate connection handling, message routing, and business logic
- **MAINTAINABLE** - Clear message types, documented protocols, easy to debug
- **SECURE** - Validate JWT on connection, check match authorization
- **RESILIENT** - Handle disconnections gracefully, support reconnection

**Modularity Rules:**
```java
// ✅ GOOD: Separate concerns
@Configuration
public class WebSocketConfig { }           // Configuration only

@Component
public class WebSocketAuthInterceptor { }  // Auth only

@Controller
public class ChatWebSocketController { }   // Message routing only

@Service
public class ChatMessageService { }        // Business logic only

// ❌ BAD: Everything in one class
@Controller
public class ChatController {
    // 500 lines mixing config, auth, routing, and business logic
}
```

**Message Format Rules:**
```java
// ✅ GOOD: Clear, typed message records
public record ChatMessage(
    UUID messageId,
    UUID matchId,
    UUID senderId,
    String content,
    MessageType type,
    Instant timestamp
) {}

public enum MessageType {
    TEXT,           // Regular text message
    IMAGE,          // Image attachment
    TYPING,         // User is typing indicator
    READ_RECEIPT,   // Message was read
    DELIVERED       // Message was delivered
}

// ❌ BAD: Untyped Map<String, Object>
```

**Why This Matters:** WebSocket bugs are hard to debug. Clear structure and separation make issues easier to find.

## Scope
Implement WebSocket communication for:

1. **Connection management** - Connect, authenticate, disconnect
2. **Message sending** - Send messages to match conversations
3. **Message receiving** - Receive messages in real-time
4. **Typing indicators** - Show when other user is typing
5. **Read receipts** - Mark messages as read
6. **Presence** - Online/offline status (optional)

## Architecture Overview

```
┌─────────────┐     WebSocket      ┌─────────────────┐
│   Browser   │◄──────────────────►│  Chat Service   │
│  (Vaadin)   │   STOMP/SockJS     │    (8083)       │
└─────────────┘                    └────────┬────────┘
                                            │
                                            ▼
                                   ┌─────────────────┐
                                   │    RabbitMQ     │
                                   │  (for scaling)  │
                                   └─────────────────┘

Message Flow:
1. User A sends message → Chat Service
2. Chat Service saves to DB
3. Chat Service publishes to RabbitMQ
4. Chat Service broadcasts to User B's WebSocket
```

## Implementation Tasks

### Task 1: WebSocket Configuration

```java
/**
 * ============================================================================
 * WEBSOCKET CONFIGURATION
 * ============================================================================
 *
 * PURPOSE:
 * Configure WebSocket endpoints and message broker.
 *
 * STOMP DESTINATIONS:
 * - /app/** - Messages FROM client TO server (application destinations)
 * - /topic/** - Broadcast messages (one-to-many)
 * - /queue/** - User-specific messages (one-to-one)
 * - /user/** - User-specific queues (Spring converts to /queue/user-{userId})
 *
 * ============================================================================
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Register STOMP endpoints that clients connect to.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // =====================================================================
        // /ws - Main WebSocket endpoint
        //
        // SockJS fallback: If WebSocket fails, falls back to HTTP polling
        // Allowed origins: Configure for your frontend URL
        // =====================================================================
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")  // TODO: Restrict in production
            .withSockJS();  // Enable SockJS fallback
    }

    /**
     * Configure the message broker.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // =====================================================================
        // Simple in-memory broker for small scale
        //
        // For production scaling: Use RabbitMQ or ActiveMQ as external broker
        // registry.enableStompBrokerRelay("/topic", "/queue")
        //     .setRelayHost("localhost")
        //     .setRelayPort(61613);
        // =====================================================================

        // Enable simple in-memory broker for /topic and /queue destinations
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages FROM client TO server
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific messages
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Configure client inbound channel (messages FROM client).
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add authentication interceptor
        registration.interceptors(new WebSocketAuthInterceptor());
    }
}
```

### Task 2: Authentication Interceptor

```java
/**
 * ============================================================================
 * WEBSOCKET AUTHENTICATION INTERCEPTOR
 * ============================================================================
 *
 * PURPOSE:
 * Authenticate WebSocket connections using JWT token.
 *
 * HOW IT WORKS:
 * 1. Client connects with JWT in query param or header
 * 2. Interceptor validates JWT
 * 3. Sets Principal for the connection
 * 4. All subsequent messages have authenticated user
 *
 * ============================================================================
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // =====================================================================
        // Only authenticate on CONNECT command
        // =====================================================================
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token == null) {
                log.warn("WebSocket connection attempt without token");
                throw new AuthenticationException("Missing authentication token");
            }

            try {
                // Validate JWT and extract user ID
                Claims claims = tokenProvider.validateToken(token);
                String userId = claims.getSubject();

                // Set the authenticated user as Principal
                accessor.setUser(new StompPrincipal(userId));

                log.info("WebSocket authenticated: userId={}", userId);
            } catch (Exception e) {
                log.warn("WebSocket authentication failed: {}", e.getMessage());
                throw new AuthenticationException("Invalid token");
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // Try Authorization header first
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Fall back to query parameter (for SockJS)
        String token = accessor.getFirstNativeHeader("token");
        return token;
    }
}

/**
 * Simple Principal implementation for WebSocket.
 */
public record StompPrincipal(String userId) implements Principal {
    @Override
    public String getName() {
        return userId;
    }
}
```

### Task 3: Message DTOs

```java
/**
 * ============================================================================
 * WEBSOCKET MESSAGE TYPES
 * ============================================================================
 *
 * All messages sent/received over WebSocket.
 * Using records for immutability and clarity.
 *
 * ============================================================================
 */

// -----------------------------------------------------------------------------
// Incoming: Client → Server
// -----------------------------------------------------------------------------

/**
 * Message sent by client to send a chat message.
 */
public record SendMessageRequest(
    UUID matchId,
    String content,
    MessageType type
) {}

/**
 * Request to mark messages as read.
 */
public record MarkReadRequest(
    UUID matchId,
    UUID lastReadMessageId
) {}

/**
 * Typing indicator from client.
 */
public record TypingIndicator(
    UUID matchId,
    boolean isTyping
) {}

// -----------------------------------------------------------------------------
// Outgoing: Server → Client
// -----------------------------------------------------------------------------

/**
 * Chat message broadcast to conversation participants.
 */
public record ChatMessageEvent(
    UUID messageId,
    UUID matchId,
    UUID senderId,
    String senderName,
    String content,
    MessageType type,
    Instant timestamp
) {}

/**
 * Notification that messages were read.
 */
public record MessagesReadEvent(
    UUID matchId,
    UUID readByUserId,
    UUID lastReadMessageId,
    Instant readAt
) {}

/**
 * Typing indicator broadcast.
 */
public record TypingEvent(
    UUID matchId,
    UUID userId,
    boolean isTyping
) {}

/**
 * Delivery confirmation.
 */
public record MessageDeliveredEvent(
    UUID messageId,
    UUID matchId,
    Instant deliveredAt
) {}

public enum MessageType {
    TEXT,
    IMAGE,
    TYPING,
    READ_RECEIPT,
    SYSTEM  // System messages (e.g., "User joined")
}
```

### Task 4: WebSocket Controller

```java
/**
 * ============================================================================
 * CHAT WEBSOCKET CONTROLLER
 * ============================================================================
 *
 * PURPOSE:
 * Handle incoming WebSocket messages and route to appropriate handlers.
 *
 * STOMP DESTINATIONS:
 * - /app/chat.send - Send a message
 * - /app/chat.typing - Send typing indicator
 * - /app/chat.read - Mark messages as read
 *
 * SUBSCRIPTIONS:
 * - /user/queue/messages - Receive messages for this user
 * - /user/queue/typing - Receive typing indicators
 * - /user/queue/read - Receive read receipts
 *
 * ============================================================================
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle sending a chat message.
     *
     * Client sends to: /app/chat.send
     * Server broadcasts to: /user/{recipientId}/queue/messages
     */
    @MessageMapping("/chat.send")
    public void sendMessage(
            SendMessageRequest request,
            Principal principal) {

        UUID senderId = UUID.fromString(principal.getName());
        log.info("Message received: matchId={}, senderId={}", request.matchId(), senderId);

        // =====================================================================
        // STEP 1: Validate sender is part of this match
        // =====================================================================
        if (!chatMessageService.isUserInMatch(senderId, request.matchId())) {
            log.warn("User {} attempted to send message to match {} they're not part of",
                senderId, request.matchId());
            throw new AccessDeniedException("Not authorized for this conversation");
        }

        // =====================================================================
        // STEP 2: Save message to database
        // =====================================================================
        ChatMessageEvent savedMessage = chatMessageService.saveMessage(
            request.matchId(),
            senderId,
            request.content(),
            request.type()
        );

        // =====================================================================
        // STEP 3: Send to recipient via their personal queue
        // =====================================================================
        UUID recipientId = chatMessageService.getOtherUserId(request.matchId(), senderId);

        // Send to recipient
        messagingTemplate.convertAndSendToUser(
            recipientId.toString(),
            "/queue/messages",
            savedMessage
        );

        // Send delivery confirmation back to sender
        messagingTemplate.convertAndSendToUser(
            senderId.toString(),
            "/queue/delivered",
            new MessageDeliveredEvent(
                savedMessage.messageId(),
                savedMessage.matchId(),
                Instant.now()
            )
        );

        log.info("Message delivered: messageId={}, to={}", savedMessage.messageId(), recipientId);
    }

    /**
     * Handle typing indicator.
     *
     * Client sends to: /app/chat.typing
     * Server broadcasts to: /user/{recipientId}/queue/typing
     */
    @MessageMapping("/chat.typing")
    public void typing(
            TypingIndicator indicator,
            Principal principal) {

        UUID senderId = UUID.fromString(principal.getName());

        // Validate user is in match
        if (!chatMessageService.isUserInMatch(senderId, indicator.matchId())) {
            return;  // Silently ignore
        }

        // Send to recipient
        UUID recipientId = chatMessageService.getOtherUserId(indicator.matchId(), senderId);

        messagingTemplate.convertAndSendToUser(
            recipientId.toString(),
            "/queue/typing",
            new TypingEvent(indicator.matchId(), senderId, indicator.isTyping())
        );
    }

    /**
     * Handle marking messages as read.
     *
     * Client sends to: /app/chat.read
     * Server broadcasts to: /user/{senderId}/queue/read
     */
    @MessageMapping("/chat.read")
    public void markRead(
            MarkReadRequest request,
            Principal principal) {

        UUID readerId = UUID.fromString(principal.getName());

        // Validate and update read status
        chatMessageService.markMessagesAsRead(
            request.matchId(),
            readerId,
            request.lastReadMessageId()
        );

        // Notify the original sender that their messages were read
        UUID senderId = chatMessageService.getOtherUserId(request.matchId(), readerId);

        messagingTemplate.convertAndSendToUser(
            senderId.toString(),
            "/queue/read",
            new MessagesReadEvent(
                request.matchId(),
                readerId,
                request.lastReadMessageId(),
                Instant.now()
            )
        );
    }
}
```

### Task 5: Event Listeners (Connection Events)

```java
/**
 * ============================================================================
 * WEBSOCKET EVENT LISTENER
 * ============================================================================
 *
 * PURPOSE:
 * Handle WebSocket lifecycle events (connect, disconnect, subscribe).
 *
 * USE CASES:
 * - Track online users
 * - Clean up on disconnect
 * - Log for debugging
 *
 * ============================================================================
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserPresenceService presenceService;  // Optional

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getUser() != null ? accessor.getUser().getName() : "unknown";

        log.info("WebSocket connected: userId={}, sessionId={}",
            userId, accessor.getSessionId());

        // Optional: Track user as online
        // presenceService.setOnline(userId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getUser() != null ? accessor.getUser().getName() : "unknown";

        log.info("WebSocket disconnected: userId={}, sessionId={}",
            userId, accessor.getSessionId());

        // Optional: Track user as offline
        // presenceService.setOffline(userId);
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        log.debug("WebSocket subscribe: userId={}, destination={}",
            accessor.getUser() != null ? accessor.getUser().getName() : "unknown",
            accessor.getDestination());
    }
}
```

### Task 6: Client-Side Integration (Vaadin)

```java
/**
 * ============================================================================
 * VAADIN WEBSOCKET CLIENT INTEGRATION
 * ============================================================================
 *
 * Example of connecting to WebSocket from Vaadin UI.
 * This shows the pattern - actual implementation in Vaadin UI service.
 *
 * ============================================================================
 */

// JavaScript to include in Vaadin view
String connectScript = """
    const socket = new SockJS('/ws');
    const stompClient = Stomp.over(socket);

    stompClient.connect(
        { 'Authorization': 'Bearer ' + token },
        function(frame) {
            console.log('Connected: ' + frame);

            // Subscribe to personal message queue
            stompClient.subscribe('/user/queue/messages', function(message) {
                const chatMessage = JSON.parse(message.body);
                // Handle incoming message
                onMessageReceived(chatMessage);
            });

            // Subscribe to typing indicators
            stompClient.subscribe('/user/queue/typing', function(message) {
                const typing = JSON.parse(message.body);
                onTypingIndicator(typing);
            });

            // Subscribe to read receipts
            stompClient.subscribe('/user/queue/read', function(message) {
                const readReceipt = JSON.parse(message.body);
                onReadReceipt(readReceipt);
            });
        },
        function(error) {
            console.error('Connection error: ' + error);
        }
    );

    // Send a message
    function sendMessage(matchId, content) {
        stompClient.send('/app/chat.send', {}, JSON.stringify({
            matchId: matchId,
            content: content,
            type: 'TEXT'
        }));
    }

    // Send typing indicator
    function sendTyping(matchId, isTyping) {
        stompClient.send('/app/chat.typing', {}, JSON.stringify({
            matchId: matchId,
            isTyping: isTyping
        }));
    }
    """;
```

### Task 7: Configuration

**application.yml:**
```yaml
# WebSocket configuration
spring:
  websocket:
    # Max message size (default 64KB)
    max-text-message-size: 65536
    max-binary-message-size: 65536

# For production: Use RabbitMQ as STOMP broker
# spring:
#   rabbitmq:
#     host: localhost
#     port: 5672
#     stomp:
#       port: 61613
```

**Dependencies (pom.xml):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

## Iteration Loop (Repeat Until Complete)

### Phase 1: Basic Connection
```bash
# Start chat service
cd backend/chat-service && mvn spring-boot:run

# Test WebSocket connection (use a WebSocket client tool or browser console)
```
- Verify connection succeeds with valid JWT
- Verify connection fails without JWT

### Phase 2: Message Sending
```bash
# Connect two clients (two browser tabs)
# Send message from client A to client B
```
- Verify message appears in client B
- Verify delivery confirmation in client A

### Phase 3: Typing & Read Receipts
- Send typing indicator → verify it appears
- Mark as read → verify sender sees read receipt

### Phase 4: Error Handling
- Test with invalid match ID
- Test with expired JWT
- Test disconnect/reconnect

### Phase 5: Integration Test
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest {
    @Test
    void shouldSendAndReceiveMessages() {
        // Use StompSession for testing
    }
}
```

## Success Criteria
- [ ] WebSocket connection works with JWT authentication
- [ ] Messages are delivered in real-time
- [ ] Only matched users can message each other
- [ ] Typing indicators work
- [ ] Read receipts work
- [ ] Disconnection is handled gracefully
- [ ] All message types are well-documented
- [ ] Event listeners log connections/disconnections

## When Stuck
1. **Search internet** for Spring WebSocket, STOMP, SockJS examples
2. **Check browser console** for JavaScript errors
3. **Check server logs** for authentication issues
4. **Use STOMP debug mode:** `stompClient.debug = console.log`

## DO NOT
- Skip authentication (security critical)
- Send messages to users not in the match
- Use polling instead of WebSocket (defeats the purpose)
- Forget to handle disconnections
- Send sensitive data in WebSocket without encryption

## Future Enhancements (Not Now)
- RabbitMQ as external STOMP broker (for scaling)
- Message persistence with offline delivery
- Push notifications for mobile
- File/image sharing over WebSocket

---
**Iterate until real-time messaging works end-to-end. Use internet access freely to resolve WebSocket issues.**
