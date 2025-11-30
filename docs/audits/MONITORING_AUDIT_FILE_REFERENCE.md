# Monitoring Audit - File Reference Guide

**Date:** 2025-11-18
**Purpose:** Quick reference for all monitoring-related files and their locations

---

## WORKING IMPLEMENTATIONS

### 1. RequestLoggingFilter - Vaadin UI Service ✓

**File:** `/home/user/POC_Dating/backend/vaadin-ui-service/src/main/java/com/dating/ui/config/RequestLoggingFilter.java`

**Key Details:**
- Line 21-24: Proper @Slf4j, @Component, @Order(HIGHEST_PRECEDENCE) annotations
- Line 44: MDC.put("requestId", requestId)
- Line 49: MDC.put("userId", ...) from UserPrincipal
- Line 68: MDC.clear() in finally block
- Line 34: Skips /actuator, /VAADIN, /PUSH

**Status:** ✓ CORRECT

---

### 2. RequestLoggingFilter - Recommendation Service ✓

**File:** `/home/user/POC_Dating/backend/recommendation-service/src/main/java/com/dating/recommendation/config/RequestLoggingFilter.java`

**Key Details:**
- Line 21-24: Proper @Slf4j, @Component, @Order(HIGHEST_PRECEDENCE) annotations
- Line 27: USER_ID_HEADER = "X-User-Id"
- Line 44: MDC.put("requestId", requestId)
- Line 50: MDC.put("userId", userId) from header
- Line 66: MDC.clear() in finally block
- Line 34: Skips /actuator

**Status:** ✓ CORRECT

---

### 3. MetricsConfig - Recommendation Service ✓

**File:** `/home/user/POC_Dating/backend/recommendation-service/src/main/java/com/dating/recommendation/config/MetricsConfig.java`

**Details:**
- Line 17-21: Adds common tags to all metrics (application name, service type)

**Status:** ✓ CORRECT

---

### 4. MetricsConfig - Vaadin UI Service ✓

**File:** `/home/user/POC_Dating/backend/vaadin-ui-service/src/main/java/com/dating/ui/config/MetricsConfig.java`

**Details:**
- Line 17-21: Adds common tags to all metrics

**Status:** ✓ CORRECT

---

### 5. Metrics Implementation - Recommendation Service ✓

**File:** `/home/user/POC_Dating/backend/recommendation-service/src/main/java/com/dating/recommendation/service/RecommendationService.java`

**Metrics:**
- Line 119-121: recommendationsGenerated Counter
- Line 123-125: recommendationTimer Timer
- Line 183: Timer.Sample for tracking
- Line 230: recommendationsGenerated.increment()
- Line 229: sample.stop(recommendationTimer)

**Status:** ✓ CORRECT

---

### 6. Metrics Implementation - Vaadin UI (UserService) ✓

**File:** `/home/user/POC_Dating/backend/vaadin-ui-service/src/main/java/com/dating/ui/service/UserService.java`

**Metrics:**
- Line 32-34: ui.login.attempts.total Counter
- Line 35-37: ui.logins.total Counter
- Line 38-40: ui.login.failures.total Counter
- Line 41-43: ui.registrations.total Counter
- Line 44-47: ui.api.call.time Timer
- Line 63: loginAttemptsCounter.increment()
- Line 66: apiCallTimer.record()
- Line 75: loginCounter.increment()

**Status:** ✓ CORRECT

---

### 7. Metrics Implementation - Vaadin UI (MatchService) ✓

**File:** `/home/user/POC_Dating/backend/vaadin-ui-service/src/main/java/com/dating/ui/service/MatchService.java`

**Metrics:**
- Line 34-37: ui.api.call.time Timer (match-service)
- Line 38-40: ui.feed.generation.time Timer
- Line 41-43: ui.swipes.total Counter
- Line 44-46: ui.matches.total Counter
- Line 59: feedGenerationTimer.record()
- Line 75: swipeCounter.increment()
- Line 78: matchCounter.increment()

**Status:** ✓ CORRECT

---

### 8. Metrics Implementation - Vaadin UI (ChatService) ✓

**File:** `/home/user/POC_Dating/backend/vaadin-ui-service/src/main/java/com/dating/ui/service/ChatService.java`

**Metrics:**
- Line 32-35: ui.api.call.time Timer (chat-service)
- Line 36-38: ui.messages.sent.total Counter
- Line 39-41: ui.messages.read.total Counter
- Line 71: messagesReadCounter.increment()
- Line 89: messagesSentCounter.increment()

**Status:** ✓ CORRECT

---

### 9. Metrics Implementation - Vaadin UI (PageViewMetricsService) ✓

**File:** `/home/user/POC_Dating/backend/vaadin-ui-service/src/main/java/com/dating/ui/service/PageViewMetricsService.java`

**Metrics:**
- Line 37-40: ui.page.views.total Counter (with page tag)
- Line 31-32: counter.increment() for page views

**Status:** ✓ CORRECT

---

### 10. Health Indicator - Recommendation Service ✓

**File:** `/home/user/POC_Dating/backend/recommendation-service/src/main/java/com/dating/recommendation/health/RecommendationServiceHealthIndicator.java`

**Details:**
- Line 13-15: @Component, @RequiredArgsConstructor
- Line 20-41: Implements HealthIndicator interface
- Checks: activeScorerCount from ScoreAggregator

**Status:** ✓ CORRECT

---

### 11. Actuator Configuration - User Service ✓

**File:** `/home/user/POC_Dating/backend/user-service/src/main/resources/application.yml`

**Configuration:**
- Line 25: servlet.context-path: /api
- Line 140: base-path: /actuator
- Line 142-143: show-details: always (⚠️ PRODUCTION RISK)
- Line 147-152: Health checks enabled (db, redis, rabbit)
- Line 138-139: Endpoints exposed (health, info, metrics, prometheus)

**Status:** ✓ CORRECT (but with production safety risk)

---

### 12. Actuator Configuration - Recommendation Service ✓

**File:** `/home/user/POC_Dating/backend/recommendation-service/src/main/resources/application.yml`

**Configuration:**
- Line 244: base-path: /actuator
- Line 246-247: show-details: always (⚠️ PRODUCTION RISK)
- Line 251-254: Health checks enabled (db, redis)
- Line 243: Endpoints exposed (health, info, metrics, prometheus)

**Status:** ✓ CORRECT (but with production safety risk)

---

### 13. Actuator Configuration - Vaadin UI Service ✓

**File:** `/home/user/POC_Dating/backend/vaadin-ui-service/src/main/resources/application.yml`

**Configuration:**
- Line 54: base-path: /actuator
- Line 57: show-details: always (⚠️ PRODUCTION RISK)
- Line 61-62: Health checks enabled (redis)
- Line 53: Endpoints exposed (health, info, metrics, prometheus)

**Status:** ✓ CORRECT (but with production safety risk)

---

## MISSING IMPLEMENTATIONS

### 1. RequestLoggingFilter - User Service ❌

**Expected Location:** `backend/user-service/src/main/java/com/dating/user/config/RequestLoggingFilter.java`

**Status:** FILE DOES NOT EXIST

**What's Needed:**
- Copy from Recommendation Service as template
- Use header-based userId extraction (X-User-Id header)
- Should skip /actuator endpoint

---

### 2. MetricsConfig - User Service ❌

**Expected Location:** `backend/user-service/src/main/java/com/dating/user/config/MetricsConfig.java`

**Status:** FILE DOES NOT EXIST

**What's Needed:**
- Copy from Recommendation Service as template
- Adds common tags for all User Service metrics

---

### 3. Health Indicators - User Service ❌

**Expected Location:** `backend/user-service/src/main/java/com/dating/user/health/`

**Status:** FILES DO NOT EXIST

**What's Needed:**
- Custom health indicator for authentication system
- Check JWT provider health
- Check authentication module status

---

### 4. Health Indicators - Vaadin UI Service ❌

**Expected Location:** `backend/vaadin-ui-service/src/main/java/com/dating/ui/health/`

**Status:** FILES DO NOT EXIST

**What's Needed:**
- Custom health indicator for session management
- Check session store (Redis) details
- Check Vaadin framework components

---

### 5. RequestLoggingFilter - Chat Service ❌

**Expected Location:** `backend/chat-service/src/main/java/com/dating/chat/config/RequestLoggingFilter.java`

**Status:** SERVICE IS SKELETON - NO IMPLEMENTATION

**What's Needed:**
- Implement service skeleton first (application.yml, etc.)
- Then add RequestLoggingFilter following Recommendation Service pattern

---

### 6. MetricsConfig - Chat Service ❌

**Expected Location:** `backend/chat-service/src/main/java/com/dating/chat/config/MetricsConfig.java`

**Status:** SERVICE IS SKELETON - NO IMPLEMENTATION

**What's Needed:**
- Implement service skeleton first
- Then add MetricsConfig following template

---

### 7. RequestLoggingFilter - Match Service ❌

**Expected Location:** `backend/match-service/src/main/java/com/dating/match/config/RequestLoggingFilter.java`

**Status:** SERVICE IS SKELETON - NO IMPLEMENTATION

---

### 8. MetricsConfig - Match Service ❌

**Expected Location:** `backend/match-service/src/main/java/com/dating/match/config/MetricsConfig.java`

**Status:** SERVICE IS SKELETON - NO IMPLEMENTATION

---

### 9. RequestLoggingFilter - API Gateway ❌

**Expected Location:** `backend/api-gateway/src/main/java/com/dating/gateway/config/RequestLoggingFilter.java`

**Status:** SERVICE IS SKELETON - NO IMPLEMENTATION

---

### 10. MetricsConfig - API Gateway ❌

**Expected Location:** `backend/api-gateway/src/main/java/com/dating/gateway/config/MetricsConfig.java`

**Status:** SERVICE IS SKELETON - NO IMPLEMENTATION

---

## DOCUMENTATION FILES

### 1. MONITORING.md (Main Documentation)

**File:** `/home/user/POC_Dating/docs/MONITORING.md`

**Issues:**
- Lines 270-275: References non-existent Chat/Match service config files
- Lines 278-282: References non-existent Chat/Match/User MetricsConfig classes
- Lines 284-289: References non-existent RequestLoggingFilter implementations
- **Line 89-97:** User Service metrics marked "When Implemented" (correct)

**What Needs Fixing:**
- Section "Files Reference" (lines 267-289) - Update or move to appendix
- Add note that Chat, Match, API Gateway are "Under Development"
- Document difference in userId extraction (Vaadin vs Services)
- Change show-details from "always" to environment-specific

---

## SKELETON SERVICES (NOT IMPLEMENTED)

### 1. Chat Service

**Location:** `/home/user/POC_Dating/backend/chat-service/`

**Current State:**
- ✓ Dockerfile exists
- ✓ README.md exists
- ✓ pom.xml exists
- ❌ NO src/ directory
- ❌ NO application.yml
- ❌ NO config/ directory
- ❌ NO RequestLoggingFilter
- ❌ NO MetricsConfig

**Port:** 8083

---

### 2. Match Service

**Location:** `/home/user/POC_Dating/backend/match-service/`

**Current State:**
- ✓ Dockerfile exists
- ✓ README.md exists
- ✓ pom.xml exists
- ❌ NO src/ directory
- ❌ NO application.yml
- ❌ NO config/ directory
- ❌ NO RequestLoggingFilter
- ❌ NO MetricsConfig

**Port:** 8082

---

### 3. API Gateway

**Location:** `/home/user/POC_Dating/backend/api-gateway/`

**Current State:**
- ✓ Dockerfile exists
- ✓ README.md exists
- ✓ pom.xml exists
- ❌ NO src/ directory
- ❌ NO application.yml
- ❌ NO config/ directory
- ❌ NO RequestLoggingFilter
- ❌ NO MetricsConfig

**Port:** 8080

---

## SUMMARY TABLE

| Component | Service | File | Status |
|-----------|---------|------|--------|
| RequestLoggingFilter | User | backend/user-service/config/ | ❌ MISSING |
| RequestLoggingFilter | Chat | backend/chat-service/config/ | ❌ SKELETON |
| RequestLoggingFilter | Match | backend/match-service/config/ | ❌ SKELETON |
| RequestLoggingFilter | Recommendation | backend/recommendation-service/config/ | ✓ EXISTS |
| RequestLoggingFilter | Vaadin UI | backend/vaadin-ui-service/config/ | ✓ EXISTS |
| RequestLoggingFilter | API Gateway | backend/api-gateway/config/ | ❌ SKELETON |
| MetricsConfig | User | backend/user-service/config/ | ❌ MISSING |
| MetricsConfig | Chat | backend/chat-service/config/ | ❌ SKELETON |
| MetricsConfig | Match | backend/match-service/config/ | ❌ SKELETON |
| MetricsConfig | Recommendation | backend/recommendation-service/config/ | ✓ EXISTS |
| MetricsConfig | Vaadin UI | backend/vaadin-ui-service/config/ | ✓ EXISTS |
| MetricsConfig | API Gateway | backend/api-gateway/config/ | ❌ SKELETON |
| Health Indicator | User | backend/user-service/health/ | ❌ MISSING |
| Health Indicator | Chat | backend/chat-service/health/ | ❌ SKELETON |
| Health Indicator | Match | backend/match-service/health/ | ❌ SKELETON |
| Health Indicator | Recommendation | backend/recommendation-service/health/ | ✓ EXISTS |
| Health Indicator | Vaadin UI | backend/vaadin-ui-service/health/ | ❌ MISSING |
| Health Indicator | API Gateway | backend/api-gateway/health/ | ❌ SKELETON |
| Metrics Recording | Recommendation | service/RecommendationService.java | ✓ EXISTS |
| Metrics Recording | Vaadin UI | service/UserService.java | ✓ EXISTS |
| Metrics Recording | Vaadin UI | service/MatchService.java | ✓ EXISTS |
| Metrics Recording | Vaadin UI | service/ChatService.java | ✓ EXISTS |
| Metrics Recording | Vaadin UI | service/PageViewMetricsService.java | ✓ EXISTS |

---

## QUICK COPY TEMPLATES

### Template 1: RequestLoggingFilter for User Service

Copy from: `/home/user/POC_Dating/backend/recommendation-service/src/main/java/com/dating/recommendation/config/RequestLoggingFilter.java`

To: `backend/user-service/src/main/java/com/dating/user/config/RequestLoggingFilter.java`

Changes needed:
- Line 1: Change package to `com.dating.user.config`
- Keep header-based userId extraction (correct for microservice)
- Skip `/actuator` endpoint only

---

### Template 2: MetricsConfig for User Service

Copy from: `/home/user/POC_Dating/backend/recommendation-service/src/main/java/com/dating/recommendation/config/MetricsConfig.java`

To: `backend/user-service/src/main/java/com/dating/user/config/MetricsConfig.java`

Changes needed:
- Line 1: Change package to `com.dating.user.config`
- Line 20: Change service tag from "recommendation" to "user"

---

## CONFIGURATION UPDATES NEEDED

### User Service - application.yml

Change (Line ~142):
```yaml
show-details: always  # Current - RISKY
```

To (environment-specific):
```yaml
show-details: when-authorized  # Production
# OR use Spring profiles:
# dev: always
# prod: when-authorized
```

### Recommendation Service - application.yml

Same change needed at line ~246

### Vaadin UI Service - application.yml

Same change needed at line ~57

---

**Last Updated:** 2025-11-18
**Document Version:** 1.0
