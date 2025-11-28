# Feign Client Fixes - Implementation Guide

## File-by-File Remediation Guide

---

## 1. Vaadin UI MatchServiceClient
**File:** `/backend/vaadin-ui-service/src/main/java/com/dating/ui/client/MatchServiceClient.java`

### BEFORE (Broken)
```java
@FeignClient(name = "match-service", url = "${services.match-service.url}")
public interface MatchServiceClient {

    @GetMapping("/api/matches/next-profile")
    User getNextProfile(@RequestHeader("Authorization") String token);

    @PostMapping("/api/matches/swipe")
    SwipeResponse recordSwipe(@RequestBody SwipeRequest request,
                              @RequestHeader("Authorization") String token);

    @GetMapping("/api/matches/my-matches")
    List<Match> getMyMatches(@RequestHeader("Authorization") String token);

    @GetMapping("/api/matches/{matchId}")
    Match getMatch(@PathVariable String matchId, @RequestHeader("Authorization") String token);
}
```

### AFTER (Fixed)
```java
@FeignClient(
    name = "match-service",
    url = "${services.match-service.url}",
    configuration = FeignClientConfig.class
)
public interface MatchServiceClient {

    // REMOVED: getNextProfile() - endpoint doesn't exist in Match Service
    // If this functionality is needed, implement it as a new endpoint in Match Service

    @PostMapping("/api/matches/swipes")  // Changed: /swipe → /swipes (plural)
    SwipeResponse recordSwipe(@RequestBody SwipeRequest request,
                              @RequestHeader("Authorization") String token);

    @GetMapping("/api/matches")  // Changed: /my-matches → root path
    List<Match> getMyMatches(@RequestHeader("Authorization") String token,
                             @RequestParam(defaultValue = "20") int limit,
                             @RequestParam(defaultValue = "0") int offset);

    @GetMapping("/api/matches/{matchId}")
    Match getMatch(@PathVariable String matchId, @RequestHeader("Authorization") String token);
}
```

**Changes Summary:**
- [x] Remove `getNextProfile()` method (endpoint doesn't exist)
- [x] Fix `recordSwipe()` path: `/swipe` → `/swipes`
- [x] Fix `getMyMatches()` path: `/my-matches` → root path
- [x] Add pagination parameters to `getMyMatches()`
- [x] Add FeignClientConfig reference

---

## 2. Vaadin UI ChatServiceClient
**File:** `/backend/vaadin-ui-service/src/main/java/com/dating/ui/client/ChatServiceClient.java`

### BEFORE (Broken)
```java
@FeignClient(name = "chat-service", url = "${services.chat-service.url}")
public interface ChatServiceClient {

    @GetMapping("/api/chat/conversations")
    List<Conversation> getConversations(@RequestHeader("Authorization") String token);

    @GetMapping("/api/chat/conversations/{conversationId}/messages")
    List<Message> getMessages(@PathVariable String conversationId,
                              @RequestHeader("Authorization") String token);

    @PostMapping("/api/chat/conversations/{conversationId}/messages")
    Message sendMessage(@PathVariable String conversationId,
                       @RequestBody SendMessageRequest request,
                       @RequestHeader("Authorization") String token);

    @GetMapping("/api/chat/conversations/{conversationId}")
    Conversation getConversation(@PathVariable String conversationId,
                                 @RequestHeader("Authorization") String token);
}
```

### AFTER (Fixed)
```java
@FeignClient(
    name = "chat-service",
    url = "${services.chat-service.url}",
    configuration = FeignClientConfig.class
)
public interface ChatServiceClient {

    @GetMapping("/api/chat/conversations")
    List<Conversation> getConversations(@RequestHeader("Authorization") String token);

    @GetMapping("/api/chat/conversations/{conversationId}/messages")
    List<Message> getMessages(@PathVariable String conversationId,
                              @RequestHeader("Authorization") String token);

    @PostMapping("/api/chat/messages")  // Changed: path from conversations/{id}/messages
    Message sendMessage(@RequestBody SendMessageRequest request,
                        @RequestHeader("Authorization") String token);

    // REMOVED: getConversation() - endpoint doesn't exist
    // ConversationController doesn't expose individual conversation retrieval
    // Use getMessages() to access conversation details
}
```

**Changes Summary:**
- [x] Fix `sendMessage()` path: `/conversations/{id}/messages` → `/messages`
- [x] Remove conversationId from sendMessage path (it's in SendMessageRequest)
- [x] Remove `getConversation()` method (endpoint doesn't exist)
- [x] Add FeignClientConfig reference

---

## 3. Vaadin UI RecommendationServiceClient
**File:** `/backend/vaadin-ui-service/src/main/java/com/dating/ui/client/RecommendationServiceClient.java`

### BEFORE (Broken)
```java
@FeignClient(name = "recommendation-service", url = "${services.recommendation-service.url}")
public interface RecommendationServiceClient {

    @GetMapping("/api/recommendations")
    List<User> getRecommendations(@RequestHeader("Authorization") String token,
                                   @RequestParam(defaultValue = "20") int limit);

    @GetMapping("/api/recommendations/refresh")
    List<User> refreshRecommendations(@RequestHeader("Authorization") String token);
}
```

### AFTER (Fixed)
```java
@FeignClient(
    name = "recommendation-service",
    url = "${services.recommendation-service.url}",
    configuration = FeignClientConfig.class
)
public interface RecommendationServiceClient {

    @GetMapping("/api/recommendations/{userId}")  // Added: userId path parameter
    List<User> getRecommendations(@PathVariable String userId,
                                   @RequestHeader("Authorization") String token,
                                   @RequestParam(defaultValue = "20") int limit);

    @PostMapping("/api/recommendations/{userId}/refresh")  // Changed: GET → POST, added userId
    void refreshRecommendations(@PathVariable String userId,
                                @RequestHeader("Authorization") String token);
}
```

**Changes Summary:**
- [x] Add `{userId}` path parameter to `getRecommendations()`
- [x] Fix `refreshRecommendations()` HTTP method: GET → POST
- [x] Add `{userId}` path parameter to `refreshRecommendations()`
- [x] Change return type from `List<User>` to `void`
- [x] Add FeignClientConfig reference

**Note:** You'll need to extract userId from the Authorization token in the service layer before calling these methods.

---

## 4. Recommendation Service UserServiceClient
**File:** `/backend/recommendation-service/src/main/java/com/dating/recommendation/client/UserServiceClient.java`

### BEFORE (Broken)
```java
@FeignClient(
    name = "user-service",
    url = "${services.user-service.url}",
    configuration = FeignClientConfig.class
)
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}")
    UserProfileDto getUserById(@PathVariable("userId") UUID userId);

    @GetMapping("/api/users/{userId}/preferences")
    UserProfileDto getUserPreferences(@PathVariable("userId") UUID userId);  // Wrong return type!

    @GetMapping("/api/users/{userId}/candidates")
    List<UserProfileDto> getCandidates(
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "limit", defaultValue = "100") int limit);  // Missing params!

    @PostMapping("/api/users/batch")
    List<UserProfileDto> getUsersByIds(@RequestBody List<UUID> userIds);  // Wrong return type!
}
```

### AFTER (Fixed)
```java
@FeignClient(
    name = "user-service",
    url = "${services.user-service.url}",
    configuration = FeignClientConfig.class
)
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}")
    UserProfileDto getUserById(@PathVariable("userId") UUID userId);

    @GetMapping("/api/users/{userId}/preferences")
    PreferencesResponse getPreferences(@PathVariable("userId") UUID userId);  // Fixed: return type

    @GetMapping("/api/users/{userId}/candidates")
    List<UserProfileDto> getCandidates(
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "minAge", defaultValue = "18") int minAge,  // Added
            @RequestParam(value = "maxAge", defaultValue = "100") int maxAge,  // Added
            @RequestParam(value = "maxDistance", defaultValue = "100") int maxDistance,  // Added
            @RequestParam(value = "limit", defaultValue = "100") int limit,
            @RequestParam(value = "excludeIds", required = false) List<UUID> excludeIds);  // Added

    @PostMapping("/api/users/batch")
    List<UserResponse> getUsersByIds(@RequestBody List<UUID> userIds);  // Fixed: return type
}
```

**Changes Summary:**
- [x] Fix `getPreferences()` return type: `UserProfileDto` → `PreferencesResponse`
- [x] Add missing parameters to `getCandidates()`: minAge, maxAge, maxDistance, excludeIds
- [x] Fix `getUsersByIds()` return type: `List<UserProfileDto>` → `List<UserResponse>`

**Required Imports:**
```java
import com.dating.recommendation.dto.PreferencesResponse;
import com.dating.recommendation.dto.UserResponse;
```

---

## 5. Vaadin UI UserServiceClient
**File:** `/backend/vaadin-ui-service/src/main/java/com/dating/ui/client/UserServiceClient.java`

### ISSUE: AuthResponse DTO Mismatch

The Vaadin UI expects a different AuthResponse structure than what User Service returns.

**User Service returns:**
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com",
  "username": "john_doe",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 900
}
```

**Vaadin UI expects:**
```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "user": {
    "id": "...",
    "email": "...",
    "username": "...",
    "firstName": "...",
    // ... other user fields
  }
}
```

### FIX OPTIONS:

#### Option A: Update Vaadin UI AuthResponse to match User Service (RECOMMENDED)
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private UUID userId;
    private String email;
    private String username;
    private String token;  // Changed from accessToken
    private String refreshToken;
    private Long expiresIn;
    
    // Convenience method to get token as "accessToken"
    public String getAccessToken() {
        return token;
    }
}
```

#### Option B: Create a mapper in User Service
Create a transformation layer that converts to Vaadin's expected format.

**Recommendation:** Use Option A - Update Vaadin UI DTOs to match the actual API response structure.

---

## 6. Create Vaadin UI FeignClientConfig
**File:** `/backend/vaadin-ui-service/src/main/java/com/dating/ui/config/FeignClientConfig.java` (NEW FILE)

```java
package com.dating.ui.config;

import feign.Logger;
import feign.Request;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign client configuration for Vaadin UI service.
 * Provides error handling, timeouts, and logging for inter-service calls.
 */
@Configuration
@Slf4j
public class FeignClientConfig {

    /**
     * Feign logger level for debugging.
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Request options with connection and read timeouts.
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                5, TimeUnit.SECONDS,      // connect timeout
                10, TimeUnit.SECONDS,     // read timeout
                true                      // follow redirects
        );
    }

    /**
     * Custom error decoder for Feign client errors.
     * Translates HTTP errors to meaningful exceptions.
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            log.error("Feign client error [{}]: {} - {}",
                    methodKey, response.status(), response.reason());

            return switch (response.status()) {
                case 400 -> new IllegalArgumentException("Bad request: " + response.reason());
                case 401, 403 -> new SecurityException("Unauthorized: " + response.reason());
                case 404 -> new ResourceNotFoundException("Resource not found");
                case 503, 504 -> new ServiceUnavailableException("Service unavailable");
                default -> new RuntimeException("Error calling service: " + response.reason());
            };
        };
    }

    // Custom exceptions
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    public static class SecurityException extends RuntimeException {
        public SecurityException(String message) {
            super(message);
        }
    }

    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message) {
            super(message);
        }
    }
}
```

Then update all Vaadin UI Feign client declarations:
```java
@FeignClient(
    name = "user-service",
    url = "${services.user-service.url}",
    configuration = FeignClientConfig.class  // Add this
)
public interface UserServiceClient { ... }
```

---

## 7. Fix Recommendation Service FeignClientConfig
**File:** `/backend/recommendation-service/src/main/java/com/dating/recommendation/config/FeignClientConfig.java`

### ADD RequestInterceptor for Header Forwarding

```java
package com.dating.recommendation.config;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * Feign client configuration for inter-service communication.
 */
@Configuration
@Slf4j
public class FeignClientConfig {

    /**
     * Request interceptor to forward headers to other services.
     * Forwards the X-User-Id and X-Correlation-Id headers for context propagation.
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                // Forward X-User-Id for authorization context
                String userId = attributes.getRequest().getHeader("X-User-Id");
                if (userId != null) {
                    requestTemplate.header("X-User-Id", userId);
                }

                // Forward correlation ID for distributed tracing
                String correlationId = attributes.getRequest().getHeader("X-Correlation-Id");
                if (correlationId != null) {
                    requestTemplate.header("X-Correlation-Id", correlationId);
                }
            }
        };
    }

    /**
     * Feign logger level.
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Request options with timeouts.
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                5, TimeUnit.SECONDS,    // connect timeout
                10, TimeUnit.SECONDS,   // read timeout
                true                     // follow redirects
        );
    }

    /**
     * Custom error decoder for Feign client errors.
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            log.error("Feign client error: {} - {}", response.status(), response.reason());

            return switch (response.status()) {
                case 404 -> new RuntimeException("User not found in user-service");
                case 503 -> new RuntimeException("User service unavailable");
                default -> new RuntimeException("Error calling user-service: " + response.reason());
            };
        };
    }
}
```

**Change:** Added the `requestInterceptor()` bean (copied from Match Service implementation)

---

## Summary of Changes by Priority

### CRITICAL (Must fix today)
- [ ] Fix Vaadin MatchServiceClient (3 endpoint issues)
- [ ] Fix Vaadin ChatServiceClient (1 critical path issue)

### HIGH (This week)
- [ ] Fix Vaadin RecommendationServiceClient (2 issues)
- [ ] Fix Recommendation UserServiceClient (3 type/parameter issues)
- [ ] Update Vaadin AuthResponse DTO structure

### MEDIUM (Next week)
- [ ] Create Vaadin FeignClientConfig
- [ ] Update Recommendation FeignClientConfig with RequestInterceptor
- [ ] Add integration tests for Feign clients

---

**Total files to modify: 7**
**New files to create: 1**
**Estimated effort: 2-3 hours**

