# Security Fixes Implementation Checklist

**Start Date:** [Today]  
**Target Completion:** Week 1 for CRITICAL, Week 2-3 for HIGH/MEDIUM  

---

## 🚨 CRITICAL FIXES (Week 1: Days 1-3, Est. 8 hours)

### Fix #1: Validate X-User-Id Header (2-3 hours)
**Priority:** MUST DO FIRST - Highest security risk

**Files to Modify:**
- [ ] `backend/match-service/src/main/java/com/dating/match/config/SecurityConfig.java`
- [ ] `backend/chat-service/src/main/java/com/dating/chat/config/SecurityConfig.java`
- [ ] `backend/recommendation-service/src/main/java/com/dating/recommendation/config/SecurityConfig.java`

**Steps:**
```
1. Create JwtAuthenticationFilter in each downstream service
   (copy from user-service with modifications)

2. Add to SecurityConfig:
   .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

3. Update endpoints to require authentication:
   .requestMatchers("/api/**").authenticated()

4. Test with invalid/missing X-User-Id header - should get 401

5. Network test: Direct call to match service without going through gateway
   Should be denied
```

**Validation:**
- [ ] Invalid X-User-Id header → 401 Unauthorized
- [ ] Missing Authorization header → 401 Unauthorized
- [ ] Direct service call (no gateway) → 401 Unauthorized
- [ ] Valid JWT through gateway → Allowed

---

### Fix #2: Fix Security Configuration (1-2 hours)

**Match Service** (`backend/match-service/src/main/java/com/dating/match/config/SecurityConfig.java`)
```diff
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/actuator/**").permitAll()
-     .anyRequest().permitAll()
+     .anyRequest().authenticated()
  )
```

**Chat Service** (`backend/chat-service/src/main/java/com/dating/chat/config/SecurityConfig.java`)
```diff
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/actuator/**").permitAll()
-     .requestMatchers("/ws/**").permitAll()
-     .anyRequest().permitAll()
+     .requestMatchers("/ws/**").authenticated()
+     .anyRequest().authenticated()
  )
```

**Recommendation Service** (`backend/recommendation-service/src/main/java/com/dating/recommendation/config/SecurityConfig.java`)
```diff
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/actuator/**").permitAll()
      .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
-     .requestMatchers("/api/**").permitAll()
+     .requestMatchers("/api/**").authenticated()
-     .anyRequest().authenticated()
+     .anyRequest().authenticated()
  )
```

**Validation:**
- [ ] All three services have authenticated requirement for `/api/**`
- [ ] Actuator endpoints still publicly accessible
- [ ] Tests pass with 401 for unauthenticated requests

---

### Fix #3: Implement Access Token Blacklist (4-6 hours)

**Create New Service: TokenBlacklistService**
```
File: backend/api-gateway/src/main/java/com/dating/gateway/security/TokenBlacklistService.java
```

**Tasks:**
- [ ] Create service to store revoked tokens in Redis with TTL
- [ ] Add method to blacklist access token: `blacklistAccessToken(token, expirationSeconds)`
- [ ] Add method to check if token is blacklisted: `isTokenBlacklisted(token)`
- [ ] Extract JWT ID (jti) or user ID as cache key

**Modify JwtAuthenticationFilter (API Gateway)**
```
File: backend/api-gateway/src/main/java/com/dating/gateway/filter/JwtAuthenticationFilter.java
```

**Tasks:**
- [ ] Check token blacklist before allowing request
- [ ] Return 401 if token is blacklisted
- [ ] Log blacklisted token attempts

**Modify AuthService (User Service)**
```
File: backend/user-service/src/main/java/com/dating/user/service/AuthService.java
```

**Tasks:**
- [ ] On logout: Call `tokenBlacklistService.blacklistAccessToken(currentToken, expirationSeconds)`
- [ ] On refresh: Blacklist old access token before issuing new one

**Add Endpoint (User Service)**
```
File: backend/user-service/src/main/java/com/dating/user/controller/AuthController.java
```

**Tasks:**
- [ ] Modify logout endpoint to accept current access token
- [ ] Store token in Authorization header for logout to read

**Validation Tests:**
- [ ] Token is blacklisted on logout
- [ ] Blacklisted token rejected with 401
- [ ] Token removed from blacklist after expiration
- [ ] Refresh token blacklists old access token

---

### Fix #4: Require JWT_SECRET Environment Variable (0.5 hour)

**File:** `backend/user-service/src/main/java/com/dating/user/config/JwtConfig.java`

**Current Code:**
```java
private String secret = "your-256-bit-secret-key-for-jwt-token-signing-must-be-long-enough";
```

**Change To:**
```java
@NotBlank(message = "JWT secret must be configured via JWT_SECRET environment variable")
private String secret;  // No default - fail fast if missing
```

**Also Update:**
- [ ] `backend/api-gateway/src/main/java/com/dating/gateway/config/JwtConfig.java`
- [ ] Add `@NotBlank` annotation to secret field

**Validation:**
- [ ] Application fails to start if JWT_SECRET not set
- [ ] Application starts with valid JWT_SECRET env var
- [ ] No hardcoded secret visible in running instance

---

## 🔴 HIGH PRIORITY FIXES (Week 1-2: Days 3-5, Est. 12 hours)

### Fix #5: Implement Auth Endpoint Rate Limiting (2-3 hours)

**Files to Modify:**
- [ ] `backend/api-gateway/src/main/resources/application.yml`
- [ ] `backend/api-gateway/src/main/java/com/dating/gateway/filter/RateLimitFilter.java`

**Implementation:**
1. Create separate rate limit tracking for auth endpoints
2. Reduce limits: login (5/min), register (3/min), refresh (10/min)
3. Add exponential backoff after failures
4. Optional: Lock account after N failed attempts

**Validation:**
- [ ] Login endpoint: 5 attempts/min per IP
- [ ] Register endpoint: 3 attempts/min per IP
- [ ] Brute force attack blocked after threshold

---

### Fix #6: Fix CORS Configuration (1 hour)

**File:** `backend/api-gateway/src/main/java/com/dating/gateway/config/CorsConfig.java`

**Option A (Recommended for dev):** Remove credentials
```java
corsConfig.setAllowCredentials(false);  // Don't send cookies cross-origin
```

**Option B (For production with sessions):** Restrict origins
```java
corsConfig.setAllowedOrigins(List.of("https://yourdomain.com"));  // Single origin
corsConfig.setAllowCredentials(true);  // Only with restricted origin
```

**Current Issue:**
```java
corsConfig.setAllowedOrigins(List.of("http://localhost:8090", "http://localhost:3000"));
corsConfig.setAllowCredentials(true);  // ❌ Dangerous with multiple origins
```

**Validation:**
- [ ] CORS allows only intended origins
- [ ] Credentials only sent to trusted origins
- [ ] Preflight requests still work (OPTIONS)

---

### Fix #7: Authenticate WebSocket Connections (2 hours)

**File:** `backend/chat-service/src/main/java/com/dating/chat/config/SecurityConfig.java`

**Change:**
```java
.requestMatchers("/ws/**").authenticated()  // Require JWT validation
```

**Create WebSocket Handler:**
```
File: backend/chat-service/src/main/java/com/dating/chat/config/WebSocketAuthHandler.java
```

**Tasks:**
- [ ] Extract JWT token from WebSocket handshake headers
- [ ] Validate token using JwtTokenProvider
- [ ] Extract user ID and pass to WebSocket handler
- [ ] Reject connection if invalid token

**Validation:**
- [ ] WebSocket connection without token → rejected
- [ ] WebSocket connection with valid token → accepted
- [ ] Invalid token → connection refused

---

### Fix #8: Improve Rate Limiting Fallback (1 hour)

**File:** `backend/api-gateway/src/main/java/com/dating/gateway/filter/RateLimitFilter.java`

**Current (Dangerous):**
```java
.onErrorResume(e -> {
    return Mono.just(true);  // Allow all if Redis down
})
```

**Change To:**
```java
.onErrorResume(e -> {
    log.error("Rate limiting unavailable: {}", e.getMessage());
    return Mono.just(false);  // Deny if rate limiting unavailable (secure)
    // OR use in-memory fallback:
    // return inMemoryRateLimiter.checkRateLimit(key, limit);
})
```

**Validation:**
- [ ] Rate limiting works normally
- [ ] Redis failure → requests denied (not allowed)
- [ ] No way to bypass rate limiting by taking down Redis

---

### Fix #9: Configure HTTPS Enforcement (1-2 hours)

**Production Profile:** `backend/*/src/main/resources/application.yml`

**Add to prod profile:**
```yaml
spring:
  config:
    activate:
      on-profile: prod

server:
  ssl:
    enabled: true
    key-store: ${SSL_KEYSTORE_PATH}
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
  http2:
    enabled: true

# Optional: HTTP to HTTPS redirect filter
```

**Create HTTPS Filter** (optional):
```
File: backend/api-gateway/src/main/java/com/dating/gateway/filter/HttpsRedirectFilter.java
```

**Validation:**
- [ ] Dev environment: HTTP works
- [ ] Prod environment: HTTPS required
- [ ] HTTP requests redirected to HTTPS
- [ ] Token transmitted over HTTPS only

---

### Fix #10: Disable Swagger in Production (1 hour)

**Files to Modify:**
- [ ] `backend/recommendation-service/src/main/resources/application.yml`
- [ ] `backend/user-service/src/main/resources/application.yml` (if present)

**Add Swagger Config:**
```
File: backend/*/src/main/java/com/dating/*/config/SwaggerConfig.java
```

**Implementation:**
```java
@Configuration
public class SwaggerConfig {
    @Bean
    public Docket api(@Value("${app.environment:dev}") String environment) {
        boolean enabled = !environment.equals("prod");
        return new Docket(DocumentationType.SWAGGER_2)
            .enable(enabled);  // Disabled in production
    }
}
```

**Validation:**
- [ ] Dev: Swagger accessible at `/swagger-ui.html`
- [ ] Prod: Swagger returns 404 Not Found
- [ ] API still works, just documentation hidden

---

## 🟡 MEDIUM PRIORITY FIXES (Week 2-3: Days 5-7, Est. 10 hours)

### Fix #11: Add Login Attempt Lockout (2 hours)
- [ ] Create LoginAttemptService with Redis tracking
- [ ] Lock account after 5 failed attempts
- [ ] 30-minute lockout duration

### Fix #12: Enhance JWT Claim Validation (1 hour)
- [ ] Add issuer validation
- [ ] Add subject (user ID) validation
- [ ] Add custom claim checks

### Fix #13: Implement Input Sanitization (2 hours)
- [ ] Create InputSanitizer component
- [ ] Sanitize text fields (bio, comments)
- [ ] Prevent XSS in user-generated content

### Fix #14: Add Security Headers (1.5 hours)
- [ ] CSP header configuration
- [ ] X-Frame-Options (DENY)
- [ ] X-Content-Type-Options (nosniff)
- [ ] HSTS (Strict-Transport-Security)

### Fix #15: Implement Audit Logging (2 hours)
- [ ] Create AuditLog entity
- [ ] Log auth events (login, logout, failures)
- [ ] Include IP address, user agent
- [ ] Store in database with timestamps

### Fix #16: Remove Password Defaults (0.5 hour)
- [ ] Remove default database password
- [ ] Remove default JWT secret
- [ ] Fail fast if not configured

### Fix #17: Complete Refresh Token Validation (1 hour)
- [ ] Add hash comparison in validateRefreshToken
- [ ] Verify token exists in database
- [ ] Check revocation status

---

## 📋 Testing Requirements

### After Each Fix
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] No new security warnings from IDE

### After All CRITICAL Fixes
- [ ] Full auth flow test (register → login → refresh → logout)
- [ ] Service-to-service communication test
- [ ] Rate limiting verification
- [ ] Token expiration verification

### Before External Testing
- [ ] OWASP ZAP scan (high severity issues fixed)
- [ ] Manual security test of auth endpoints
- [ ] Load testing with rate limiting
- [ ] Token blacklist verification

### Before Production Deployment
- [ ] All unit/integration tests pass
- [ ] Security code review passed
- [ ] OWASP ZAP scan clean
- [ ] Penetration testing completed
- [ ] Security headers verified
- [ ] HTTPS working
- [ ] Monitoring/alerting active

---

## 📊 Progress Tracking

### Week 1 Targets (Days 1-3)
```
Day 1: Fixes #1, #2, #4
Day 2: Fix #3 (first phase)
Day 3: Fix #3 (testing), preliminary testing
```

### Week 2 Targets (Days 4-10)
```
Days 4-5: Fixes #5, #6, #7
Days 6-7: Fixes #8, #9, #10
Days 8-10: MEDIUM priority fixes, comprehensive testing
```

### Week 3 Targets (Days 11+)
```
Days 11-12: Complete remaining fixes
Days 13-14: Full security test suite
Days 15: Staging deployment readiness
```

---

## 🔗 Related Files

**Configuration:**
- `backend/api-gateway/src/main/resources/application.yml`
- `backend/user-service/src/main/resources/application.yml`

**Security Components:**
- `backend/api-gateway/src/main/java/com/dating/gateway/security/`
- `backend/api-gateway/src/main/java/com/dating/gateway/filter/`
- `backend/user-service/src/main/java/com/dating/user/security/`

**Tests:**
- `backend/api-gateway/src/test/java/com/dating/gateway/security/`
- `backend/user-service/src/test/java/com/dating/user/security/`

**Documentation:**
- `/CLAUDE.md` - Project guidelines
- `/.claude/ARCHITECTURE_PATTERNS.md` - JWT flow details
- `/.claude/CODE_PATTERNS.md` - Coding standards

---

**Last Updated:** November 18, 2025  
**Review Frequency:** Daily during remediation
