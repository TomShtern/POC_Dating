# SECOND-ROUND PERFORMANCE REVIEW - Java Dating App Microservices Backend
**Date:** 2025-11-19  
**Scope:** User, Match, Chat, Recommendation Services  
**Severity Classifications:** CRITICAL (blocks functionality), HIGH (severe perf impact), MEDIUM (moderate impact), LOW (minor)

---

## PART 1: VERIFICATION OF FIRST-ROUND FIXES

### ✓ PASS: @EnableAsync Annotations (3 Services Verified)
- **User Service:** `@EnableAsync` ✓ Present (line 14)
- **Match Service:** `@EnableAsync` ✓ Present (line 16)
- **Recommendation Service:** `@EnableAsync` ✓ Present (line 14)

**ISSUE FOUND:** Chat Service MISSING @EnableAsync ✗
- **Severity:** HIGH
- **File:** `/home/user/POC_Dating/backend/chat-service/src/main/java/com/dating/chat/ChatServiceApplication.java`
- **Problem:** Chat Service has @EnableScheduling but no @EnableAsync, yet async features may be used elsewhere
- **Impact:** Async methods would run on calling thread instead of thread pool
- **Fix:** Add @EnableAsync annotation to ChatServiceApplication class

---

### ✓ PASS: JPA @Index Annotations (Syntax Verified)
All @Index annotations reviewed - **SYNTAX IS CORRECT**

**Entities Checked:**
- User (4 indexes) ✓
- UserPreference (1 index) ✓
- RefreshToken (4 indexes) ✓
- Match (5 indexes with unique constraint) ✓
- Swipe (4 indexes with unique constraint) ✓
- MatchScore (2 indexes) ✓
- Message (6 indexes) ✓
- Recommendation (6 indexes) ✓
- InteractionHistory (5 indexes) ✓

**No syntax errors found.** All use valid columnList syntax with proper composite indexes.

---

### ⚠ PARTIAL: Repository Cache Warming Methods Exist (But Missing Async Executor)
**Cache warming methods found:**
- UserRepository: `findByLastLoginAtAfterOrderByLastLoginAtDesc` ✓
- SwipeRepository: `findDistinctUserIdsByCreatedAtAfter` ✓
- RecommendationRepository: `findDistinctUserIdsWithRecentRecommendations` ✓
- RecommendationRepository: `findByUserIdAndExpiresAtAfterOrderByScoreDesc` ✓

**Cache Warming Implementation:**
- User Service CacheWarmer uses @Async ✓
- Match Service CacheWarmer uses @Async ✓
- Recommendation Service CacheWarmer uses @Async ✓

**CRITICAL ISSUE:** No TaskExecutor Bean Configured
- **Severity:** HIGH
- **Problem:** @Async methods without a custom TaskExecutor will use the default SimpleAsyncTaskExecutor
- **Impact:** Creates new thread for EACH async call (unbounded, no pooling) → memory leak risk
- **Fix:** Add AsyncConfig with ThreadPoolTaskExecutor bean

---

## PART 2: NEW PERFORMANCE ISSUES FOUND

### ISSUE 1: N+1 QUERY PROBLEM - MATCH SERVICE (CRITICAL)
**Severity:** HIGH  
**Type:** N+1 Queries via HTTP Calls  

#### Location 1: MatchService.getMatches()
**File:** `/home/user/POC_Dating/backend/match-service/src/main/java/com/dating/match/service/MatchService.java`  
**Lines:** 55-57

```java
List<MatchResponse> matches = matchPage.getContent().stream()
        .map(match -> mapToMatchResponse(match, userId))  // LINE 56
        .toList();
```

**mapToMatchResponse() chain:**
```java
private MatchResponse mapToMatchResponse(Match match, UUID currentUserId) {
    UUID otherUserId = match.getOtherUserId(currentUserId);
    MatchResponse.MatchedUserInfo userInfo = getMatchedUserInfo(otherUserId);  // HTTP CALL HERE
    return new MatchResponse(match.getId(), userInfo, match.getMatchedAt());
}

private MatchResponse.MatchedUserInfo getMatchedUserInfo(UUID userId) {
    var user = userServiceClient.getUserById(userId);  // **INDIVIDUAL HTTP CALL**
    // ...
}
```

**Problem:**
- If user has 50 matches → **50 HTTP calls to User Service**
- Sequential, not parallel → blocks response thread
- Cache hit won't help if Match ID is part of cache key

**Impact:**
- P95 latency: Could hit 5-10 seconds for users with many matches
- User Service throughput bottleneck
- Blocking response thread

**Fix:** Batch load user data
```java
// Collect all user IDs to fetch
Set<UUID> userIdsToFetch = matchPage.getContent().stream()
    .map(match -> match.getOtherUserId(userId))
    .collect(Collectors.toSet());

// Single batch call
List<UserProfileResponse> userMap = userServiceClient.getUsersByIds(new ArrayList<>(userIdsToFetch));
Map<UUID, UserProfileResponse> userCache = userMap.stream()
    .collect(Collectors.toMap(UserProfileResponse::id, u -> u));

// Use cached data in stream
List<MatchResponse> matches = matchPage.getContent().stream()
    .map(match -> {
        UUID otherUserId = match.getOtherUserId(userId);
        UserProfileResponse user = userCache.get(otherUserId);
        return mapToMatchResponseWithCachedUser(match, userId, user);
    })
    .toList();
```

---

#### Location 2: MatchService.getMatchDetails()
**File:** `/home/user/POC_Dating/backend/match-service/src/main/java/com/dating/match/service/MatchService.java`  
**Lines:** 155-156

```java
private MatchDetailResponse mapToMatchDetailResponse(Match match) {
    var user1Info = getUserInfo(match.getUser1Id());    // HTTP CALL 1
    var user2Info = getUserInfo(match.getUser2Id());    // HTTP CALL 2
    // ...
}

private MatchDetailResponse.UserInfo getUserInfo(UUID userId) {
    var user = userServiceClient.getUserById(userId);  // **INDIVIDUAL HTTP CALL**
    // ...
}
```

**Problem:** 2 sequential HTTP calls for every match detail request

**Fix:** Batch load both users in one call

---

#### Location 3: FeedService.getFeed()
**File:** `/home/user/POC_Dating/backend/match-service/src/main/java/com/dating/match/service/FeedService.java`  
**Lines:** 69-75

```java
for (int i = start; i < end; i++) {
    var rec = filteredRecs.get(i);
    FeedResponse.FeedUserInfo userInfo = getUserInfoForFeed(rec.id(), rec.score());  // **INSIDE LOOP**
    if (userInfo != null) {
        feedUsers.add(userInfo);
    }
}

private FeedResponse.FeedUserInfo getUserInfoForFeed(UUID userId, int compatibilityScore) {
    try {
        var user = userServiceClient.getUserById(userId);  // **INDIVIDUAL HTTP CALL INSIDE LOOP**
        // ...
    }
}
```

**Problem:**
- If limit=20 → **20 HTTP calls in a loop**
- Sequential, not parallel
- Worst case: ~500ms per user × 20 = **10 seconds latency**

**Fix:** Collect IDs, batch fetch, then map:
```java
// Collect IDs to fetch
List<UUID> userIds = filteredRecs.stream()
    .skip(start)
    .limit(limit - start)
    .map(RecommendedUser::id)
    .toList();

// Single batch call
List<UserProfileResponse> users = userServiceClient.getUsersByIds(userIds);
Map<UUID, UserProfileResponse> userMap = users.stream()
    .collect(Collectors.toMap(UserProfileResponse::id, u -> u));

// Now map using cached data
List<FeedResponse.FeedUserInfo> feedUsers = filteredRecs.stream()
    .skip(start)
    .limit(limit - start)
    .map(rec -> {
        UserProfileResponse user = userMap.get(rec.id());
        return toFeedUserInfo(user, rec.score());
    })
    .collect(Collectors.toList());
```

---

#### Location 4: SwipeService.createMatch()
**File:** `/home/user/POC_Dating/backend/match-service/src/main/java/com/dating/match/service/SwipeService.java`  
**Lines:** 156

```java
private Match createMatch(UUID userId, UUID targetUserId) {
    // ... match creation code ...
    eventPublisher.publishMatchCreated(match, getUserName(user1Id), getUserName(user2Id));  // 2 CALLS
}

private String getUserName(UUID userId) {
    var user = userServiceClient.getUserById(userId);  // **HTTP CALL**
    return user.firstName() != null ? user.firstName() : user.username();
}
```

**Problem:** 2 sequential HTTP calls to get display names

**Fix:** Batch load both users

---

### ISSUE 2: MISSING BATCH ENDPOINT IN MATCH SERVICE (HIGH)
**Severity:** HIGH  
**Type:** Architecture Gap  

**Problem:**
- Match Service's `UserServiceClient` has no batch endpoint
- Recommendation Service's client HAS batch: `getUsersByIds(@RequestBody List<UUID> userIds)`
- This forces N+1 pattern in Match Service

**File:** `/home/user/POC_Dating/backend/match-service/src/main/java/com/dating/match/client/UserServiceClient.java`

**Missing Method:**
```java
@PostMapping("/api/users/batch")
List<UserProfileResponse> getUsersByIds(@RequestBody List<UUID> userIds);
```

**Fix:** Add batch method to Match Service's UserServiceClient interface

---

### ISSUE 3: NO ASYNC EXECUTOR CONFIGURED (HIGH)
**Severity:** HIGH  
**Type:** Resource Management  

**Problem:**
- @EnableAsync enabled but no custom TaskExecutor bean defined
- Spring will use **SimpleAsyncTaskExecutor** which:
  - Creates **new thread per async call** (no pooling)
  - No queue bounds
  - Memory leak risk under load
  - Unbounded thread creation

**Files Affected:**
- User Service CacheWarmer uses @Async
- Match Service CacheWarmer uses @Async
- Recommendation Service CacheWarmer uses @Async
- (Any future async methods)

**Impact:**
- 1000+ users logging in → 1000 threads created for cache warming
- Server memory exhaustion
- Thread creation overhead (1-2MB per thread in Java)

**Fix:** Create AsyncConfig in each service:
```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
```

---

### ISSUE 4: REDIS POOL CONFIGURATION SUBOPTIMAL (MEDIUM)
**Severity:** MEDIUM  
**Type:** Connection Management  

**Current Configuration (all services):**
```yaml
data:
  redis:
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 2000ms
```

**Problems:**
1. **max-wait: 2000ms** - May cause timeout under load spike
2. **min-idle: 5** - May keep 5 idle connections even during low traffic
3. **No timeout on idle connections** in pool config

**Impact:**
- High load → Redis connection timeout → request failure
- Recommendation: increase max-wait to 5000ms
- Consider connection eviction config

**Files:**
- `/home/user/POC_Dating/backend/user-service/src/main/resources/application.yml` (lines 60-65)
- `/home/user/POC_Dating/backend/match-service/src/main/resources/application.yml` (lines 60-65)
- `/home/user/POC_Dating/backend/chat-service/src/main/resources/application.yml` (lines 63-68)
- `/home/user/POC_Dating/backend/recommendation-service/src/main/resources/application.yml` (lines 59-64)

**Fix:** Increase max-wait and add eviction config
```yaml
data:
  redis:
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 5000ms
        min-evictable-idle-time-millis: 300000
        eviction-policy: LEAST_RECENTLY_USED
```

---

### ISSUE 5: CACHE TTL INCONSISTENCY (MEDIUM)
**Severity:** MEDIUM  
**Type:** Configuration  

**Problem:**
Different cache TTLs may cause stale data or unnecessary cache misses:

| Service | Cache | TTL | Issue |
|---------|-------|-----|-------|
| User Service | users | 1 hour | Profile changes stale for 1 hour |
| Match Service | feed | 24 hours | ⚠ Very long, may miss new users |
| Match Service | matches | 30 minutes | OK |
| Chat Service | messages | 60 minutes | OK (immutable) |
| Recommendation | recommendations | 24 hours | Depends on algorithm TTL |

**Recommendation:**
- User profiles: 1 hour is reasonable IF cache is invalidated on updates
- Feed: 24 hours is too long for fast-changing recommendations
- Suggestion: 4-6 hours for feed, or invalidate on new users

**Files:**
- `/home/user/POC_Dating/backend/match-service/src/main/java/com/dating/match/config/CacheConfig.java` (lines 32-34)
- `/home/user/POC_Dating/backend/recommendation-service/src/main/java/com/dating/recommendation/config/CacheConfig.java` (lines 32-34)

---

### ISSUE 6: MISSING @Transactional(readOnly=true) ON READ METHODS (MEDIUM)
**Severity:** MEDIUM  
**Type:** Performance Optimization  

**Problem:**
Some methods with "read" in their names don't have `readOnly=true`:

**Examples:**
- MatchService.getMatches() HAS @Transactional(readOnly=true) ✓
- MatchService.getMatchDetails() HAS @Transactional(readOnly=true) ✓
- FeedService.getFeed() HAS @Transactional(readOnly=true) ✓
- MessageService.getMessages() HAS @Transactional(readOnly=true) ✓

**Actually PASS - all read methods correctly marked**

---

### ISSUE 7: SLOW FEIGN TIMEOUT CONFIGURATION (MEDIUM)
**Severity:** MEDIUM  
**Type:** Resilience  

**Current Configuration:**
```yaml
feign:
  client:
    config:
      default:
        connect-timeout: 5000
        read-timeout: 10000
```

**Problem:**
- read-timeout: 10s is reasonable but recommend 15s for heavy operations
- Connect timeout 5s might be too tight for overloaded services
- No circuit breaker timeout configured

**Files:**
- `/home/user/POC_Dating/backend/match-service/src/main/resources/application.yml` (lines 111-113)
- Recommendation Service uses shorter timeouts (5s) which may cause cascading failures

**Recommendation:**
```yaml
feign:
  client:
    config:
      default:
        connect-timeout: 8000      # More forgiving
        read-timeout: 15000        # Account for heavy operations
  circuitbreaker:
    enabled: true
    sliding-window-size: 100
    failure-rate-threshold: 50
```

---

### ISSUE 8: NO QUERY RESULT SET SIZING (LOW)
**Severity:** LOW  
**Type:** Query Optimization  

**Problem:**
Some queries return ALL data and rely on application pagination:

**Example - SwipeService:**
```java
List<Swipe> findByUserId(UUID userId);  // Returns ALL swipes for user
```

For user with 10,000 swipes:
- Loads entire list into memory
- Then application filters/paginates
- Network bandwidth wasted

**Better:** Add pagination to query
```java
List<Swipe> findByUserId(UUID userId, Pageable pageable);
```

**Affected Repositories:**
- SwipeRepository.findByUserId() - should add Pageable
- SwipeRepository.findByTargetUserId() - should add Pageable

---

### ISSUE 9: SYNCHRONOUS CACHE WARMING BLOCKS STARTUP (MEDIUM)
**Severity:** MEDIUM  
**Type:** Startup Performance  

**Problem:**
CacheWarmer.run() is called on ApplicationRunner.run() → **blocks application startup**

```java
@Override
public void run(ApplicationArguments args) {
    log.info("Starting cache warming...");
    warmCaches();  // BLOCKING CALL
}

@Async
public void warmCaches() {
    // ...1000 database queries...
}
```

Even with @Async, warmCaches() is called synchronously, returning immediately but:
- Startup logging shows warmup as instant
- Async task begins immediately, heavy load during startup

**Better approach:**
```java
@Override
public void run(ApplicationArguments args) {
    warmCachesAsync();  // Fire and forget
}

@Async
public void warmCachesAsync() {
    // ... same logic
}
```

Or implement scheduled task instead of ApplicationRunner.

**Files:**
- `/home/user/POC_Dating/backend/match-service/src/main/java/com/dating/match/config/CacheWarmer.java` (lines 40-43)
- `/home/user/POC_Dating/backend/user-service/src/main/java/com/dating/user/config/CacheWarmer.java` (lines 36-39)
- `/home/user/POC_Dating/backend/recommendation-service/src/main/java/com/dating/recommendation/config/CacheWarmer.java` (lines 36-39)

---

### ISSUE 10: INEFFICIENT COLLECTION OPERATIONS (LOW)
**Severity:** LOW  
**Type:** Code Quality  

**Location:** CacheWarmer classes
```java
Set<UUID> activeUserIds = new HashSet<>();
swipeRepository.findDistinctUserIdsByCreatedAtAfter(threshold, PageRequest.of(0, MAX_USERS_TO_WARM))
        .forEach(activeUserIds::add);  // Could use toSet() directly
```

**Better:**
```java
Set<UUID> activeUserIds = swipeRepository.findDistinctUserIdsByCreatedAtAfter(threshold, PageRequest.of(0, MAX_USERS_TO_WARM))
        .stream()
        .collect(Collectors.toSet());
```

---

## SUMMARY TABLE

| Issue ID | Service | Type | Severity | Impact | Fixable |
|----------|---------|------|----------|--------|---------|
| 1 | Chat | Missing @EnableAsync | HIGH | Async runs on caller thread | ✓ |
| 2 | Match | N+1 Queries (MatchService) | HIGH | 50 users = 50 HTTP calls | ✓ |
| 3 | Match | N+1 Queries (FeedService) | HIGH | 20 feed items = 20 HTTP calls | ✓ |
| 4 | Match | Missing Batch Endpoint | HIGH | Forces N+1 pattern | ✓ |
| 5 | All | No TaskExecutor Bean | HIGH | Memory leak risk | ✓ |
| 6 | All | Redis Pool Timeout | MEDIUM | Connection failures under load | ✓ |
| 7 | Match | Feign Timeout | MEDIUM | Cascading failures | ✓ |
| 8 | All | Cache TTL Inconsistency | MEDIUM | Stale data or cache misses | ✓ |
| 9 | All | Startup Cache Warming | MEDIUM | Heavy load during startup | ✓ |
| 10 | All | Collection Operations | LOW | Minor inefficiency | ✓ |

---

## PRIORITY FIX ORDER

### Phase 1 (Critical - This Week)
1. **Add @EnableAsync to Chat Service** (5 min)
2. **Fix N+1 in MatchService.getMatches()** (30 min) - Affects user experience directly
3. **Fix N+1 in FeedService.getFeed()** (30 min) - Highest impact on user experience
4. **Add batch endpoint to Match Service UserServiceClient** (15 min)
5. **Configure TaskExecutor bean** (30 min) - Memory safety

### Phase 2 (High - Next Sprint)
6. Refactor MatchService.getMatchDetails() for batch loading
7. Refactor SwipeService.createMatch() for batch loading
8. Increase Redis pool max-wait timeout
9. Adjust Feign timeout configs

### Phase 3 (Medium - Later)
10. Optimize cache warming startup behavior
11. Adjust cache TTLs based on testing
12. Add pagination to unlimited repository methods

---

