# Architectural Patterns Deep-Dive

## Overview

This document provides comprehensive details on the core architectural patterns implemented in the POC Dating application. Each pattern includes design rationale, implementation details, code examples, and best practices.

---

## 1. Event-Driven Architecture

### 1.1 Pattern Overview

The POC Dating application uses an **event-driven architecture** where microservices communicate asynchronously through message brokers. This decouples services, improves scalability, and enables real-time event processing.

**Key Benefits:**
- Service independence - Services don't directly call each other
- Scalability - Events can be processed asynchronously
- Resilience - Temporary service outages don't block operations
- Auditability - All changes are recorded as events
- Real-time updates - Events enable immediate propagation of state changes

### 1.2 RabbitMQ Setup

**Infrastructure Configuration:**

```yaml
# docker-compose.yml - RabbitMQ service
rabbitmq:
  image: rabbitmq:3.12-management-alpine
  container_name: dating_rabbitmq
  environment:
    RABBITMQ_DEFAULT_USER: guest
    RABBITMQ_DEFAULT_PASS: guest
  ports:
    - "5672:5672"      # AMQP port
    - "15672:15672"    # Management UI
  volumes:
    - rabbitmq_data:/var/lib/rabbitmq
  healthcheck:
    test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
    interval: 30s
    timeout: 10s
    retries: 5
```

**Spring Boot Configuration:**

```java
// application.yml - RabbitMQ configuration
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    connection-timeout: 10000
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1000
          max-interval: 10000
        concurrency: 5
        max-concurrency: 10
```

**Queue and Exchange Declarations:**

```java
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // User Service Exchanges
    public static final String USER_EXCHANGE = "user.exchange";
    public static final String USER_ROUTING_KEY_REGISTERED = "user.registered";
    public static final String USER_ROUTING_KEY_UPDATED = "user.updated";
    public static final String USER_ROUTING_KEY_DELETED = "user.deleted";

    // Match Service Exchanges
    public static final String MATCH_EXCHANGE = "match.exchange";
    public static final String MATCH_ROUTING_KEY_CREATED = "match.created";
    public static final String MATCH_ROUTING_KEY_ENDED = "match.ended";

    // Chat Service Exchanges
    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String CHAT_ROUTING_KEY_MESSAGE_SENT = "chat.message.sent";
    public static final String CHAT_ROUTING_KEY_MESSAGE_READ = "chat.message.read";

    // User Service Queues
    public static final String MATCH_USER_REGISTERED_QUEUE = "match.user.registered.queue";
    public static final String RECOMMENDATION_USER_REGISTERED_QUEUE = "recommendation.user.registered.queue";

    // Match Service Queues
    public static final String CHAT_MATCH_CREATED_QUEUE = "chat.match.created.queue";
    public static final String NOTIFICATION_MATCH_CREATED_QUEUE = "notification.match.created.queue";

    // Chat Service Queues
    public static final String NOTIFICATION_MESSAGE_SENT_QUEUE = "notification.message.sent.queue";

    // === EXCHANGE DECLARATIONS ===
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(USER_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange matchExchange() {
        return new TopicExchange(MATCH_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE, true, false);
    }

    // === USER SERVICE QUEUE DECLARATIONS ===
    @Bean
    public Queue matchUserRegisteredQueue() {
        return QueueBuilder.durable(MATCH_USER_REGISTERED_QUEUE)
            .ttl(86400000)  // 24 hours
            .deadLetterExchange("dlx.exchange")
            .deadLetterRoutingKey("dlx.user.registered")
            .build();
    }

    @Bean
    public Queue recommendationUserRegisteredQueue() {
        return QueueBuilder.durable(RECOMMENDATION_USER_REGISTERED_QUEUE)
            .ttl(86400000)
            .deadLetterExchange("dlx.exchange")
            .deadLetterRoutingKey("dlx.user.registered")
            .build();
    }

    // === MATCH SERVICE QUEUE DECLARATIONS ===
    @Bean
    public Queue chatMatchCreatedQueue() {
        return QueueBuilder.durable(CHAT_MATCH_CREATED_QUEUE)
            .ttl(86400000)
            .deadLetterExchange("dlx.exchange")
            .deadLetterRoutingKey("dlx.match.created")
            .build();
    }

    @Bean
    public Queue notificationMatchCreatedQueue() {
        return QueueBuilder.durable(NOTIFICATION_MATCH_CREATED_QUEUE)
            .ttl(86400000)
            .deadLetterExchange("dlx.exchange")
            .deadLetterRoutingKey("dlx.match.created")
            .build();
    }

    // === CHAT SERVICE QUEUE DECLARATIONS ===
    @Bean
    public Queue notificationMessageSentQueue() {
        return QueueBuilder.durable(NOTIFICATION_MESSAGE_SENT_QUEUE)
            .ttl(86400000)
            .deadLetterExchange("dlx.exchange")
            .deadLetterRoutingKey("dlx.message.sent")
            .build();
    }

    // === BINDING DECLARATIONS ===
    @Bean
    public Binding bindMatchUserRegistered(Queue matchUserRegisteredQueue,
                                          TopicExchange userExchange) {
        return BindingBuilder.bind(matchUserRegisteredQueue)
            .to(userExchange)
            .with(USER_ROUTING_KEY_REGISTERED);
    }

    @Bean
    public Binding bindRecommendationUserRegistered(Queue recommendationUserRegisteredQueue,
                                                   TopicExchange userExchange) {
        return BindingBuilder.bind(recommendationUserRegisteredQueue)
            .to(userExchange)
            .with(USER_ROUTING_KEY_REGISTERED);
    }

    @Bean
    public Binding bindChatMatchCreated(Queue chatMatchCreatedQueue,
                                       TopicExchange matchExchange) {
        return BindingBuilder.bind(chatMatchCreatedQueue)
            .to(matchExchange)
            .with(MATCH_ROUTING_KEY_CREATED);
    }

    @Bean
    public Binding bindNotificationMatchCreated(Queue notificationMatchCreatedQueue,
                                               TopicExchange matchExchange) {
        return BindingBuilder.bind(notificationMatchCreatedQueue)
            .to(matchExchange)
            .with(MATCH_ROUTING_KEY_CREATED);
    }

    @Bean
    public Binding bindNotificationMessageSent(Queue notificationMessageSentQueue,
                                              TopicExchange chatExchange) {
        return BindingBuilder.bind(notificationMessageSentQueue)
            .to(chatExchange)
            .with(CHAT_ROUTING_KEY_MESSAGE_SENT);
    }

    // === DEAD LETTER EXCHANGE ===
    @Bean
    public Exchange deadLetterExchange() {
        return new TopicExchange("dlx.exchange", true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("dlx.queue").build();
    }

    @Bean
    public Binding bindDeadLetter(Queue deadLetterQueue, Exchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
            .to((TopicExchange) deadLetterExchange)
            .with("dlx.*");
    }
}
```

### 1.3 Event Publishing

**Event Model Classes:**

```java
import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Base event class
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {
    private String eventId = UUID.randomUUID().toString();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private Instant timestamp = Instant.now();

    private String source;
    private String eventType;
}

// User registered event
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent extends BaseEvent {
    private UUID userId;
    private String email;
    private String username;
    private Instant registeredAt;

    public UserRegisteredEvent(UUID userId, String email, String username) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.registeredAt = Instant.now();
        setSource("user-service");
        setEventType("USER_REGISTERED");
    }
}

// User updated event
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdatedEvent extends BaseEvent {
    private UUID userId;
    private String fieldUpdated;
    private Object previousValue;
    private Object newValue;

    public UserUpdatedEvent(UUID userId, String fieldUpdated, Object previousValue, Object newValue) {
        this.userId = userId;
        this.fieldUpdated = fieldUpdated;
        this.previousValue = previousValue;
        this.newValue = newValue;
        setSource("user-service");
        setEventType("USER_UPDATED");
    }
}

// Match created event
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchCreatedEvent extends BaseEvent {
    private UUID matchId;
    private UUID user1Id;
    private UUID user2Id;
    private Instant matchedAt;

    public MatchCreatedEvent(UUID matchId, UUID user1Id, UUID user2Id) {
        this.matchId = matchId;
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.matchedAt = Instant.now();
        setSource("match-service");
        setEventType("MATCH_CREATED");
    }
}

// Message sent event
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageSentEvent extends BaseEvent {
    private UUID messageId;
    private UUID matchId;
    private UUID senderId;
    private UUID receiverId;
    private String content;
    private Instant sentAt;

    public MessageSentEvent(UUID messageId, UUID matchId, UUID senderId, UUID receiverId, String content) {
        this.messageId = messageId;
        this.matchId = matchId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.sentAt = Instant.now();
        setSource("chat-service");
        setEventType("MESSAGE_SENT");
    }
}
```

**Publisher Implementation:**

```java
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes a user registration event to all interested services
     */
    public void publishUserRegistered(UserRegisteredEvent event) {
        log.info("Publishing UserRegistered event: userId={}, email={}",
                event.getUserId(), event.getEmail());

        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.USER_EXCHANGE,
                RabbitMQConfig.USER_ROUTING_KEY_REGISTERED,
                event,
                message -> {
                    message.getMessageProperties().setHeader("X-Event-ID", event.getEventId());
                    message.getMessageProperties().setHeader("X-Event-Type", "USER_REGISTERED");
                    message.getMessageProperties().setHeader("X-Timestamp", event.getTimestamp().toString());
                    return message;
                }
            );
            log.debug("UserRegistered event published successfully");
        } catch (Exception e) {
            log.error("Failed to publish UserRegistered event", e);
            throw new EventPublishingException("Failed to publish user registered event", e);
        }
    }

    /**
     * Publishes a match created event
     */
    public void publishMatchCreated(MatchCreatedEvent event) {
        log.info("Publishing MatchCreated event: matchId={}, users={}-{}",
                event.getMatchId(), event.getUser1Id(), event.getUser2Id());

        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.MATCH_EXCHANGE,
                RabbitMQConfig.MATCH_ROUTING_KEY_CREATED,
                event,
                message -> {
                    message.getMessageProperties().setHeader("X-Event-ID", event.getEventId());
                    message.getMessageProperties().setHeader("X-Event-Type", "MATCH_CREATED");
                    return message;
                }
            );
            log.debug("MatchCreated event published successfully");
        } catch (Exception e) {
            log.error("Failed to publish MatchCreated event", e);
            throw new EventPublishingException("Failed to publish match created event", e);
        }
    }

    /**
     * Publishes a message sent event
     */
    public void publishMessageSent(MessageSentEvent event) {
        log.info("Publishing MessageSent event: messageId={}, matchId={}",
                event.getMessageId(), event.getMatchId());

        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                RabbitMQConfig.CHAT_ROUTING_KEY_MESSAGE_SENT,
                event
            );
        } catch (Exception e) {
            log.error("Failed to publish MessageSent event", e);
            throw new EventPublishingException("Failed to publish message sent event", e);
        }
    }
}

// Exception class
public class EventPublishingException extends RuntimeException {
    public EventPublishingException(String message) {
        super(message);
    }

    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 1.4 Event Consumption

**Consumer Implementation in Match Service:**

```java
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventListener {

    private final SwipeService swipeService;
    private final FeedService feedService;

    /**
     * Listens for user.registered events and initializes swipe data
     */
    @RabbitListener(queues = RabbitMQConfig.MATCH_USER_REGISTERED_QUEUE)
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegistered event: userId={}", event.getUserId());

        try {
            // Initialize empty swipe history for new user
            swipeService.initializeSwipeHistory(event.getUserId());

            // Generate initial feed
            feedService.generateInitialFeed(event.getUserId());

            log.info("Successfully initialized match data for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Error processing UserRegistered event for user: {}",
                     event.getUserId(), e);
            // Exception will trigger retry (configured in application.yml)
            throw new EventProcessingException("Failed to process user registered event", e);
        }
    }

    /**
     * Listens for user.updated events and refreshes cached data
     */
    @RabbitListener(queues = "match.user.updated.queue")
    public void handleUserUpdated(UserUpdatedEvent event) {
        log.info("Received UserUpdated event: userId={}, field={}",
                event.getUserId(), event.getFieldUpdated());

        try {
            // Refresh cached user preferences if relevant fields changed
            if (isPreferenceField(event.getFieldUpdated())) {
                feedService.refreshUserFeed(event.getUserId());
                log.info("Refreshed feed for user: {}", event.getUserId());
            }
        } catch (Exception e) {
            log.error("Error processing UserUpdated event", e);
            throw new EventProcessingException("Failed to process user updated event", e);
        }
    }

    private boolean isPreferenceField(String field) {
        return field.matches("(?i)age|distance|gender|location");
    }
}

// Exception class
public class EventProcessingException extends RuntimeException {
    public EventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**Consumer Implementation in Chat Service:**

```java
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchEventListener {

    private final ChatRoomService chatRoomService;
    private final NotificationPublisher notificationPublisher;

    /**
     * Listens for match.created events and creates chat room
     */
    @RabbitListener(queues = RabbitMQConfig.CHAT_MATCH_CREATED_QUEUE)
    public void handleMatchCreated(MatchCreatedEvent event) {
        log.info("Received MatchCreated event: matchId={}, users={}-{}",
                event.getMatchId(), event.getUser1Id(), event.getUser2Id());

        try {
            // Create chat room for matched users
            ChatRoom chatRoom = chatRoomService.createChatRoom(
                event.getMatchId(),
                event.getUser1Id(),
                event.getUser2Id()
            );

            log.info("Chat room created: {}", chatRoom.getId());
        } catch (Exception e) {
            log.error("Error processing MatchCreated event", e);
            throw new EventProcessingException("Failed to create chat room", e);
        }
    }
}
```

### 1.5 Event Flow Diagrams

**User Registration Flow:**
```
┌─────────────┐
│   Browser   │
└──────┬──────┘
       │ POST /api/users/auth/register
       ▼
┌──────────────────┐
│ User Service API │
└──────┬───────────┘
       │ 1. Validate input
       │ 2. Hash password
       │ 3. Save user
       │ 4. Generate JWT
       ▼
┌─────────────────────────────┐
│ Publish UserRegisteredEvent │
└──────┬──────────────────────┘
       │ Publish to user.exchange
       │ Routing key: user.registered
       ▼
┌──────────────────┐
│   RabbitMQ       │
└──────┬───────────┘
       │
       ├─► Queue: match.user.registered.queue
       │   └─► Match Service (Initialize swipe history)
       │
       ├─► Queue: recommendation.user.registered.queue
       │   └─► Recommendation Service (Generate initial recommendations)
       │
       └─► Queue: notification.user.registered.queue
           └─► Notification Service (Send welcome email)
```

**Match Creation and Notification Flow:**
```
┌──────────────────┐
│  Match Service   │
│ (Swipe matched)  │
└──────┬───────────┘
       │ 1. Detect mutual swipes
       │ 2. Create Match record
       │ 3. Publish MatchCreatedEvent
       ▼
┌─────────────────────────────┐
│ Publish MatchCreatedEvent   │
└──────┬──────────────────────┘
       │ Publish to match.exchange
       │ Routing key: match.created
       ▼
┌──────────────────┐
│   RabbitMQ       │
└──────┬───────────┘
       │
       ├─► Queue: chat.match.created.queue
       │   └─► Chat Service (Create chat room)
       │
       └─► Queue: notification.match.created.queue
           └─► Notification Service (Send "You have a match!" notification)
```

---

## 2. Caching Strategy

### 2.1 Redis Configuration

**Docker Compose Setup:**

```yaml
redis:
  image: redis:7-alpine
  container_name: dating_redis
  ports:
    - "6379:6379"
  volumes:
    - redis_data:/data
  command: redis-server --appendonly yes
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 30s
    timeout: 10s
    retries: 5
```

**Spring Boot Configuration:**

```yaml
# application.yml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000
    jedis:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms
  cache:
    type: redis
    redis:
      time-to-live: 1h
      cache-null-values: false
    cache-names:
      - users
      - userProfiles
      - userPreferences
      - feeds
      - matches
      - recommendations
      - sessions
```

### 2.2 Cache Keys and TTLs

**Comprehensive Cache Key Strategy:**

```java
import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class CacheKeys {

    // User Service Cache Keys
    public static class User {
        public static String profile(UUID userId) {
            return "user:" + userId + ":profile";
        }

        public static String preferences(UUID userId) {
            return "user:" + userId + ":preferences";
        }

        public static String byEmail(String email) {
            return "user:email:" + email;
        }

        public static String byUsername(String username) {
            return "user:username:" + username;
        }

        public static String stats(UUID userId) {
            return "user:" + userId + ":stats";
        }
    }

    // Match Service Cache Keys
    public static class Match {
        public static String feed(UUID userId) {
            return "match:" + userId + ":feed";
        }

        public static String feedPage(UUID userId, int page, int size) {
            return "match:" + userId + ":feed:page:" + page + ":size:" + size;
        }

        public static String matchesList(UUID userId) {
            return "match:" + userId + ":matches";
        }

        public static String matchDetails(UUID matchId) {
            return "match:" + matchId + ":details";
        }

        public static String swipeCount(UUID userId) {
            return "match:" + userId + ":swipe:count";
        }

        public static String lastSwipeTime(UUID userId) {
            return "match:" + userId + ":last:swipe:time";
        }
    }

    // Recommendation Service Cache Keys
    public static class Recommendation {
        public static String forUser(UUID userId) {
            return "recommendation:" + userId + ":candidates";
        }

        public static String scores(UUID userId) {
            return "recommendation:" + userId + ":scores";
        }

        public static String modelVersion() {
            return "recommendation:model:version";
        }
    }

    // Chat Service Cache Keys
    public static class Chat {
        public static String messages(UUID matchId, int page) {
            return "chat:" + matchId + ":messages:page:" + page;
        }

        public static String conversation(UUID matchId) {
            return "chat:" + matchId + ":conversation";
        }

        public static String unreadCount(UUID userId) {
            return "chat:" + userId + ":unread:count";
        }
    }

    // Session Cache Keys
    public static class Session {
        public static String token(String tokenId) {
            return "session:token:" + tokenId;
        }

        public static String userSessions(UUID userId) {
            return "session:" + userId + ":sessions";
        }

        public static String refreshToken(String tokenId) {
            return "session:refresh:" + tokenId;
        }
    }
}

/**
 * Cache TTL configuration (in seconds)
 */
public final class CacheTTL {
    // User data - 1 hour (frequently accessed, not time-sensitive)
    public static final long USER_PROFILE = 3600;
    public static final long USER_PREFERENCES = 3600;

    // Match data - 24 hours (expensive to compute, relatively stable)
    public static final long USER_FEED = 86400;
    public static final long FEED_PAGE = 86400;
    public static final long MATCHES_LIST = 3600;

    // Recommendations - 24 hours (ML model runs once daily)
    public static final long RECOMMENDATIONS = 86400;

    // Chat - 1 hour (recent conversations)
    public static final long MESSAGES = 3600;

    // Sessions - 30 minutes (active session)
    public static final long SESSION = 1800;
    public static final long REFRESH_TOKEN = 604800; // 7 days

    // Rate limiting counters - 1 minute
    public static final long RATE_LIMIT = 60;
}
```

### 2.3 Spring Cache Annotations

**User Service Caching:**

```java
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Get user by ID with caching
     * Cache key: user:{userId}:profile
     * TTL: 1 hour
     */
    @Cacheable(
        value = "users",
        key = "T(com.dating.util.CacheKeys).User.profile(#userId)",
        unless = "#result == null"
    )
    public UserDto getUserById(UUID userId) {
        log.info("Fetching user from database: {}", userId);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        return mapToDto(user);
    }

    /**
     * Get user by email with caching
     */
    @Cacheable(
        value = "users",
        key = "T(com.dating.util.CacheKeys).User.byEmail(#email)",
        unless = "#result == null"
    )
    public UserDto getUserByEmail(String email) {
        log.info("Fetching user by email from database: {}", email);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException(email));
        return mapToDto(user);
    }

    /**
     * Update user with cache invalidation
     * Evicts: user profile, stats
     */
    @CacheEvict(
        value = "users",
        key = "T(com.dating.util.CacheKeys).User.profile(#userId)"
    )
    @CachePut(
        value = "users",
        key = "T(com.dating.util.CacheKeys).User.profile(#userId)"
    )
    public UserDto updateUser(UUID userId, UpdateUserRequest request) {
        log.info("Updating user: {}", userId);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setBio(request.getBio());

        User updated = userRepository.save(user);

        // Also evict preferences cache if they were updated
        if (request.getPreferences() != null) {
            String prefsKey = CacheKeys.User.preferences(userId);
            redisTemplate.delete(prefsKey);
        }

        return mapToDto(updated);
    }

    /**
     * Get user preferences with caching
     * Cache key: user:{userId}:preferences
     * TTL: 1 hour
     */
    @Cacheable(
        value = "userPreferences",
        key = "T(com.dating.util.CacheKeys).User.preferences(#userId)",
        unless = "#result == null"
    )
    public PreferencesDto getUserPreferences(UUID userId) {
        log.info("Fetching preferences from database: {}", userId);
        UserPreferences prefs = userRepository.findPreferencesById(userId)
            .orElseThrow(() -> new PreferencesNotFoundException(userId));
        return mapToDto(prefs);
    }

    /**
     * Delete user - evict all user-related caches
     */
    @CacheEvict(
        value = {"users", "userPreferences"},
        allEntries = true
    )
    public void deleteUser(UUID userId) {
        log.info("Deleting user: {}", userId);
        userRepository.deleteById(userId);
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .username(user.getUsername())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .build();
    }
}
```

**Match Service Caching:**

```java
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final UserRepository userRepository;
    private final SwipeRepository swipeRepository;
    private final RecommendationServiceClient recommendationClient;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Generate and cache user feed
     * Cache key: match:{userId}:feed
     * TTL: 24 hours (expensive operation)
     */
    @Cacheable(
        value = "feeds",
        key = "T(com.dating.util.CacheKeys).Match.feed(#userId)",
        unless = "#result == null || #result.isEmpty()"
    )
    public List<UserProfileDto> generateFeed(UUID userId) {
        log.info("Generating fresh feed for user: {}", userId);

        // 1. Get user preferences
        UserPreferences prefs = userRepository.findPreferencesById(userId)
            .orElseThrow(() -> new PreferencesNotFoundException(userId));

        // 2. Get already swiped users
        Set<UUID> swipedUsers = swipeRepository.findAllTargetsByUserId(userId);
        swipedUsers.add(userId); // Don't show self

        // 3. Filter candidates by preferences
        List<User> candidates = userRepository.findCandidates(
            prefs.getMinAge(),
            prefs.getMaxAge(),
            prefs.getMaxDistance(),
            userId,
            swipedUsers
        );

        // 4. Get scores from recommendation service
        Map<UUID, Double> scores = recommendationClient.scoreProfiles(
            userId,
            candidates.stream().map(User::getId).collect(Collectors.toList())
        );

        // 5. Sort by score
        List<UserProfileDto> feed = candidates.stream()
            .map(this::mapToProfileDto)
            .sorted((a, b) -> Double.compare(
                scores.getOrDefault(b.getId(), 0.0),
                scores.getOrDefault(a.getId(), 0.0)
            ))
            .limit(100) // Cache top 100
            .collect(Collectors.toList());

        log.info("Generated feed with {} candidates for user: {}", feed.size(), userId);
        return feed;
    }

    /**
     * Get paginated feed results
     * Each page is cached separately
     */
    @Cacheable(
        value = "feeds",
        key = "T(com.dating.util.CacheKeys).Match.feedPage(#userId, #page, #pageSize)",
        unless = "#result == null"
    )
    public Page<UserProfileDto> getFeedPage(UUID userId, int page, int pageSize) {
        List<UserProfileDto> fullFeed = generateFeed(userId);

        int start = page * pageSize;
        int end = Math.min(start + pageSize, fullFeed.size());

        if (start >= fullFeed.size()) {
            return new PageImpl<>(Collections.emptyList(),
                               PageRequest.of(page, pageSize),
                               fullFeed.size());
        }

        return new PageImpl<>(
            fullFeed.subList(start, end),
            PageRequest.of(page, pageSize),
            fullFeed.size()
        );
    }

    /**
     * Record swipe and evict feed cache
     * Feed must be regenerated after each swipe
     */
    @CacheEvict(
        value = "feeds",
        beforeInvocation = false
    )
    public SwipeDto recordSwipe(UUID userId, UUID targetId, SwipeType type) {
        log.info("Recording swipe: user={}, target={}, type={}", userId, targetId, type);

        Swipe swipe = Swipe.builder()
            .userId(userId)
            .targetUserId(targetId)
            .swipeType(type)
            .build();

        Swipe saved = swipeRepository.save(swipe);

        // Evict all feed caches for this user
        String feedKey = CacheKeys.Match.feed(userId);
        redisTemplate.delete(feedKey);

        return mapToDto(saved);
    }

    /**
     * Refresh user feed on demand
     */
    @CacheEvict(
        value = "feeds",
        key = "T(com.dating.util.CacheKeys).Match.feed(#userId)"
    )
    public void refreshUserFeed(UUID userId) {
        log.info("Explicitly refreshing feed for user: {}", userId);
    }
}
```

### 2.4 Cache Invalidation Strategies

**Event-Based Invalidation:**

```java
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationListener {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Listen for user preference updates and invalidate related caches
     */
    @EventListener
    public void onUserPreferencesUpdated(UserPreferencesUpdatedEvent event) {
        log.info("Invalidating caches for user preference update: {}", event.getUserId());

        UUID userId = event.getUserId();

        // Evict preference cache
        String prefsKey = CacheKeys.User.preferences(userId);
        redisTemplate.delete(prefsKey);

        // Evict feed cache (preferences affect feed generation)
        String feedKey = CacheKeys.Match.feed(userId);
        redisTemplate.delete(feedKey);

        // Also invalidate feed pages
        for (int page = 0; page < 10; page++) {
            String pageKey = CacheKeys.Match.feedPage(userId, page, 20);
            redisTemplate.delete(pageKey);
        }
    }

    /**
     * Listen for match events and invalidate related caches
     */
    @EventListener
    public void onMatchCreated(MatchCreatedEvent event) {
        log.info("Invalidating caches for match creation: {}", event.getMatchId());

        // Invalidate matches list for both users
        String matches1Key = CacheKeys.Match.matchesList(event.getUser1Id());
        String matches2Key = CacheKeys.Match.matchesList(event.getUser2Id());

        redisTemplate.delete(matches1Key);
        redisTemplate.delete(matches2Key);
    }

    /**
     * Listen for message events
     */
    @EventListener
    public void onMessageSent(MessageSentEvent event) {
        log.info("Invalidating message cache for match: {}", event.getMatchId());

        // Invalidate all message pages for this match
        for (int page = 0; page < 100; page++) {
            String messageKey = CacheKeys.Chat.messages(event.getMatchId(), page);
            redisTemplate.delete(messageKey);
        }

        // Invalidate conversation summary
        String convKey = CacheKeys.Chat.conversation(event.getMatchId());
        redisTemplate.delete(convKey);

        // Update unread count for receiver
        String unreadKey = CacheKeys.Chat.unreadCount(event.getReceiverId());
        Long unreadCount = (Long) redisTemplate.opsForValue().get(unreadKey);
        if (unreadCount == null) {
            redisTemplate.opsForValue().set(unreadKey, 1L, Duration.ofHours(24));
        } else {
            redisTemplate.opsForValue().increment(unreadKey);
        }
    }
}
```

**Manual Cache Warming:**

```java
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheWarmingService {

    private final UserService userService;
    private final FeedService feedService;
    private final MatchService matchService;

    /**
     * Pre-warm user profile caches for active users
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void warmUserProfileCaches() {
        log.info("Starting cache warming for user profiles");

        List<UUID> activeUsers = userRepository.findActiveUsersLastDay();

        activeUsers.stream()
            .peek(userId -> log.debug("Warming cache for user: {}", userId))
            .forEach(userId -> {
                try {
                    userService.getUserById(userId);
                    userService.getUserPreferences(userId);
                } catch (Exception e) {
                    log.warn("Error warming cache for user {}: {}", userId, e.getMessage());
                }
            });

        log.info("Completed warming {} user profile caches", activeUsers.size());
    }

    /**
     * Pre-warm feed caches for active users
     * Runs daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void warmFeedCaches() {
        log.info("Starting cache warming for feeds");

        List<UUID> activeUsers = userRepository.findActiveUsersLastDay();
        int successCount = 0;
        int failureCount = 0;

        for (UUID userId : activeUsers) {
            try {
                feedService.generateFeed(userId);
                successCount++;
            } catch (Exception e) {
                log.warn("Error generating feed for user {}: {}", userId, e.getMessage());
                failureCount++;
            }
        }

        log.info("Completed warming feed caches: success={}, failures={}", successCount, failureCount);
    }
}
```

---

## 3. JWT Authentication Flow

### 3.1 Token Generation

**JWT Configuration:**

```java
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration.ms:900000}") // 15 minutes default
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration.ms:604800000}") // 7 days default
    private long refreshTokenExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Generate access token (JWT)
     *
     * Token Claims:
     * - sub (subject): User ID
     * - email: User email
     * - username: Username
     * - roles: User roles
     * - iat: Issued at timestamp
     * - exp: Expiration timestamp
     * - iss: Issuer (dating-app)
     * - jti: JWT ID (unique identifier)
     */
    public AuthTokens generateTokens(User user) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(jwtExpirationMs, ChronoUnit.MILLIS);

        // Generate access token
        String accessToken = Jwts.builder()
            .setSubject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("username", user.getUsername())
            .claim("roles", user.getRoles())
            .claim("firstName", user.getFirstName())
            .claim("lastName", user.getLastName())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiryDate))
            .setIssuer("dating-app")
            .setId(UUID.randomUUID().toString())
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();

        // Generate refresh token
        Instant refreshExpiryDate = now.plus(refreshTokenExpirationMs, ChronoUnit.MILLIS);
        String refreshToken = Jwts.builder()
            .setSubject(user.getId().toString())
            .claim("type", "refresh")
            .claim("email", user.getEmail())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(refreshExpiryDate))
            .setIssuer("dating-app")
            .setId(UUID.randomUUID().toString())
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();

        return AuthTokens.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtExpirationMs / 1000)
            .build();
    }

    /**
     * Extract user ID from token
     */
    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject());
    }

    /**
     * Extract all claims from token
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}

// DTO classes
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthTokens {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
}
```

### 3.2 User Registration and Login

**Registration:**

```java
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EventPublisher eventPublisher;

    /**
     * Register new user
     *
     * Steps:
     * 1. Validate input
     * 2. Check if email already exists
     * 3. Hash password with BCrypt
     * 4. Save user entity
     * 5. Generate tokens
     * 6. Publish UserRegisteredEvent
     */
    @Transactional
    public AuthResponse register(UserRegistrationRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        // Validate input
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already registered");
        }

        // Create user entity
        User user = User.builder()
            .email(request.getEmail())
            .username(request.getUsername())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .status(UserStatus.ACTIVE)
            .roles(Set.of("ROLE_USER"))
            .createdAt(Instant.now())
            .build();

        // Save user
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {} ({})", savedUser.getEmail(), savedUser.getId());

        // Generate tokens
        AuthTokens tokens = tokenProvider.generateTokens(savedUser);

        // Store refresh token
        RefreshToken refreshToken = RefreshToken.builder()
            .userId(savedUser.getId())
            .token(tokens.getRefreshToken())
            .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
            .build();
        refreshTokenRepository.save(refreshToken);

        // Publish event
        eventPublisher.publishUserRegistered(
            new UserRegisteredEvent(savedUser.getId(), savedUser.getEmail(), savedUser.getUsername())
        );

        return AuthResponse.builder()
            .userId(savedUser.getId())
            .email(savedUser.getEmail())
            .username(savedUser.getUsername())
            .accessToken(tokens.getAccessToken())
            .refreshToken(tokens.getRefreshToken())
            .tokenType(tokens.getTokenType())
            .expiresIn(tokens.getExpiresIn())
            .build();
    }

    /**
     * Login user
     *
     * Steps:
     * 1. Find user by email
     * 2. Validate password
     * 3. Generate new tokens
     * 4. Store refresh token
     * 5. Return tokens
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.getEmail());

        // Find user
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Invalid password for user: {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Check if user is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserInactiveException("User account is not active");
        }

        // Generate tokens
        AuthTokens tokens = tokenProvider.generateTokens(user);

        // Store refresh token
        RefreshToken refreshToken = RefreshToken.builder()
            .userId(user.getId())
            .token(tokens.getRefreshToken())
            .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
            .build();
        refreshTokenRepository.save(refreshToken);

        log.info("User logged in successfully: {}", user.getEmail());

        return AuthResponse.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .username(user.getUsername())
            .accessToken(tokens.getAccessToken())
            .refreshToken(tokens.getRefreshToken())
            .tokenType(tokens.getTokenType())
            .expiresIn(tokens.getExpiresIn())
            .build();
    }
}
```

**Login Controller:**

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
```

### 3.3 Token Validation and Refresh

**JWT Filter:**

```java
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = extractTokenFromRequest(request);

            if (jwt != null && tokenProvider.validateToken(jwt)) {
                // Extract user ID from token
                UUID userId = tokenProvider.getUserIdFromToken(jwt);

                // Create authentication token
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    new ArrayList<>()
                );

                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Add user ID to request header for downstream services
                request.setAttribute("userId", userId);
                log.debug("JWT token validated for user: {}", userId);
            }
        } catch (Exception e) {
            log.error("Could not validate JWT token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     * Expected format: "Bearer {token}"
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

**Token Refresh Endpoint:**

```java
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh requested");

        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
}

// Implementation
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    /**
     * Refresh access token using refresh token
     */
    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue) {
        log.info("Processing token refresh");

        // Find refresh token in database
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
            .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        // Check if expired
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidTokenException("Refresh token has expired");
        }

        // Get user
        User user = userRepository.findById(refreshToken.getUserId())
            .orElseThrow(() -> new UserNotFoundException(refreshToken.getUserId()));

        // Generate new tokens
        AuthTokens newTokens = tokenProvider.generateTokens(user);

        // Update refresh token
        refreshToken.setToken(newTokens.getRefreshToken());
        refreshToken.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);

        log.info("Token refreshed successfully for user: {}", user.getId());

        return AuthResponse.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .username(user.getUsername())
            .accessToken(newTokens.getAccessToken())
            .refreshToken(newTokens.getRefreshToken())
            .tokenType(newTokens.getTokenType())
            .expiresIn(newTokens.getExpiresIn())
            .build();
    }
}
```

### 3.4 Security Configuration

**Spring Security Configuration:**

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // 12 rounds
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors()
            .and()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                // Public endpoints
                .antMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()
                .antMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Protected endpoints
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(
                new JwtAuthenticationFilter(tokenProvider),
                UsernamePasswordAuthenticationFilter.class
            )
            .exceptionHandling()
            .authenticationEntryPoint(new JwtAuthenticationEntryPoint());

        return http.build();
    }
}
```

---

## 4. Feed Generation Algorithm

### 4.1 Candidate Filtering

**Repository Query:**

```java
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find candidate profiles for feed generation
     *
     * Filters applied:
     * 1. Age range (min/max age)
     * 2. Distance radius (location-based)
     * 3. Already swiped users (don't show again)
     * 4. Active users only
     * 5. User is not viewing themselves
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.status = 'ACTIVE'
        AND u.id != :userId
        AND u.id NOT IN :excludedUserIds
        AND YEAR(CURRENT_DATE) - YEAR(u.dateOfBirth) >= :minAge
        AND YEAR(CURRENT_DATE) - YEAR(u.dateOfBirth) <= :maxAge
        AND (
            6371 * acos(
                cos(radians(:userLatitude))
                * cos(radians(u.location.latitude))
                * cos(radians(u.location.longitude) - radians(:userLongitude))
                + sin(radians(:userLatitude))
                * sin(radians(u.location.latitude))
            )
        ) <= :maxDistanceKm
        ORDER BY u.createdAt DESC
        """)
    List<User> findCandidates(
        @Param("userId") UUID userId,
        @Param("minAge") int minAge,
        @Param("maxAge") int maxAge,
        @Param("userLatitude") double userLatitude,
        @Param("userLongitude") double userLongitude,
        @Param("maxDistanceKm") int maxDistanceKm,
        @Param("excludedUserIds") Set<UUID> excludedUserIds
    );

    /**
     * Get all users that the current user has swiped
     */
    @Query("""
        SELECT DISTINCT s.targetUserId FROM Swipe s
        WHERE s.userId = :userId
        """)
    Set<UUID> findAllTargetsByUserId(@Param("userId") UUID userId);

    /**
     * Check if users match (mutual swipes)
     */
    @Query("""
        SELECT COUNT(s) > 0 FROM Swipe s
        WHERE (s.userId = :userId1 AND s.targetUserId = :userId2 AND s.swipeType = 'LIKE')
        OR (s.userId = :userId2 AND s.targetUserId = :userId1 AND s.swipeType = 'LIKE')
        """)
    boolean checkMutualLike(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);
}
```

### 4.2 Scoring Logic

**Recommendation Service Client:**

```java
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "recommendation-service", url = "${recommendation.service.url}")
public interface RecommendationServiceClient {

    @PostMapping("/api/recommendations/score")
    Map<UUID, Double> scoreProfiles(
        @RequestParam UUID userId,
        @RequestBody List<UUID> candidateIds
    );
}

// Scoring implementation in Recommendation Service
@Service
@RequiredArgsConstructor
@Slf4j
public class ScoringService {

    private final UserRepository userRepository;
    private final SwipeRepository swipeRepository;
    private final ModelService modelService;

    /**
     * Score candidate profiles for a user
     *
     * Scoring factors (weighted):
     * 1. Profile completeness (10%): bio, photos, interests
     * 2. Preference match (40%): age, location, interests alignment
     * 3. Activity score (20%): recent activity, response rate
     * 4. Engagement probability (30%): ML model prediction
     */
    public Map<UUID, Double> scoreProfiles(UUID userId, List<UUID> candidateIds) {
        log.info("Scoring {} candidates for user: {}", candidateIds.size(), userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        UserPreferences preferences = user.getPreferences();

        Map<UUID, Double> scores = new HashMap<>();

        for (UUID candidateId : candidateIds) {
            try {
                User candidate = userRepository.findById(candidateId)
                    .orElseThrow(() -> new UserNotFoundException(candidateId));

                double score = calculateScore(user, candidate, preferences);
                scores.put(candidateId, score);

                log.debug("Scored candidate {}: {}", candidateId, score);
            } catch (Exception e) {
                log.warn("Error scoring candidate {}: {}", candidateId, e.getMessage());
                scores.put(candidateId, 0.0);
            }
        }

        return scores;
    }

    /**
     * Calculate comprehensive score for a candidate
     */
    private double calculateScore(User currentUser, User candidate, UserPreferences prefs) {
        // Weight allocation
        double profileScore = calculateProfileCompleteness(candidate) * 0.10;
        double preferenceScore = calculatePreferenceMatch(candidate, prefs) * 0.40;
        double activityScore = calculateActivityScore(candidate) * 0.20;
        double mlScore = calculateMLScore(currentUser, candidate) * 0.30;

        double totalScore = profileScore + preferenceScore + activityScore + mlScore;

        log.debug("Score breakdown - profile: {}, preference: {}, activity: {}, ml: {}",
                 profileScore, preferenceScore, activityScore, mlScore);

        return totalScore;
    }

    /**
     * Profile completeness: 0-1 scale
     * Factors: bio length, photo count, interests filled
     */
    private double calculateProfileCompleteness(User user) {
        double score = 0;

        if (user.getBio() != null && user.getBio().length() > 50) score += 0.25;
        if (user.getPhotos() != null && user.getPhotos().size() >= 3) score += 0.25;
        if (user.getInterests() != null && user.getInterests().size() >= 5) score += 0.25;
        if (user.getVerificationStatus() == VerificationStatus.VERIFIED) score += 0.25;

        return score;
    }

    /**
     * Preference match: 0-1 scale
     * Factors: age match, location proximity, shared interests
     */
    private double calculatePreferenceMatch(User candidate, UserPreferences prefs) {
        double score = 0;

        // Age match (10-50 km radius)
        int age = calculateAge(candidate.getDateOfBirth());
        if (age >= prefs.getMinAge() && age <= prefs.getMaxAge()) score += 0.33;

        // Location proximity
        double distance = calculateDistance(
            prefs.getLatitude(), prefs.getLongitude(),
            candidate.getLocation().getLatitude(), candidate.getLocation().getLongitude()
        );
        if (distance <= prefs.getMaxDistance()) score += 0.33;

        // Interest overlap
        Set<String> sharedInterests = new HashSet<>(candidate.getInterests());
        sharedInterests.retainAll(prefs.getInterestedIn());
        double interestPercentage = (double) sharedInterests.size() / prefs.getInterestedIn().size();
        score += interestPercentage * 0.34;

        return score;
    }

    /**
     * Activity score: 0-1 scale
     * Factors: last login, swipe frequency, message response rate
     */
    private double calculateActivityScore(User user) {
        double score = 0;

        // Recent login (within last 7 days: 0.33)
        if (user.getLastLoginAt() != null) {
            long daysSinceLogin = ChronoUnit.DAYS.between(
                user.getLastLoginAt(), Instant.now()
            );
            if (daysSinceLogin <= 7) score += 0.33;
            else if (daysSinceLogin <= 30) score += 0.16;
        }

        // Swipe frequency (at least 10 swipes in last 7 days: 0.33)
        long swipeCount = swipeRepository.countByUserIdAndCreatedAtAfter(
            user.getId(),
            Instant.now().minus(7, ChronoUnit.DAYS)
        );
        if (swipeCount >= 10) score += 0.33;
        else if (swipeCount >= 5) score += 0.16;

        // Message response rate (0.34)
        double responseRate = calculateMessageResponseRate(user);
        score += responseRate * 0.34;

        return score;
    }

    /**
     * ML-based engagement probability
     * Uses trained model to predict likelihood of mutual engagement
     */
    private double calculateMLScore(User currentUser, User candidate) {
        try {
            // Prepare feature vector
            Map<String, Object> features = new HashMap<>();
            features.put("user_id", currentUser.getId());
            features.put("candidate_id", candidate.getId());
            features.put("age_diff", Math.abs(
                calculateAge(currentUser.getDateOfBirth()) - calculateAge(candidate.getDateOfBirth())
            ));
            features.put("distance", calculateDistance(
                currentUser.getLocation().getLatitude(), currentUser.getLocation().getLongitude(),
                candidate.getLocation().getLatitude(), candidate.getLocation().getLongitude()
            ));
            features.put("interest_match", calculateInterestSimilarity(currentUser, candidate));
            features.put("activity_level", calculateActivityScore(candidate));

            // Call ML model
            double prediction = modelService.predict(features);

            return Math.min(prediction, 1.0); // Normalize to [0, 1]
        } catch (Exception e) {
            log.warn("ML scoring failed, using default: {}", e.getMessage());
            return 0.5; // Default neutral score
        }
    }

    // Helper methods
    private int calculateAge(LocalDate dateOfBirth) {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula
        double earthRadiusKm = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
```

### 4.3 Feed Caching and Pagination

**Feed Generation Service (Already shown in Caching Strategy section)**

The feed generation uses Redis caching with 24-hour TTL for top 100 candidates, and pagination is applied on top of the cached feed.

---

## 5. Real-Time Messaging

### 5.1 WebSocket Architecture with Vaadin @Push

**Vaadin Configuration:**

```java
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.spring.annotation.SpringComponent;
import org.springframework.stereotype.Component;

@SpringComponent
@Push // Enable WebSocket push
@PWA(name = "POC Dating", shortName = "Dating")
public class AppShell implements AppShellConfigurator {
    // Vaadin uses automatic WebSocket connection
}
```

**Chat View Component:**

```java
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "chat/:matchId", layout = MainLayout.class)
@PageTitle("Chat | POC Dating")
@Slf4j
public class ChatView extends VerticalLayout {

    private final ChatService chatService;
    private final MessageWebSocketHandler webSocketHandler;
    private UUID matchId;
    private UUID currentUserId;

    private Div messagesContainer;
    private TextArea messageInput;
    private Button sendButton;

    public ChatView(@Autowired ChatService chatService,
                   @Autowired MessageWebSocketHandler webSocketHandler) {
        this.chatService = chatService;
        this.webSocketHandler = webSocketHandler;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        initializeUI();
        attachComponentAsDefault();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // Extract match ID from route
        matchId = UUID.fromString(getRouteParameters().get("matchId").orElse(""));
        currentUserId = getCurrentUserId();

        log.info("User {} opened chat for match {}", currentUserId, matchId);

        // Load message history
        loadMessageHistory();

        // Register for real-time updates via WebSocket
        registerForRealTimeUpdates();
    }

    private void initializeUI() {
        // Message display area
        messagesContainer = new Div();
        messagesContainer.setWidthFull();
        messagesContainer.setHeight("400px");
        messagesContainer.getStyle().set("overflow-y", "auto");
        messagesContainer.getStyle().set("border", "1px solid #ccc");
        messagesContainer.getStyle().set("padding", "10px");

        // Message input
        messageInput = new TextArea();
        messageInput.setWidthFull();
        messageInput.setHeight("100px");
        messageInput.setPlaceholder("Type a message...");

        // Send button
        sendButton = new Button("Send", e -> sendMessage());
        sendButton.addClickShortcut(Key.ENTER);

        HorizontalLayout inputLayout = new HorizontalLayout(messageInput, sendButton);
        inputLayout.setWidthFull();

        add(messagesContainer, inputLayout);
    }

    private void loadMessageHistory() {
        try {
            List<MessageDto> messages = chatService.getMessages(matchId, 0, 50);
            messages.forEach(this::addMessageToUI);
        } catch (Exception e) {
            log.error("Error loading message history: {}", e.getMessage());
        }
    }

    private void sendMessage() {
        String content = messageInput.getValue().trim();
        if (content.isEmpty()) return;

        try {
            // Send to backend via REST
            MessageDto sentMessage = chatService.sendMessage(matchId, content);

            // Clear input
            messageInput.clear();

            // Update UI
            getUI().ifPresent(ui -> ui.access(() -> {
                addMessageToUI(sentMessage);
            }));

            log.info("Message sent: {}", sentMessage.getId());
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }

    /**
     * Register for real-time message updates via WebSocket
     */
    private void registerForRealTimeUpdates() {
        String sessionId = VaadinSession.getCurrent().getSession().getId();

        // Subscribe to updates for this match
        webSocketHandler.subscribeToMatch(matchId, currentUserId, message -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                addMessageToUI(message);
            }));
        });
    }

    private void addMessageToUI(MessageDto message) {
        Div messageDiv = new Div();
        messageDiv.setWidthFull();
        messageDiv.getStyle().set("padding", "8px");
        messageDiv.getStyle().set("margin-bottom", "8px");

        String senderName = message.getSenderId().equals(currentUserId) ? "You" : "Match";
        messageDiv.setText(String.format("[%s] %s: %s",
            message.getCreatedAt(), senderName, message.getContent()));

        messageDiv.getStyle().set("background-color",
            message.getSenderId().equals(currentUserId) ? "#e3f2fd" : "#f5f5f5");

        messagesContainer.add(messageDiv);
        messagesContainer.getElement().executeJs("this.scrollTop = this.scrollHeight");
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        // Unsubscribe from WebSocket updates
        webSocketHandler.unsubscribeFromMatch(matchId, currentUserId);
    }
}
```

### 5.2 WebSocket Message Handler

**Backend WebSocket Manager:**

```java
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageWebSocketHandler {

    private final ChatService chatService;
    private final MessagePublisher messagePublisher;

    // Map: matchId -> Set of sessions subscribed to that match
    private final Map<UUID, Set<WebSocketSession>> sessionsByMatch = new ConcurrentHashMap<>();

    // Map: sessionId -> userId (for identifying message sender)
    private final Map<String, UUID> usersBySession = new ConcurrentHashMap<>();

    /**
     * Register a WebSocket session for a match
     */
    public void registerSession(WebSocketSession session, UUID matchId, UUID userId) {
        log.info("Registering WebSocket session: user={}, match={}", userId, matchId);

        sessionsByMatch.computeIfAbsent(matchId, k -> ConcurrentHashMap.newKeySet())
            .add(session);

        usersBySession.put(session.getId(), userId);
    }

    /**
     * Unregister a WebSocket session
     */
    public void unregisterSession(WebSocketSession session, UUID matchId) {
        log.info("Unregistering WebSocket session: match={}", matchId);

        sessionsByMatch.computeIfPresent(matchId, (k, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });

        usersBySession.remove(session.getId());
    }

    /**
     * Broadcast a message to all users in a match (real-time delivery)
     */
    public void broadcastMessageToMatch(UUID matchId, MessageDto message) {
        log.info("Broadcasting message to match {}: {}", matchId, message.getId());

        Set<WebSocketSession> sessions = sessionsByMatch.get(matchId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("No active sessions for match {}", matchId);
            return;
        }

        String messageJson = convertToJson(message);
        TextMessage textMessage = new TextMessage(messageJson);

        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                    log.debug("Message sent to session: {}", session.getId());
                } else {
                    unregisterSession(session, matchId);
                }
            } catch (Exception e) {
                log.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
                try {
                    unregisterSession(session, matchId);
                } catch (Exception ex) {
                    log.error("Error unregistering session", ex);
                }
            }
        });
    }

    /**
     * Handle incoming message from WebSocket
     */
    public void handleIncomingMessage(WebSocketSession session, String messageJson) {
        try {
            UUID userId = usersBySession.get(session.getId());
            if (userId == null) {
                log.warn("Unknown user for session: {}", session.getId());
                return;
            }

            // Parse incoming message
            Map<String, String> payload = parseJson(messageJson);
            UUID matchId = UUID.fromString(payload.get("matchId"));
            String content = payload.get("content");

            // Save message to database via REST
            MessageDto savedMessage = chatService.sendMessage(matchId, content);

            // Broadcast to all connected clients
            broadcastMessageToMatch(matchId, savedMessage);

            // Publish event for notifications
            messagePublisher.publishMessageSent(
                new MessageSentEvent(
                    savedMessage.getId(),
                    matchId,
                    userId,
                    savedMessage.getReceiverId(),
                    content
                )
            );

        } catch (Exception e) {
            log.error("Error handling incoming message: {}", e.getMessage());
        }
    }

    private String convertToJson(MessageDto message) {
        // Convert to JSON using ObjectMapper
        return "";
    }

    private Map<String, String> parseJson(String json) {
        // Parse JSON
        return new HashMap<>();
    }
}
```

**WebSocket Controller:**

```java
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {

    private final MessageWebSocketHandler messageHandler;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());

        // Extract match ID and user ID from query parameters or handshake attributes
        UUID matchId = (UUID) session.getAttributes().get("matchId");
        UUID userId = (UUID) session.getAttributes().get("userId");

        messageHandler.registerSession(session, matchId, userId);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = (String) message.getPayload();
        messageHandler.handleIncomingMessage(session, payload);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: {}", exception.getMessage());

        UUID matchId = (UUID) session.getAttributes().get("matchId");
        if (matchId != null) {
            messageHandler.unregisterSession(session, matchId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("WebSocket connection closed: {}, reason: {}", session.getId(), closeStatus);

        UUID matchId = (UUID) session.getAttributes().get("matchId");
        if (matchId != null) {
            messageHandler.unregisterSession(session, matchId);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
```

### 5.3 Message Status Tracking

**Message Status Flow:**

```
SENT (message saved to DB)
  ↓
DELIVERED (client confirms receipt via REST)
  ↓
READ (user opens message in chat)
```

**Service Implementation:**

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final MessageRepository messageRepository;
    private final MessagePublisher messagePublisher;
    private final ChatRoomRepository chatRoomRepository;

    /**
     * Save message as SENT
     */
    @Transactional
    public MessageDto sendMessage(UUID matchId, String content) {
        log.info("Sending message to match: {}", matchId);

        Message message = Message.builder()
            .matchId(matchId)
            .senderId(getCurrentUserId())
            .content(content)
            .status(MessageStatus.SENT)
            .createdAt(Instant.now())
            .build();

        Message saved = messageRepository.save(message);

        // Publish event
        messagePublisher.publishMessageSent(
            new MessageSentEvent(saved.getId(), matchId, saved.getSenderId(),
                                saved.getReceiverId(), content)
        );

        return mapToDto(saved);
    }

    /**
     * Mark message as DELIVERED
     */
    @Transactional
    public void markAsDelivered(UUID messageId) {
        log.info("Marking message as delivered: {}", messageId);

        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new MessageNotFoundException(messageId));

        message.setStatus(MessageStatus.DELIVERED);
        message.setDeliveredAt(Instant.now());
        messageRepository.save(message);
    }

    /**
     * Mark message as READ
     */
    @Transactional
    public void markAsRead(UUID messageId) {
        log.info("Marking message as read: {}", messageId);

        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new MessageNotFoundException(messageId));

        message.setStatus(MessageStatus.READ);
        message.setReadAt(Instant.now());
        messageRepository.save(message);

        // Publish READ event
        messagePublisher.publishMessageRead(
            new MessageReadEvent(messageId, message.getMatchId(), message.getSenderId())
        );
    }

    /**
     * Get unread message count for user
     */
    public long getUnreadMessageCount(UUID userId) {
        return messageRepository.countUnreadByReceiverId(userId);
    }

    /**
     * Get messages for a match (paginated)
     */
    public Page<MessageDto> getMessages(UUID matchId, int page, int pageSize) {
        return messageRepository.findByMatchIdOrderByCreatedAtDesc(
            matchId,
            PageRequest.of(page, pageSize)
        ).map(this::mapToDto);
    }
}
```

---

## 6. Database Schema Details

### 6.1 Table Structures

**Users Table:**

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    bio TEXT,
    date_of_birth DATE,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    verification_status VARCHAR(50) DEFAULT 'UNVERIFIED',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP,

    INDEX idx_email (email),
    INDEX idx_username (username),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

CREATE TABLE user_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    min_age INT DEFAULT 18,
    max_age INT DEFAULT 65,
    max_distance_km INT DEFAULT 50,
    interested_in_gender VARCHAR(50),
    latitude FLOAT NOT NULL,
    longitude FLOAT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    INDEX idx_user_id (user_id),
    INDEX idx_location (latitude, longitude)
);
```

**Swipes Table (High Volume):**

```sql
CREATE TABLE swipes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    swipe_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    INDEX idx_user_id (user_id),
    INDEX idx_target_user_id (target_user_id),
    INDEX idx_user_created (user_id, created_at),
    UNIQUE KEY uk_user_target (user_id, target_user_id)
);
```

**Matches Table:**

```sql
CREATE TABLE matches (
    id UUID PRIMARY KEY,
    user1_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user2_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    matched_at TIMESTAMP NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,

    INDEX idx_user1_id (user1_id),
    INDEX idx_user2_id (user2_id),
    INDEX idx_matched_at (matched_at),
    UNIQUE KEY uk_users (LEAST(user1_id, user2_id), GREATEST(user1_id, user2_id))
);
```

**Messages Table:**

```sql
CREATE TABLE messages (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'SENT',
    created_at TIMESTAMP NOT NULL,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,

    INDEX idx_match_id (match_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_match_created (match_id, created_at),
    INDEX idx_status (status)
);
```

### 6.2 Indexing Strategies

**Composite Indexes for Common Queries:**

```sql
-- Find swipes for user on a date range
ALTER TABLE swipes ADD INDEX idx_user_date (user_id, created_at DESC);

-- Find unread messages for user
ALTER TABLE messages ADD INDEX idx_receiver_status (receiver_id, status, created_at DESC);

-- Find active matches for user
ALTER TABLE matches ADD INDEX idx_user_status (user1_id, status);
ALTER TABLE matches ADD INDEX idx_user_status_2 (user2_id, status);

-- Find candidates by location
ALTER TABLE user_preferences ADD SPATIAL INDEX spatial_idx_location (POINT(latitude, longitude));
```

**Partition Strategy for High-Volume Tables:**

```sql
-- Partition swipes by month for better query performance
ALTER TABLE swipes
PARTITION BY RANGE (YEAR_MONTH(created_at)) (
    PARTITION p202501 VALUES LESS THAN (202502),
    PARTITION p202502 VALUES LESS THAN (202503),
    -- ... more partitions
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

---

## Summary

This deep-dive document covers all major architectural patterns in the POC Dating application with comprehensive code examples and best practices. Key takeaways:

1. **Event-Driven Architecture** enables loose coupling and scalability
2. **Caching Strategy** significantly improves performance for frequently accessed data
3. **JWT Authentication** provides secure, stateless authentication
4. **Feed Generation** combines filtering, scoring, and caching for optimal performance
5. **Real-Time Messaging** uses WebSockets for instant communication
6. **Database Schema** is optimized with proper indexes and partitioning

All patterns follow Spring Boot best practices and are designed for a production-ready application.
