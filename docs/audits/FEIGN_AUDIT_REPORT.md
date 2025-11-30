# Comprehensive Feign Client Audit Report
**Project:** POC Dating Application  
**Date:** 2025-11-18  
**Scope:** All Feign clients across Match Service, Chat Service, Recommendation Service, and Vaadin UI

---

## Executive Summary

**Total Issues Found: 13**
- CRITICAL: 4 (Wrong endpoints, missing endpoints, wrong HTTP methods)
- HIGH: 6 (Wrong paths, missing parameters, type mismatches)
- MEDIUM: 3 (Missing configurations, missing clients)

**Impact:** Several Feign clients call non-existent endpoints and have type mismatches that will cause runtime errors. The Vaadin UI service lacks proper error handling and header forwarding configuration.

---

## CRITICAL ISSUES (Will cause runtime failures)

### 1. Vaadin UI MatchServiceClient - Non-existent Endpoint
**Severity:** CRITICAL  
**File:** `/backend/vaadin-ui-service/src/main/java/com/dating/ui/client/MatchServiceClient.java` (Line 19)

**Issue:**
```java
@GetMapping("/api/matches/next-profile")
User getNextProfile(@RequestHeader("Authorization") String token);
```

**Problem:** This endpoint does NOT exist in Match Service. MatchController only has:
- `GET /api/matches` (get all matches)
- `GET /api/matches/{matchId}` (get specific match)
- `DELETE /api/matches/{matchId}` (unmatch)

**Impact:** Calls to `getNextProfile()` will return 404 Not Found

**Recommendation:** 
- Remove this method if not needed
- OR implement a new endpoint in Match Service if this is intended functionality

---

### 2. Vaadin UI MatchServiceClient - Wrong HTTP Method (Swipe)
**Severity:** CRITICAL  
**File:** `/backend/vaadin-ui-service/src/main/java/com/dating/ui/client/MatchServiceClient.java` (Line 22-24)

**Issue:**
```java
@PostMapping("/api/matches/swipe")
SwipeResponse recordSwipe(@RequestBody SwipeRequest request,
                          @RequestHeader("Authorization") String token);
```

**Expected Endpoint:**
```
POST /api/matches/swipes (plural, not singular)
```

**Problem:** The actual endpoint in SwipeController is `/api/matches/swipes` (plural), not `/api/matches/swipe` (singular)

**Impact:** Swipe operations will fail with 404 Not Found

**Fix:** Change to:
```java
@PostMapping("/api/matches/swipes")
SwipeResponse recordSwipe(@RequestBody SwipeRequest request,
                          @RequestHeader("Authorization") String token);
```

---

### 3. Vaadin UI MatchServiceClient - Wrong Endpoint (Get Matches)
**Severity:** CRITICAL  
**File:** `/backend/vaadin-ui-service/src/main/java/com/dating/ui/client/MatchServiceClient.java` (Line 26-27)

**Issue:**
```java
@GetMapping("/api/matches/my-matches")
List<Match> getMyMatches(@RequestHeader("Authorization") String token);
```

**Expected Endpoint:**
```
GET /api/matches (with pagination query params)
```

**Problem:** The actual endpoint is just `/api/matches` with pagination parameters, not `/api/matches/my-matches`

**Impact:** Retrieving user's matches will fail with 404

**Fix:** Change to:
```java
@GetMapping("/api/matches")
List<Match> getMyMatches(@RequestHeader("Authorization") String token,
                         @RequestParam(defaultValue = "20") int limit,
                         @RequestParam(defaultValue = "0") int offset);
```

---

### 4. Vaadin UI ChatServiceClient - Wrong Endpoint Path
**Severity:** CRITICAL  
**File:** `/backend/vaadin-ui-service/src/main/java/com/dating/ui/client/ChatServiceClient.java` (Line 26-29)

**Issue:**
```java
@PostMapping("/api/chat/conversations/{conversationId}/messages")
Message sendMessage(@PathVariable String conversationId,
                   @RequestBody SendMessageRequest request,
                   @RequestHeader("Authorization") String token);
```

**Expected Endpoint:**
```
POST /api/chat/messages
```

**Problem:** 
- MessageController is at `/api/chat/messages` not `/api/chat/conversations/{conversationId}/messages`
- The message endpoint does NOT include conversationId in the path; it's in the request body

**Impact:** Sending messages will fail with 404

**Fix:** Change to:
```java
@PostMapping("/api/chat/messages")
Message sendMessage(@RequestBody SendMessageRequest request,
                    @RequestHeader("Authorization") String token);
```

---

## HIGH ISSUES (Wrong behavior, type mismatches)

### 5. Vaadin UI ChatServiceClient - Non-existent Endpoint
**Severity:** HIGH  
**File:** `/backend/vaadin-ui-service/src/main/java/com/dating/ui/client/ChatServiceClient.java` (Line 31-33)

**Issue:**
```java
@GetMapping("/api/chat/conversations/{conversationId}")
Conversation getConversation(@PathVariable String conversationId,
                             @RequestHeader("Authorization") String token);
```

**Problem:** This endpoint does NOT exist in Chat Service. ConversationController only has:
- `GET /api/chat/conversations` (get all)
- `GET /api/chat/conversations/{conversationId}/messages` (get messages)
- `POST /api/chat/conversations/{conversationId}/read` (mark read)

**Impact:** Cannot retrieve specific conversation details

**Recommendation:** Remove this method or implement the endpoint if needed

---

### 6. Vaadin UI RecommendationServiceClient - Wrong Endpoint (Get Recommendations)
**Severity:** HIGH  
**File:** `/backend/vaadin-ui-service/src/main/java/com/dating/ui/client/RecommendationServiceClient.java` (Line 18-20)

**Issue:**
```java
@GetMapping("/api/recommendations")
List<User> getRecommendations(@RequestHeader("Authorization") String token,
                               @RequestParam(defaultValue = "20") int limit);
```

**Expected Endpoint:**
```
GET /api/recommendations/{userId}
```

**Problem:** RecommendationController expects `userId` as a path parameter, not a query parameter. The userId should come from the Authorization token context.

**Impact:** Cannot fetch recommendations; API expects userId in path

**Fix:** Need to extract userId from token and add to path:
```java
@GetMapping("/api/recommendations/{userId}")
List<User> getRecommendations(@PathVariable String userId,
                               @RequestHeader("Authorization") String token,
                               @RequestParam(defaultValue = "20") int limit);
```

---

### 7. Vaadin UI RecommendationServiceClient - Wrong Endpoint (Refresh)
**Severity:** HIGH  
**File:** `/backend/vaadin-ui-service/src/main/java/com/dating/ui/client/RecommendationServiceClient.java` (Line 22-23)

**Issue:**
```java
@GetMapping("/api/recommendations/refresh")
List<User> refreshRecommendations(@RequestHeader("Authorization") String token);
```

**Expected Endpoint:**
```
POST /api/recommendations/{userId}/refresh
```

**Problems:**
1. Should be POST, not GET
2. Missing `{userId}` path parameter
3. Should return `Void` (204 No Content), not a list

**Impact:** Refresh operation fails; HTTP method mismatch

**Fix:** Change to:
```java
@PostMapping("/api/recommendations/{userId}/refresh")
void refreshRecommendations(@PathVariable String userId,
                            @RequestHeader("Authorization") String token);
```

---

### 8. Recommendation Service UserServiceClient - Wrong Return Type
**Severity:** HIGH  
**File:** `/backend/recommendation-service/src/main/java/com/dating/recommendation/client/UserServiceClient.java` (Line 41-42)

**Issue:**
```java
@GetMapping("/api/users/{userId}/preferences")
UserProfileDto getUserPreferences(@PathVariable("userId") UUID userId);
```

**Problem:** The endpoint returns `PreferencesResponse`, not `UserProfileDto`. This is a different DTO structure entirely.

**Impact:** Deserialization will fail or return incomplete data

**Expected Type:**
```java
PreferencesResponse getPreferences(@PathVariable("userId") UUID userId);
```

---

### 9. Recommendation Service UserServiceClient - Missing Parameter in Endpoint
**Severity:** HIGH  
**File:** `/backend/recommendation-service/src/main/java/com/dating/recommendation/client/UserServiceClient.java` (Line 51-54)

**Issue:**
```java
@GetMapping("/api/users/{userId}/candidates")
List<UserProfileDto> getCandidates(
        @PathVariable("userId") UUID userId,
        @RequestParam(value = "limit", defaultValue = "100") int limit);
```

**Problem:** The actual endpoint in UserController has additional required parameters:
```java
@GetMapping("/{userId}/candidates")
public ResponseEntity<List<UserResponse>> getCandidates(
        @PathVariable UUID userId,
        @RequestParam(defaultValue = "18") int minAge,
        @RequestParam(defaultValue = "100") int maxAge,
        @RequestParam(defaultValue = "100") int maxDistance,
        @RequestParam(required = false) List<UUID> excludeIds)
```

**Impact:** Missing filtering parameters; results won't match expected behavior

**Fix:** Add missing parameters:
```java
@GetMapping("/api/users/{userId}/candidates")
List<UserProfileDto> getCandidates(
        @PathVariable("userId") UUID userId,
        @RequestParam(value = "minAge", defaultValue = "18") int minAge,
        @RequestParam(value = "maxAge", defaultValue = "100") int maxAge,
        @RequestParam(value = "maxDistance", defaultValue = "100") int maxDistance,
        @RequestParam(value = "limit", defaultValue = "100") int limit,
        @RequestParam(value = "excludeIds", required = false) List<UUID> excludeIds);
```

---

### 10. Recommendation Service UserServiceClient - Wrong Return Type (Batch)
**Severity:** HIGH  
**File:** `/backend/recommendation-service/src/main/java/com/dating/recommendation/client/UserServiceClient.java` (Line 62-63)

**Issue:**
```java
@PostMapping("/api/users/batch")
List<UserProfileDto> getUsersByIds(@RequestBody List<UUID> userIds);
```

**Problem:** Returns `List<UserProfileDto>` but endpoint returns `List<UserResponse>`

**Impact:** Deserialization fails or returns wrong field types

**Fix:** Change return type:
```java
@PostMapping("/api/users/batch")
List<UserResponse> getUsersByIds(@RequestBody List<UUID> userIds);
```

---

### 11. Vaadin UI UserServiceClient - Wrong Return Type Structure
**Severity:** MEDIUM  
**File:** `/backend/vaadin-ui-service/src/main/java/com/dating/ui/client/UserServiceClient.java` (Line 17-31)

**Issue:**
```java
@PostMapping("/api/users/auth/login")
AuthResponse login(@RequestBody LoginRequest request);

@GetMapping("/api/users/{userId}")
User getUser(@PathVariable String userId, @RequestHeader("Authorization") String token);
```

**Problem:** 
1. AuthResponse structure in Vaadin UI expects `accessToken`, `refreshToken`, `user`
2. But User Service returns `token`, `refreshToken`, `userId`, `email`, `username`, `expiresIn`
3. Field mismatch: `accessToken` vs `token`, and missing nested `user` object

**Impact:** Login responses won't deserialize correctly; UI will fail to parse auth response

**Data Structure Mismatch:**
```
Vaadin expects:
{
  "accessToken": "...",
  "refreshToken": "...",
  "user": { UserObject }
}

But gets:
{
  "userId": "...",
  "email": "...",
  "username": "...",
  "token": "...",
  "refreshToken": "...",
  "expiresIn": ...
}
```

---

## MEDIUM ISSUES (Missing configurations, incomplete setups)

### 12. Vaadin UI Missing FeignClientConfig
**Severity:** MEDIUM  
**Issue:** Vaadin UI service has 4 Feign clients but no FeignClientConfig

**Current:** Uses default Feign configuration
```java
@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserServiceClient { ... }
```

**Problems:**
1. No error handling/custom ErrorDecoder
2. No request timeout configuration
3. No header forwarding (Authorization header must be manually passed to each method)
4. No logging configuration
5. No circuit breaker protection

**Impact:** Service-to-service calls lack resilience and proper error handling

**Recommendation:** Create `/backend/vaadin-ui-service/src/main/java/com/dating/ui/config/FeignClientConfig.java`:
```java
@Configuration
public class FeignClientConfig {
    
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            5, TimeUnit.SECONDS,      // connect timeout
            10, TimeUnit.SECONDS,     // read timeout
            true                      // follow redirects
        );
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            log.error("Feign error: {} - {}", response.status(), response.reason());
            return switch (response.status()) {
                case 404 -> new ResourceNotFoundException("Not found");
                case 401, 403 -> new UnauthorizedException("Unauthorized");
                case 503 -> new ServiceUnavailableException("Service unavailable");
                default -> new RuntimeException("Service error: " + response.reason());
            };
        };
    }
}
```

Then update all clients:
```java
@FeignClient(
    name = "user-service",
    url = "${services.user-service.url}",
    configuration = FeignClientConfig.class
)
```

---

### 13. Chat Service Missing Feign Clients
**Severity:** MEDIUM  
**Location:** `/backend/chat-service/`

**Issue:** Chat Service has no Feign clients but MessageController code suggests it might need one:

```java
// In MessageController (line 70-74):
if (!response.getSenderId().equals(userId)) {
    // User is not the sender, they must be the receiver (other participant in match)
    // For proper validation, we should check match participants from Match Service
    // TODO: Check if user is a participant in the match
    log.debug("User {} is not sender of message {}, assuming they are the receiver",
            userId, messageId);
}
```

**Recommendation:** Consider adding MatchServiceClient to validate match participants:
```java
@FeignClient(
    name = "match-service",
    url = "${services.match-service.url}",
    configuration = FeignClientConfig.class
)
public interface MatchServiceClient {
    @GetMapping("/api/matches/{matchId}")
    MatchResponse getMatch(@PathVariable UUID matchId);
}
```

---

## Configuration Analysis

### Match Service FeignClientConfig - GOOD
**File:** `/backend/match-service/src/main/java/com/dating/match/config/FeignClientConfig.java`

**Status:** ✓ Properly configured
- Has RequestInterceptor to forward X-User-Id header
- Has RequestInterceptor to forward X-Correlation-Id header
- Has Logger configuration
- Clients properly use this config

---

### Recommendation Service FeignClientConfig - ISSUES
**File:** `/backend/recommendation-service/src/main/java/com/dating/recommendation/config/FeignClientConfig.java`

**Issues:**
1. ✓ Has Logger configuration
2. ✓ Has Request.Options with timeouts
3. ✓ Has ErrorDecoder
4. ✗ **Missing RequestInterceptor** - doesn't forward X-User-Id or X-Correlation-Id headers

**Recommendation:** Add header forwarding:
```java
@Bean
public RequestInterceptor requestInterceptor() {
    return requestTemplate -> {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            String userId = attributes.getRequest().getHeader("X-User-Id");
            if (userId != null) {
                requestTemplate.header("X-User-Id", userId);
            }
            
            String correlationId = attributes.getRequest().getHeader("X-Correlation-Id");
            if (correlationId != null) {
                requestTemplate.header("X-Correlation-Id", correlationId);
            }
        }
    };
}
```

---

### Vaadin UI Application Configuration
**File:** `/backend/vaadin-ui-service/src/main/resources/application.yml`

**Status:** ✓ Service URLs properly configured
```yaml
services:
  user-service:
    url: http://${USER_SERVICE_HOST:localhost}:8081
  match-service:
    url: http://${MATCH_SERVICE_HOST:localhost}:8082
  chat-service:
    url: http://${CHAT_SERVICE_HOST:localhost}:8083
  recommendation-service:
    url: http://${RECOMMENDATION_SERVICE_HOST:localhost}:8084
```

---

## Summary Table

| Service | Client | Issue Type | Status |
|---------|--------|------------|--------|
| Vaadin UI | UserServiceClient | Type Mismatch (AuthResponse) | HIGH |
| Vaadin UI | MatchServiceClient | Wrong endpoint (next-profile) | CRITICAL |
| Vaadin UI | MatchServiceClient | Wrong path (swipe → swipes) | CRITICAL |
| Vaadin UI | MatchServiceClient | Wrong path (my-matches) | CRITICAL |
| Vaadin UI | ChatServiceClient | Wrong path (send message) | CRITICAL |
| Vaadin UI | ChatServiceClient | Non-existent endpoint (get conversation) | HIGH |
| Vaadin UI | RecommendationServiceClient | Wrong endpoint (get recommendations) | HIGH |
| Vaadin UI | RecommendationServiceClient | Wrong method (refresh should be POST) | HIGH |
| Match Service | UserServiceClient | No issues | OK |
| Match Service | RecommendationServiceClient | No issues | OK |
| Recommendation Service | UserServiceClient | Type mismatch (preferences) | HIGH |
| Recommendation Service | UserServiceClient | Missing parameters (candidates) | HIGH |
| Recommendation Service | UserServiceClient | Type mismatch (batch) | HIGH |
| Chat Service | (none) | Missing clients | MEDIUM |
| Vaadin UI | (none) | Missing FeignClientConfig | MEDIUM |
| Rec Service | FeignClientConfig | Missing header interceptor | MEDIUM |

---

## Recommendations Priority

### Immediate (Today)
1. Fix Vaadin UI MatchServiceClient paths (Critical - 3 issues)
2. Fix Vaadin UI ChatServiceClient send message endpoint (Critical)
3. Fix Vaadin UI RecommendationServiceClient endpoints (High - 2 issues)
4. Fix Recommendation Service UserServiceClient return types (High - 3 issues)

### Short-term (This week)
5. Create FeignClientConfig for Vaadin UI
6. Add header forwarding to Recommendation Service FeignClientConfig
7. Fix Vaadin UI AuthResponse deserialization

### Medium-term (Next iteration)
8. Consider adding MatchServiceClient to Chat Service
9. Add integration tests for Feign clients
10. Document Feign client contracts

---

## Testing Recommendations

1. **Integration Tests:** Add tests that verify Feign client methods call correct endpoints
2. **Contract Tests:** Use Spring Cloud Contract to verify client-server contracts
3. **Endpoint Verification:** Validate all Feign method paths against actual controller endpoints
4. **Type Safety:** Add JSON schema validation for responses

---

**End of Report**
