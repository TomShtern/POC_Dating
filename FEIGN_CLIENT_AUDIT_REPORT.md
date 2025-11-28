# Recommendation Service - Feign Client Audit Report
**Date:** 2025-11-18  
**Auditor:** Claude Code  
**Service:** recommendation-service  
**Status:** CRITICAL ISSUE FOUND

---

## Executive Summary

**CRITICAL COMPILATION ERROR DETECTED** in the recommendation-service. The `UserServiceClient.getCandidates()` method signature was updated in commit `fca681d` to accept individual filter parameters (minAge, maxAge, maxDistance, excludeIds), but the call site in `RecommendationService.java` was NOT updated to match the new signature. This will cause a **compilation failure**.

**Impact:** The recommendation service will NOT compile and deploy.

---

## Detailed Findings

### 1. CRITICAL: getCandidates() Call Mismatch (Line 153)

**File:** `/home/user/POC_Dating/backend/recommendation-service/src/main/java/com/dating/recommendation/service/RecommendationService.java`

**Line 153 - Current Code (BROKEN):**
```java
List<UserProfileDto> candidates = userServiceClient.getCandidates(userId, limit * 3);
```

**Expected Method Signature:**
```java
List<UserProfileDto> getCandidates(
    @PathVariable("userId") UUID userId,
    @RequestParam(value = "minAge", defaultValue = "18") int minAge,
    @RequestParam(value = "maxAge", defaultValue = "100") int maxAge,
    @RequestParam(value = "maxDistance", defaultValue = "100") int maxDistance,
    @RequestParam(value = "excludeIds", required = false) List<UUID> excludeIds);
```

**Problem:**
- The method now requires **5 parameters**: userId, minAge, maxAge, maxDistance, excludeIds
- The call only provides **2 parameters**: userId, limit * 3
- The second parameter `limit * 3` (int) would map to `minAge`, but the method also requires `maxAge` and `maxDistance`
- **Compilation Error Type:** Method signature mismatch
- **Error Message:** `error: method getCandidates in interface UserServiceClient cannot be applied to given types. Required: (UUID, int, int, int, List). Found: (UUID, int)`

**Root Cause:**
The `UserServiceClient.getCandidates()` method signature was changed in commit `fca681d` from:
```java
// OLD (before fca681d)
List<UserProfileDto> getCandidates(
    @PathVariable("userId") UUID userId,
    @RequestParam(value = "limit", defaultValue = "100") int limit);

// NEW (after fca681d)
List<UserProfileDto> getCandidates(
    @PathVariable("userId") UUID userId,
    @RequestParam(value = "minAge", defaultValue = "18") int minAge,
    @RequestParam(value = "maxAge", defaultValue = "100") int maxAge,
    @RequestParam(value = "maxDistance", defaultValue = "100") int maxDistance,
    @RequestParam(value = "excludeIds", required = false) List<UUID> excludeIds);
```

But the call site was not updated.

**Fix Required:**
The call must be updated to pass the new parameters. Since the `sourceUser` (obtained on line 149) contains the user's preferences, those should be used:

```java
// Line 153 - CORRECTED
List<UserProfileDto> candidates = userServiceClient.getCandidates(
    userId,
    sourceUser.getMinAge(),      // User's minimum age preference
    sourceUser.getMaxAge(),       // User's maximum age preference
    sourceUser.getMaxDistanceKm(), // User's max distance preference
    null                           // excludeIds - optional, can be null
);
```

**Alternative (with excludeIds):**
If you want to exclude previously seen candidates:
```java
List<UUID> excludeIds = getExcludedUserIds(userId); // Implement this if needed
List<UserProfileDto> candidates = userServiceClient.getCandidates(
    userId,
    sourceUser.getMinAge(),
    sourceUser.getMaxAge(),
    sourceUser.getMaxDistanceKm(),
    excludeIds
);
```

**Parameters Explanation:**
| Parameter | Type | Source | Purpose |
|-----------|------|--------|---------|
| userId | UUID | Method parameter | Current user requesting recommendations |
| minAge | int | sourceUser.getMinAge() | Minimum age filter from user preferences |
| maxAge | int | sourceUser.getMaxAge() | Maximum age filter from user preferences |
| maxDistance | int | sourceUser.getMaxDistanceKm() | Maximum distance filter from user preferences |
| excludeIds | List<UUID> | null or custom logic | IDs to exclude (e.g., already matched users) |

---

## Verification of Other Feign Client Methods

### Other getCandidates() Usages
**Audit Result:** PASSED - Only one usage found and that's the problematic one identified above.

### Other UserServiceClient Methods in recommendation-service

**1. getUserById() - Line 90, 91, 129, 149** ✅ CORRECT
```java
UserProfileDto sourceUser = userServiceClient.getUserById(userId);
UserProfileDto targetUser = userServiceClient.getUserById(targetUserId);
```
- Method signature: `getUserById(@PathVariable("userId") UUID userId)`
- Usage: Correctly passing 1 parameter (UUID)
- Status: PASS

**2. getUsersByIds() - Line 133, 238** ✅ CORRECT
```java
List<UserProfileDto> candidates = userServiceClient.getUsersByIds(candidateIds);
List<UserProfileDto> users = userServiceClient.getUsersByIds(userIds);
```
- Method signature: `getUsersByIds(@RequestBody List<UUID> userIds)`
- Usage: Correctly passing 1 parameter (List<UUID>)
- Status: PASS

**3. getUserPreferences() - No usages found** ⚠️ UNUSED
- Method is defined but never called in recommendation-service
- Consider if this should be used instead of getUserById() for preference-only queries

---

## Cross-Service Audit

### match-service UserServiceClient
**File:** `/home/user/POC_Dating/backend/match-service/src/main/java/com/dating/match/client/UserServiceClient.java`

**Methods:**
- `getUserById(@PathVariable("userId") UUID userId)` ✅
- `getPreferences(@PathVariable("userId") UUID userId)` ✅

**getCandidates() method:** NOT PRESENT (match-service doesn't use it)

**Usages in match-service:** No calls to UserServiceClient found that would be affected.

**Status:** ✅ PASS - No issues in match-service

---

### vaadin-ui-service UserServiceClient
**File:** `/home/user/POC_Dating/backend/vaadin-ui-service/src/main/java/com/dating/ui/client/UserServiceClient.java`

**Methods:**
- `login(@RequestBody LoginRequest request)` ✅
- `register(@RequestBody RegisterRequest request)` ✅
- `getUser(@PathVariable String userId, @RequestHeader String token)` ✅
- `updateUser(@PathVariable String userId, @RequestBody User user, @RequestHeader String token)` ✅
- `getPreferences(@PathVariable String userId, @RequestHeader String token)` ✅
- `updatePreferences(@PathVariable String userId, @RequestBody User preferences, @RequestHeader String token)` ✅

**getCandidates() method:** NOT PRESENT (Vaadin UI doesn't directly query candidates)

**Status:** ✅ PASS - No issues in vaadin-ui-service

---

## Parameter Type Verification

All parameter types in the `getCandidates()` method are correct:

| Parameter | Declared Type | Actual Usage | Match |
|-----------|---------------|--------------|-------|
| userId | UUID | sourceUser.getId() is UUID | ✅ Yes |
| minAge | int (primitive) | sourceUser.getMinAge() returns int | ✅ Yes |
| maxAge | int (primitive) | sourceUser.getMaxAge() returns int | ✅ Yes |
| maxDistance | int (primitive) | sourceUser.getMaxDistanceKm() returns int | ✅ Yes |
| excludeIds | List<UUID> (nullable) | null or List<UUID> | ✅ Yes |

---

## Recommendations

### Priority: CRITICAL (Blocks Compilation)

1. **Update line 153 in RecommendationService.java** to pass all required parameters to `getCandidates()`:
   ```java
   List<UserProfileDto> candidates = userServiceClient.getCandidates(
       userId,
       sourceUser.getMinAge(),
       sourceUser.getMaxAge(),
       sourceUser.getMaxDistanceKm(),
       null  // excludeIds optional
   );
   ```

2. **Test the updated call**:
   ```bash
   cd /home/user/POC_Dating/backend/recommendation-service
   mvn clean compile
   mvn test
   ```

3. **Consider enhancement**: Add logic to exclude previously matched users:
   ```java
   List<UUID> excludeIds = getExcludedUserIds(userId);
   List<UserProfileDto> candidates = userServiceClient.getCandidates(
       userId,
       sourceUser.getMinAge(),
       sourceUser.getMaxAge(),
       sourceUser.getMaxDistanceKm(),
       excludeIds
   );
   ```

4. **Review commit fca681d**: Ensure all other method signature changes were properly propagated to all call sites.

---

## Summary of Issues

| ID | Severity | Component | Issue | Status |
|----|----------|-----------|-------|--------|
| 1 | CRITICAL | RecommendationService | getCandidates() call missing 3 required parameters | UNFIXED |
| 2 | INFO | RecommendationService | getUserPreferences() method unused | DOCUMENTED |
| 3 | LOW | match-service | No getCandidates() usage (expected) | OK |
| 4 | LOW | vaadin-ui-service | No getCandidates() usage (expected) | OK |

---

## Files Requiring Changes

1. **MUST FIX:**
   - `/home/user/POC_Dating/backend/recommendation-service/src/main/java/com/dating/recommendation/service/RecommendationService.java` (Line 153)

2. **VERIFIED (No Changes Needed):**
   - `/home/user/POC_Dating/backend/recommendation-service/src/main/java/com/dating/recommendation/client/UserServiceClient.java` ✅
   - `/home/user/POC_Dating/backend/match-service/src/main/java/com/dating/match/client/UserServiceClient.java` ✅
   - `/home/user/POC_Dating/backend/vaadin-ui-service/src/main/java/com/dating/ui/client/UserServiceClient.java` ✅

---

**Report Generated:** 2025-11-18  
**Next Review:** After implementing fixes
