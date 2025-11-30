# Test Audit Report: DTO Structure Changes

**Date:** 2025-11-18  
**Scope:** Audit of all test files for DTO structure compatibility  
**Total Issues Found:** 18 issues across 5 broken test files

---

## Executive Summary

The recent DTO structure changes have introduced breaking changes in 5 test files across the chat, user, and recommendation services. The main issues are:

1. **MessageResponse field changes** - `sentAt`/`deliveredAt` replaced with `createdAt` only
2. **UUID type conversions** - UserResponse and PreferencesResponse now use UUID instead of String for IDs
3. **Missing pagination fields** - List responses missing `limit` and `offset` fields
4. **Getter method name changes** - New method names don't match old test expectations

**Critical Impact:** 5 out of 13 test files will fail on execution or compilation.

---

## CRITICAL ISSUES FOUND

### 1. **MessageMapperTest.java** [6 issues]
**File:** `/home/user/POC_Dating/backend/chat-service/src/test/java/com/dating/chat/mapper/MessageMapperTest.java`

| Line(s) | Issue | Current | Fix |
|---------|-------|---------|-----|
| 35 | Using `deliveredAt` field (no longer exists) | `.deliveredAt(null)` | Remove entirely |
| 49 | Assertion on removed method | `.getSentAt()` | Change to `.getCreatedAt()` |
| 50 | Assertion on removed method | `.getDeliveredAt()` | Change to `.getReadAt()` |
| 57-58 | Variable naming confusion | `sentAt`, `deliveredAt` | Rename to `createdAt`, `readAt` |
| 68 | Using `deliveredAt` field | `.deliveredAt(deliveredAt)` | Remove entirely |
| 77-78 | Multiple assertions on missing methods | `.getSentAt()`, `.getDeliveredAt()` | Use `.getCreatedAt()` and `.getReadAt()` |

**Detailed Fix:**

Current broken test (lines 54-80):
```java
@Test
void testToMessageResponse_WithDeliveredAndRead() {
    // Arrange
    Instant sentAt = Instant.now().minusSeconds(3600);
    Instant deliveredAt = Instant.now().minusSeconds(1800);
    Instant readAt = Instant.now();

    Message message = Message.builder()
            .id(UUID.randomUUID())
            .matchId(UUID.randomUUID())
            .senderId(UUID.randomUUID())
            .content("Test")
            .status(MessageStatus.READ)
            .createdAt(sentAt)              // ✗ Conflict: variable named sentAt
            .deliveredAt(deliveredAt)       // ✗ Field doesn't exist
            .readAt(readAt)
            .build();

    // Act
    MessageResponse response = mapper.toMessageResponse(message);

    // Assert
    assertEquals(MessageStatus.READ, response.getStatus());
    assertEquals(sentAt, response.getSentAt());        // ✗ Method doesn't exist
    assertEquals(deliveredAt, response.getDeliveredAt());  // ✗ Method doesn't exist
    assertEquals(readAt, response.getReadAt());
}
```

Should be:
```java
@Test
void testToMessageResponse_WithReadAt() {
    // Arrange
    Instant createdAt = Instant.now().minusSeconds(3600);
    Instant readAt = Instant.now();

    Message message = Message.builder()
            .id(UUID.randomUUID())
            .matchId(UUID.randomUUID())
            .senderId(UUID.randomUUID())
            .content("Test")
            .status(MessageStatus.READ)
            .createdAt(createdAt)
            .readAt(readAt)
            .build();

    // Act
    MessageResponse response = mapper.toMessageResponse(message);

    // Assert
    assertEquals(MessageStatus.READ, response.getStatus());
    assertEquals(createdAt, response.getCreatedAt());
    assertEquals(readAt, response.getReadAt());
}
```

Also fix line 35 in `testToMessageResponse_Success()`:
```java
// BEFORE
Message message = Message.builder()
        .id(messageId)
        .matchId(conversationId)
        .senderId(senderId)
        .content("Hello!")
        .status(MessageStatus.SENT)
        .createdAt(now)
        .deliveredAt(null)  // ✗ REMOVE THIS LINE
        .readAt(null)
        .build();

// AFTER
Message message = Message.builder()
        .id(messageId)
        .matchId(conversationId)
        .senderId(senderId)
        .content("Hello!")
        .status(MessageStatus.SENT)
        .createdAt(now)
        .readAt(null)
        .build();
```

Also fix line 49:
```java
// BEFORE
assertEquals(now, response.getSentAt());     // ✗ Method doesn't exist

// AFTER
assertEquals(now, response.getCreatedAt());
```

Also fix line 50:
```java
// BEFORE
assertNull(response.getDeliveredAt());       // ✗ Method doesn't exist

// AFTER
assertNull(response.getReadAt());
```

---

### 2. **ChatServiceTest.java** [3 issues]
**File:** `/home/user/POC_Dating/backend/chat-service/src/test/java/com/dating/chat/service/ChatServiceTest.java`

| Line(s) | Issue | Current | Fix |
|---------|-------|---------|-----|
| 70 | Using `sentAt` field (doesn't exist) | `.sentAt(Instant.now())` | Change to `.createdAt(Instant.now())` |
| 80 | Using `sentAt` field | `.sentAt(Instant.now().minusSeconds(60))` | Change to `.createdAt(...)` |
| 166-170 | Missing pagination fields in MessageListResponse | No `limit`/`offset` | Add `.limit(50).offset(0)` |

**Issue 2a & 2b - Line 70 & 80:**
```java
// BROKEN - Line 70
testMessageResponse = MessageResponse.builder()
        .id(messageId)
        .conversationId(conversationId)
        .senderId(senderId)
        .content("Hello!")
        .status(MessageStatus.SENT)
        .sentAt(Instant.now())              // ✗ Field doesn't exist
        .build();

// BROKEN - Line 80
otherUserMessageResponse = MessageResponse.builder()
        .id(UUID.randomUUID())
        .conversationId(conversationId)
        .senderId(receiverId)
        .content("Hi there!")
        .status(MessageStatus.SENT)
        .sentAt(Instant.now().minusSeconds(60))  // ✗ Field doesn't exist
        .build();

// FIX - Change both to use createdAt
.createdAt(Instant.now())
.createdAt(Instant.now().minusSeconds(60))
```

**Issue 2c - Lines 166-170:**
```java
// BROKEN
MessageListResponse expectedResponse = MessageListResponse.builder()
        .conversationId(conversationId)
        .messages(messages)
        .total(1)
        .hasMore(false)
        .build();  // ✗ Missing limit and offset

// FIX
MessageListResponse expectedResponse = MessageListResponse.builder()
        .conversationId(conversationId)
        .messages(messages)
        .total(1)
        .limit(50)         // ADD
        .offset(0)         // ADD
        .hasMore(false)
        .build();
```

---

### 3. **MessageServiceTest.java** [1 issue]
**File:** `/home/user/POC_Dating/backend/chat-service/src/test/java/com/dating/chat/service/MessageServiceTest.java`

| Line(s) | Issue | Current | Fix |
|---------|-------|---------|-----|
| 79 | Using `sentAt` field | `.sentAt(Instant.now())` | Change to `.createdAt(Instant.now())` |

**Fix:**
```java
// BROKEN - Line 79
testMessageResponse = MessageResponse.builder()
        .id(messageId)
        .conversationId(conversationId)
        .senderId(senderId)
        .content("Hello!")
        .status(MessageStatus.SENT)
        .sentAt(Instant.now())              // ✗ Field doesn't exist
        .build();

// FIXED
testMessageResponse = MessageResponse.builder()
        .id(messageId)
        .conversationId(conversationId)
        .senderId(senderId)
        .content("Hello!")
        .status(MessageStatus.SENT)
        .createdAt(Instant.now())           // ✓ Use correct field
        .build();
```

---

### 4. **UserServiceTest.java** [2 issues]
**File:** `/home/user/POC_Dating/backend/user-service/src/test/java/com/dating/user/service/UserServiceTest.java`

| Line(s) | Issue | Current | Fix |
|---------|-------|---------|-----|
| 62 | UserResponse.id using String instead of UUID | `.id(userId.toString())` | Change to `.id(userId)` |
| 82 | Type mismatch in assertion | `assertEquals(userId.toString(), ...)` | Change to `assertEquals(userId, ...)` |
| 115 | UserResponse.id using String instead of UUID | `.id(userId.toString())` | Change to `.id(userId)` |

**Issue 4a & 4b - Lines 62 & 82 (setUp method):**
```java
// BROKEN - Line 62
userResponse = UserResponse.builder()
        .id(userId.toString())              // ✗ Should be UUID, not String
        .email("test@example.com")
        .username("testuser")
        .firstName("John")
        .lastName("Doe")
        .build();

// BROKEN - Line 82 (in test assertion)
assertEquals(userId.toString(), response.getId());  // ✗ Type mismatch

// FIX
userResponse = UserResponse.builder()
        .id(userId)                         // ✓ Use UUID directly
        .email("test@example.com")
        .username("testuser")
        .firstName("John")
        .lastName("Doe")
        .build();

// FIX - Line 82
assertEquals(userId, response.getId());     // ✓ Compare UUID to UUID
```

**Issue 4c - Line 115 (in testUpdateUser_Success):**
```java
// BROKEN - Line 115
UserResponse updatedResponse = UserResponse.builder()
        .id(userId.toString())              // ✗ Should be UUID
        .email("test@example.com")
        .username("testuser")
        .firstName("Jane")
        .lastName("Smith")
        .bio("New bio")
        .build();

// FIX
UserResponse updatedResponse = UserResponse.builder()
        .id(userId)                         // ✓ Use UUID directly
        .email("test@example.com")
        .username("testuser")
        .firstName("Jane")
        .lastName("Smith")
        .bio("New bio")
        .build();
```

---

### 5. **PreferenceServiceTest.java** [4 issues]
**File:** `/home/user/POC_Dating/backend/user-service/src/test/java/com/dating/user/service/PreferenceServiceTest.java`

| Line(s) | Issue | Current | Fix |
|---------|-------|---------|-----|
| 71-72 | Using String instead of UUID for id/userId | `.id(...toString())`, `.userId(...toString())` | Remove `.toString()` |
| 131-132 | Using String instead of UUID for id/userId | `.id(...toString())`, `.userId(...toString())` | Remove `.toString()` |

**Issue 5a - Lines 71-72 (setUp method):**
```java
// BROKEN
preferencesResponse = PreferencesResponse.builder()
        .id(testPreference.getId().toString())  // ✗ Should be UUID
        .userId(userId.toString())              // ✗ Should be UUID
        .minAge(21)
        .maxAge(35)
        .maxDistanceKm(50)
        .interestedIn("FEMALE")
        .notificationEnabled(true)
        .build();

// FIX
preferencesResponse = PreferencesResponse.builder()
        .id(testPreference.getId())             // ✓ Use UUID directly
        .userId(userId)                         // ✓ Use UUID directly
        .minAge(21)
        .maxAge(35)
        .maxDistanceKm(50)
        .interestedIn("FEMALE")
        .notificationEnabled(true)
        .build();
```

**Issue 5b - Lines 131-132 (in testUpdatePreferences_Success):**
```java
// BROKEN
PreferencesResponse updatedResponse = PreferencesResponse.builder()
        .id(testPreference.getId().toString())  // ✗ Should be UUID
        .userId(userId.toString())              // ✗ Should be UUID
        .minAge(25)
        .maxAge(40)
        .maxDistanceKm(100)
        .interestedIn("BOTH")
        .notificationEnabled(false)
        .build();

// FIX
PreferencesResponse updatedResponse = PreferencesResponse.builder()
        .id(testPreference.getId())             // ✓ Use UUID directly
        .userId(userId)                         // ✓ Use UUID directly
        .minAge(25)
        .maxAge(40)
        .maxDistanceKm(100)
        .interestedIn("BOTH")
        .notificationEnabled(false)
        .build();
```

---

### 6. **RecommendationServiceTest.java** [1 potential issue]
**File:** `/home/user/POC_Dating/backend/recommendation-service/src/test/java/com/dating/recommendation/service/RecommendationServiceTest.java`

| Line(s) | Issue | Note | Severity |
|---------|-------|------|----------|
| 215 | Response may need limit/offset/hasMore fields | Service constructs response, verify fields are set | MEDIUM |

**Context - Line 215:**
```java
// Line 215 - service method call
RecommendationListResponse response = recommendationService.getRecommendations(userId, 10, "v1");
```

**Analysis:** The test calls a service method that constructs the response. The RecommendationListResponse DTO requires these fields:
- `recommendations` (List<RecommendationResponse>)
- `total` (long)
- `limit` (int)
- `offset` (int)
- `hasMore` (boolean)
- `generatedAt` (Instant)

**Action:** Review the `RecommendationService.getRecommendations()` method to ensure it sets all these fields. If the service constructs the response correctly, this test may pass. If the service needs to be fixed, that's a separate issue.

**Expected fix (if test needs updating):**
```java
RecommendationListResponse expectedResponse = RecommendationListResponse.builder()
        .recommendations(recommendations)
        .total(totalCount)
        .limit(10)              // ADD if missing
        .offset(0)              // ADD if missing
        .hasMore(totalCount > 10)  // ADD if missing
        .generatedAt(Instant.now())
        .build();
```

---

## SUMMARY TABLE

| File | Count | High | Medium | Issues |
|------|-------|------|--------|--------|
| MessageMapperTest.java | 6 | 5 | 1 | sentAt/deliveredAt removal, missing methods |
| ChatServiceTest.java | 3 | 3 | 0 | sentAt field, missing pagination fields |
| MessageServiceTest.java | 1 | 1 | 0 | sentAt field |
| UserServiceTest.java | 2 | 2 | 0 | String→UUID type conversion |
| PreferenceServiceTest.java | 4 | 4 | 0 | String→UUID type conversion |
| RecommendationServiceTest.java | 1 | 0 | 1 | Verify pagination fields |
| **TOTAL** | **18** | **15** | **3** | |

---

## Test Execution Impact

### Will FAIL (Compilation or Runtime Errors):
1. ✗ **MessageMapperTest.java** - Compilation fails on missing `getSentAt()`, `getDeliveredAt()`, and `.deliveredAt()` builder method
2. ✗ **ChatServiceTest.java** - Compilation fails on missing `.sentAt()` builder method, incomplete MessageListResponse
3. ✗ **MessageServiceTest.java** - Compilation fails on missing `.sentAt()` builder method
4. ✗ **UserServiceTest.java** - Runtime assertion failures due to type mismatch (String vs UUID)
5. ✗ **PreferenceServiceTest.java** - Runtime assertion failures or type issues (String vs UUID)

### Requires Review:
6. ? **RecommendationServiceTest.java** - May pass if service constructs response correctly

### No Issues:
- ✓ MatchServiceTest.java
- ✓ FeedServiceTest.java
- ✓ SwipeServiceTest.java
- ✓ AuthServiceTest.java
- ✓ Other non-audited tests

---

## Remediation Checklist

### Priority 1 - Critical (Fix First):
- [ ] **MessageMapperTest.java** - 6 issues
  - [ ] Line 35: Remove `.deliveredAt(null)`
  - [ ] Line 49: Change `.getSentAt()` to `.getCreatedAt()`
  - [ ] Line 50: Change `.getDeliveredAt()` to `.getReadAt()`
  - [ ] Lines 57-58: Rename variables `sentAt` → `createdAt`, `deliveredAt` → `readAt`
  - [ ] Line 68: Remove `.deliveredAt(deliveredAt)`
  - [ ] Line 77-78: Update assertions to use new method names

- [ ] **ChatServiceTest.java** - 3 issues
  - [ ] Line 70: Change `.sentAt()` to `.createdAt()`
  - [ ] Line 80: Change `.sentAt()` to `.createdAt()`
  - [ ] Line 166-170: Add `.limit(50).offset(0)` to builder

- [ ] **MessageServiceTest.java** - 1 issue
  - [ ] Line 79: Change `.sentAt()` to `.createdAt()`

### Priority 2 - High:
- [ ] **UserServiceTest.java** - 2 issues
  - [ ] Line 62: Remove `.toString()` from userId
  - [ ] Line 82: Update assertion to compare UUID to UUID
  - [ ] Line 115: Remove `.toString()` from userId

- [ ] **PreferenceServiceTest.java** - 4 issues
  - [ ] Line 71: Remove `.toString()` from id
  - [ ] Line 72: Remove `.toString()` from userId
  - [ ] Line 131: Remove `.toString()` from id
  - [ ] Line 132: Remove `.toString()` from userId

### Priority 3 - Medium:
- [ ] **RecommendationServiceTest.java** - 1 issue
  - [ ] Line 215: Review service implementation to verify all response fields are set
  - [ ] Update test if service needs modification

---

## DTO Reference

### MessageResponse (Current Structure)
```java
private UUID id;
private UUID conversationId;
private UUID senderId;
private String senderName;
private String content;
private MessageStatus status;
private Instant createdAt;      // ← Changed from sentAt
private Instant readAt;          // ← deliveredAt was removed
```

### UserResponse (Current Structure)
```java
private UUID id;                 // ← Changed from String
private String email;
private String username;
private String firstName;
private String lastName;
// ... other fields
```

### PreferencesResponse (Current Structure)
```java
private UUID id;                 // ← Changed from String
private UUID userId;             // ← Changed from String
private Integer minAge;
// ... other fields
```

### MessageListResponse (Current Structure)
```java
private UUID conversationId;
private List<MessageResponse> messages;
private int total;
private int limit;               // ← Required field
private int offset;              // ← Required field
private boolean hasMore;
```

### ConversationsListResponse (Current Structure)
```java
private List<ConversationResponse> conversations;
private int total;
private int limit;               // ← Required field
private int offset;              // ← Required field
private boolean hasMore;         // ← Required field
```

### RecommendationListResponse (Current Structure)
```java
private List<RecommendationResponse> recommendations;
private long total;
private int limit;               // ← Required field
private int offset;              // ← Required field
private boolean hasMore;         // ← Required field
private Instant generatedAt;
```

---

## Next Steps

1. **Fix all Priority 1 issues** - These prevent compilation
2. **Run tests** - Verify MessageMapper, ChatService, MessageService tests pass
3. **Fix all Priority 2 issues** - These cause assertion failures
4. **Run tests** - Verify UserService and PreferenceService tests pass
5. **Review Priority 3 issues** - Verify recommendation service constructs responses correctly
6. **Full test suite** - Run `mvn clean test` to verify no regressions
7. **Code review** - Ensure fixes align with DTO structure documentation

---

## Related Files for Reference

- DTO Definitions: `backend/*/src/main/java/com/dating/*/dto/response/`
- Chat Service DTOs: `/home/user/POC_Dating/backend/chat-service/src/main/java/com/dating/chat/dto/response/`
- User Service DTOs: `/home/user/POC_Dating/backend/user-service/src/main/java/com/dating/user/dto/response/`
- Recommendation DTOs: `/home/user/POC_Dating/backend/recommendation-service/src/main/java/com/dating/recommendation/dto/response/`

