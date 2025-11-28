# POC Dating Application - Redis Caching Strategy Audit Report

## Executive Summary
The caching strategy is **mostly well-implemented** with good Redis configuration across services. However, there are **CRITICAL issues** with incomplete cache invalidation patterns and some TTL inconsistencies that could lead to stale data problems.

---

## 1. CRITICAL ISSUES

### 1.1 CRITICAL: Missing @CacheEvict on SwipeService.recordSwipe() Related Caches
**Location**: `/backend/match-service/src/main/java/com/dating/match/service/SwipeService.java:49`

**Issue**: 
- SwipeService.recordSwipe() only evicts FEED_CACHE for the current user
- When a mutual match is created, it should also evict MATCHES_CACHE and MATCH_DETAILS_CACHE for BOTH users
- Currently: Only `@CacheEvict(value = CacheConfig.FEED_CACHE, key = "#userId")`
- Expected: Should also evict MATCHES_CACHE for both users when a match is created

**Impact**: 
- New matches may not appear in user's match list immediately
- Users will see stale cached matches
- Could lead to inconsistent match state between users

**Recommendation**: 
```java
@Caching(evict = {
    @CacheEvict(value = CacheConfig.FEED_CACHE, key = "#userId"),
    @CacheEvict(value = CacheConfig.MATCHES_CACHE, allEntries = true),  // Invalidate all cached matches
    @CacheEvict(value = CacheConfig.MATCH_DETAILS_CACHE, allEntries = true)
})
```

---

### 1.2 CRITICAL: Missing Token Blacklist Implementation
**Location**: API Gateway JWT validation does NOT check token blacklist

**Issue**:
- CacheConstants.TOKEN_BLACKLIST_CACHE is defined but never used
- JwtValidator.java doesn't check if token is blacklisted
- TokenService.revokeAllUserTokens() works, but invalidated tokens are still accepted
- Security concern: Logout doesn't invalidate the token

**Current Flow**:
1. User logs out → TokenService.revokeAllUserTokens() is called
2. Refresh tokens are marked as revoked in DB
3. BUT access token is never added to blacklist
4. Access token can still be used until natural expiration (15 minutes)

**Impact**: Security vulnerability - Logged out users can continue using their tokens

**Recommendation**: 
Implement token blacklist check in JwtValidator:
```java
public boolean validateToken(String token) {
    // ... existing validation ...
    // Check if token is blacklisted
    if (isTokenBlacklisted(token)) {
        log.warn("Token is blacklisted");
        return false;
    }
    return true;
}
```

---

### 1.3 CRITICAL: Missing Cache Eviction on Message Operations
**Location**: `/backend/chat-service/src/main/java/com/dating/chat/service/MessageService.java`

**Issue**:
- sendMessage() evicts caches (good)
- markAllAsRead() evicts caches (good)
- BUT: getLastMessage() is not cached, yet it's called in buildConversationResponse()
- Result: Every conversation fetch triggers a database query for last message
- Performance impact: O(n) queries for n conversations

**Current**:
```java
@Transactional(readOnly = true)
public MessageResponse getLastMessage(UUID conversationId) {
    Message message = messageRepository.findLastMessageByMatchId(conversationId);
    return message != null ? messageMapper.toMessageResponse(message) : null;
}
```

**Recommendation**: Add caching:
```java
@Cacheable(value = CacheConfig.MESSAGES_CACHE, key = "'last-' + #conversationId")
@Transactional(readOnly = true)
public MessageResponse getLastMessage(UUID conversationId) { ... }
```
And evict in sendMessage() and markAllAsRead()

---

## 2. HIGH SEVERITY ISSUES

### 2.1 HIGH: TTL Inconsistency - Recommendation Service Uses Wrong Default TTL
**Location**: `/backend/recommendation-service/src/main/resources/application.yml:69`

**Issue**:
```yaml
cache:
  type: redis
  redis:
    time-to-live: ${REDIS_TTL_MINUTES:1440}m  # 24 hours = 1440 minutes
```

**Problem**:
- Default is 1440 minutes (24 hours) - correct
- But this is a fallback default for OTHER caches not explicitly configured
- RECOMMENDATIONS_CACHE is explicitly set to 24 hours in CacheConfig (correct)
- However, inconsistency: all other services use 30 minutes as default

**Verification Against CLAUDE.md Requirements**:
| Service | Expected TTL | Current TTL | Status |
|---------|-------------|-----------|--------|
| User Profiles | 1 hour | 1 hour ✓ | OK |
| User Preferences | 1 hour | 1 hour ✓ | OK |
| Match Feeds | 24 hours | 24 hours ✓ | OK |
| Matches | 24 hours | 30 minutes ✗ | **MISMATCH** |
| Recommendations | 24 hours | 24 hours ✓ | OK |
| Conversations | 1 hour | 30-60 min | OK |
| Sessions | 30 minutes | 30 minutes ✓ | OK |

**Recommendation**: Match Service should use 24 hours for MATCHES_CACHE:
```java
// In match-service/config/CacheConfig.java
private static final Duration MATCHES_CACHE_TTL = Duration.ofHours(24);  // Change from 30 minutes
```

---

### 2.2 HIGH: Match Service Feed Cache Key Issue
**Location**: `/backend/match-service/src/main/java/com/dating/match/service/FeedService.java:39`

**Issue**:
```java
@Cacheable(value = CacheConfig.FEED_CACHE, key = "#userId + '_' + #limit + '_' + #offset")
```

**Problem**:
- Cache key includes offset and limit
- If user requests (limit=10, offset=0) then (limit=10, offset=10), they get TWO separate cache entries
- Violates the 24-hour TTL intent (should cache the entire feed, not paginated chunks)
- Wastes Redis memory with duplicate data across pagination boundaries

**Impact**:
- Cache hit rate will be lower than expected
- Increased Redis memory consumption
- Multiple cache entries for same user

**Recommendation**: Cache by userId only, handle pagination in-memory:
```java
@Cacheable(value = CacheConfig.FEED_CACHE, key = "#userId")
public FeedResponse getFeed(UUID userId, int limit, int offset) {
    // Fetch full feed, cache it, then apply pagination
}
```

---

### 2.3 HIGH: Match Service MATCHES_CACHE Key Too Specific
**Location**: `/backend/match-service/src/main/java/com/dating/match/service/MatchService.java:46`

**Issue**:
```java
@Cacheable(value = CacheConfig.MATCHES_CACHE, key = "#userId + '_' + #limit + '_' + #offset")
```

**Same problem as Feed**: Pagination-specific cache keys waste memory and reduce hit rate.

**Recommendation**: 
```java
@Cacheable(value = CacheConfig.MATCHES_CACHE, key = "#userId")
public MatchListResponse getMatches(UUID userId, int limit, int offset) {
    // Fetch all matches for user, cache them, then paginate
}
```

---

### 2.4 HIGH: Chat ConversationService Cache Not Invalidated on New Match
**Location**: `/backend/chat-service/src/main/java/com/dating/chat/service/ConversationService.java:36`

**Issue**:
```java
@Cacheable(value = CacheConfig.CONVERSATIONS_CACHE, key = "#userId + '-' + #limit")
public List<ConversationResponse> getConversations(UUID userId, int limit) { ... }
```

**Problem**:
- ConversationService has no way to know when a new match is created
- When two users match, their conversation list should update
- But there's NO cache invalidation when a match is created
- Result: New conversation won't appear in cached list until TTL expires

**Impact**: Users won't see new conversations for 30-60 minutes after a match

**Recommendation**:
- Match Service should publish event when match is created
- Chat Service should listen and invalidate conversation caches for both users
- OR: Use shorter TTL (5 minutes) for CONVERSATIONS_CACHE
- OR: Add "match created" listener to ConversationService

---

### 2.5 HIGH: Recommendation Cache Invalidation Not Triggered on Profile Update
**Location**: User Service publishes UserUpdatedEvent, but Recommendation Service doesn't listen

**Issue**:
- When user profile changes, their recommendations become stale
- RecommendationService has @CacheEvict but no listener for user updates
- Cache will stay stale for 24 hours

**Current Behavior**:
```java
// User Service publishes:
eventPublisher.publishUserUpdated(userId, "profile");

// Recommendation Service does NOT listen for this event
// Cache stays stale until TTL expires (24 hours)
```

**Impact**: Recommendations based on outdated profile data for up to 24 hours

**Recommendation**: 
Add RabbitMQ listener in RecommendationService:
```java
@RabbitListener(queues = "recommendation.user.updated.queue")
public void handleUserUpdated(UserUpdatedEvent event) {
    recommendationRepository.deleteByUserId(event.getUserId());
    // Or: recommendationCache.evict(event.getUserId());
}
```

---

## 3. MEDIUM SEVERITY ISSUES

### 3.1 MEDIUM: Vaadin Session TTL Too Short
**Location**: `/backend/vaadin-ui-service/src/main/resources/application.yml:13`

**Issue**:
```yaml
session:
  timeout: 30m  # Matches SESSION_TTL_SECONDS = 1800 (30 minutes)
```

**Problem**:
- 30 minutes is quite short for a dating app
- User might be browsing profiles, then step away
- Returns after 35 minutes → session expired, must re-login
- Could happen during normal usage

**Comparison**: Most web apps use 1-4 hours

**Recommendation**: Consider extending to 2-4 hours:
```yaml
session:
  timeout: 2h  # More reasonable for interactive app
```

Or make it configurable via environment variable.

---

### 3.2 MEDIUM: Inconsistent Cache Configuration Across Services
**Location**: Various CacheConfig.java files

**Issue**:
- Each service defines its own cache names and TTLs
- user-service/CacheConfig has: USERS_CACHE, USER_PREFERENCES_CACHE, USER_BY_EMAIL_CACHE
- But CacheConstants.java (common-library) also defines USERS_CACHE
- Duplication and inconsistency risks

**Current**:
- `CacheConstants.USERS_CACHE = "users"` (1 hour)
- `user-service/CacheConfig.USERS_CACHE = "users"` (1 hour) - duplicate
- `user-service/CacheConfig.USER_BY_EMAIL_CACHE = "users_by_email"` - not in CacheConstants

**Recommendation**: 
Consolidate all cache names and configurations in CacheConstants:
```java
// CacheConstants.java
public static final String USER_BY_EMAIL_CACHE = "users_by_email";

// Services should reference only CacheConstants, not local definitions
```

---

### 3.3 MEDIUM: Missing Redis Key Serialization Strategy Details
**Location**: CacheConfig.java files

**Issue**:
- Using GenericJackson2JsonRedisSerializer for values (good)
- Using StringRedisSerializer for keys (good)
- But: No clear documentation on why Jackson vs other serializers

**Concern**: 
- Jackson serialization might have issues with complex nested objects
- API Gateway uses StringRedisSerializer for both key and value (for rate limiting)
- Inconsistency between services

**Recommendation**: 
- Document serialization strategy in CLAUDE.md
- Consider using a consistent serializer across all services
- Add Jackson configuration for polymorphic types if needed

---

### 3.4 MEDIUM: No Cache Warming Strategy
**Location**: Throughout the codebase

**Issue**:
- Cache is purely lazy-loaded (cache-aside pattern)
- First user request after app startup = cache miss, DB hit
- No pre-loading of frequently accessed data

**Impact**:
- First 24 hours after deployment will have lower performance
- Recommendation generation is expensive (ML scoring)
- Cold start for match feeds

**Recommendation**: 
Consider cache warming for:
1. User profiles (load on demand)
2. Recommendations (generate on scheduled background job)
3. Match feeds (pre-compute for active users)

---

### 3.5 MEDIUM: No Cache Monitoring or Metrics
**Location**: No cache metrics configured

**Issue**:
- No visibility into cache hit/miss rates
- Can't tell if cache is actually helping
- No alerts for cache performance degradation
- Redis connection pool might be undersized

**Current Connection Pool**:
```yaml
lettuce:
  pool:
    max-active: 20
    max-idle: 10
    min-idle: 5
    max-wait: 2000ms
```

**Recommendation**: 
Enable Spring Cache metrics in application.yml:
```yaml
management:
  metrics:
    enable:
      cache: true
    distribution:
      timer.cache.puts.duration: [10ms, 100ms, 500ms]
```

---

## 4. CONFIGURATION SUMMARY TABLE

### Redis Configuration Across Services

| Service | Host Var | Port Var | Pool Size | Default TTL | Cache Type |
|---------|----------|----------|-----------|------------|-----------|
| user-service | REDIS_HOST | REDIS_PORT | 20 | 30min | Redis ✓ |
| match-service | REDIS_HOST | REDIS_PORT | 20 | 30min | Redis ✓ |
| chat-service | REDIS_HOST | REDIS_PORT | 20 | 60min | Redis ✓ |
| recommendation-service | REDIS_HOST | REDIS_PORT | 20 | 1440min | Redis ✓ |
| api-gateway | REDIS_HOST | REDIS_PORT | - | - | Reactive ✓ |
| vaadin-ui-service | REDIS_HOST | REDIS_PORT | - | 30min | Sessions ✓ |

**All services properly configured for Redis** ✓

---

### Cache Names Defined vs Used

| Cache Name | Defined In | Used In | TTL | Status |
|-----------|-----------|--------|-----|--------|
| users | CacheConstants | UserService | 1h | ✓ |
| user_preferences | CacheConstants | PreferenceService | 1h | ✓ |
| user_feed | CacheConstants | NOT USED | 24h | ⚠ |
| feed | match-service/CacheConfig | FeedService | 24h | ✓ |
| matches | CacheConstants | MatchService | 24h | 30min ✗ |
| match_details | match-service/CacheConfig | MatchService | 1h | ✓ |
| recommendations | CacheConstants | RecommendationService | 24h | ✓ |
| conversations | CacheConstants | ConversationService | 1h | 30-60min ✓ |
| conversation_messages | chat-service/CacheConfig | MessageService | 1h | ✓ |
| unread_counts | chat-service/CacheConfig | MessageService | 15min | ✓ |
| token_blacklist | CacheConstants | NEVER USED | 7d | ✗ |
| sessions | CacheConstants | Vaadin | 30min | ✓ |
| rate_limit | CacheConstants | RateLimitFilter | 1min | ✓ |

---

## 5. CACHE ANNOTATION AUDIT

### Services with @Cacheable/@CacheEvict

| Service | Method | Annotation | Cache | Issue |
|---------|--------|-----------|-------|-------|
| **UserService** | getUserById() | @Cacheable | users | ✓ |
| | updateUser() | @Caching(evict) | users, user_by_email | ✓ |
| | deleteUser() | @Caching(evict) | users, user_by_email, prefs | ✓ |
| **PreferenceService** | getPreferences() | @Cacheable | user_preferences | ✓ |
| | updatePreferences() | @CacheEvict | user_preferences | ✓ |
| **FeedService** | getFeed() | @Cacheable | feed | ⚠ Pagination key issue |
| **MatchService** | getMatches() | @Cacheable | matches | ⚠ Pagination key issue |
| | getMatchDetails() | @Cacheable | match_details | ✓ |
| | unmatch() | @CacheEvict | matches, match_details | ✓ |
| **SwipeService** | recordSwipe() | @CacheEvict | feed | ⚠ INCOMPLETE |
| **ConversationService** | getConversations() | @Cacheable | conversations | ✓ |
| **MessageService** | sendMessage() | @Caching(evict) | messages, conversations, unread | ✓ |
| | getMessages() | @Cacheable | conversation_messages | ✓ |
| | markAllAsRead() | @Caching(evict) | conversation_messages, unread | ✓ |
| | countUnread() | @Cacheable | unread_counts | ✓ |
| | getLastMessage() | NONE | - | ⚠ MISSING |
| **RecommendationService** | getRecommendations() | @Cacheable | recommendations | ✓ |
| | getCompatibilityScore() | @Cacheable | compatibility_scores | ✓ |
| | refreshRecommendations() | @CacheEvict | recommendations | ✓ |

---

## 6. JWT & TOKEN BLACKLIST AUDIT

### Current Implementation Status

| Component | Implemented | Status |
|-----------|-------------|--------|
| JWT Generation | ✓ | User Service generates tokens |
| JWT Validation | ✓ | API Gateway validates tokens |
| Access Token (15min) | ✓ | Proper expiration |
| Refresh Token (7d) | ✓ | Stored in DB with revoke flag |
| Token Refresh Flow | ✓ | User Service handles refresh |
| Refresh Token Revocation | ✓ | Revoked in DB on logout |
| **Access Token Blacklist** | ✗ | **NOT IMPLEMENTED** |
| Token Blacklist Check | ✗ | Never checked in JwtValidator |
| Logout Invalidation | ✗ | Only revokes refresh tokens |

### Security Gap
After logout:
1. Refresh tokens are revoked in DB ✓
2. But access token remains valid for 15 minutes ✗
3. No way to immediately invalidate access token
4. User can continue using old access token until expiration

---

## 7. SUMMARIZED RECOMMENDATIONS

### Priority 1 (CRITICAL - Fix Immediately)
1. **Implement token blacklist check** in JwtValidator
   - Add Redis cache check before token validation
   - Blacklist access token on logout
   - File: `/backend/api-gateway/src/main/java/com/dating/gateway/security/JwtValidator.java`

2. **Fix SwipeService cache eviction** 
   - Invalidate MATCHES_CACHE for both users when match is created
   - File: `/backend/match-service/src/main/java/com/dating/match/service/SwipeService.java`

3. **Add token blacklist listener** in Recommendation Service
   - Must invalidate recommendations when profile changes
   - Implement RabbitMQ listener for UserUpdatedEvent

### Priority 2 (HIGH - Fix in Next Sprint)
1. Fix FeedService and MatchService pagination cache keys
2. Add @Cacheable to MessageService.getLastMessage()
3. Fix Match Service MATCHES_CACHE TTL (24 hours, not 30 minutes)
4. Add conversation cache invalidation on match created event

### Priority 3 (MEDIUM - Nice to Have)
1. Consolidate cache definitions in CacheConstants
2. Add cache metrics monitoring
3. Consider cache warming strategy
4. Document serialization strategy
5. Extend Vaadin session timeout

---

## 8. TESTING RECOMMENDATIONS

### Cache Invalidation Tests
```java
@Test
void testSwipeCreatesMatchAndInvalidatesCaches() {
    // User A swipes right on User B
    // User B has already swiped right on User A
    // Verify:
    // 1. Feed cache invalidated for BOTH users
    // 2. Matches cache invalidated for BOTH users
}

@Test
void testUserProfileUpdateInvalidatesRecommendations() {
    // User updates profile
    // Verify: RecommendationService caches invalidated
}

@Test
void testLogoutInvalidatesAccessToken() {
    // User logs out
    // Verify: Access token added to blacklist
    // Verify: Next request with same token is rejected
}
```

### Cache Performance Tests
```java
@Test
void testFeedCacheHitRate() {
    // Call getFeed() 10 times with same userId
    // Verify: Only 1 DB query (9 cache hits)
}
```

---

## 9. CONCLUSION

**Overall Grade: B+ (Good with Critical Gaps)**

**Strengths**:
- ✓ Consistent Redis configuration across all services
- ✓ Well-defined TTL hierarchy in CacheConstants
- ✓ Most @Cacheable annotations in place
- ✓ Most @CacheEvict annotations on write operations
- ✓ Proper serialization strategy

**Weaknesses**:
- ✗ Token blacklist NOT implemented (security issue)
- ✗ Incomplete cache invalidation on match creation
- ✗ Pagination cache key antipattern
- ✗ Cache invalidation chain missing (profile → recommendations)
- ✗ No cache warming or monitoring

**Estimated Impact of Issues**:
- **Security**: Token blacklist is a critical gap
- **Data Consistency**: Could see stale matches/conversations for minutes
- **Performance**: Pagination cache keys reduce hit rate
- **User Experience**: Some features could take up to 24 hours to reflect updates

**Recommendation**: Address Priority 1 items immediately before next production deployment.
