# Java Backend Microservices Implementation Prompt

## Context
You are implementing the Java Spring Boot backend for a POC Dating application. The Vaadin UI frontend is being developed by another agent - do not modify anything in `backend/vaadin-ui-service/`. You have **full internet access** to research any issues, Spring Boot patterns, or library documentation.

## ⚠️ CRITICAL: Code Quality Requirements

**WRITE CLEAN, MAINTAINABLE, MODULAR CODE.**

This is non-negotiable. Every file you create must be:
- **MODULAR** - Single responsibility per class, small focused methods (<20 lines), clear boundaries
- **MAINTAINABLE** - Self-documenting names, consistent patterns, easy to modify
- **CLEAN** - No duplication, no dead code, proper error handling

**Modularity Rules:**
```java
// ✅ GOOD: Small, focused methods
public UserResponse register(UserRegistrationRequest request) {
    validateEmailNotTaken(request.email());
    User user = createUserEntity(request);
    User savedUser = userRepository.save(user);
    publishRegistrationEvent(savedUser);
    return mapToResponse(savedUser);
}

// ❌ BAD: Monolithic 100-line methods
public UserResponse register(UserRegistrationRequest request) {
    // 100 lines of mixed concerns...
}
```

**Why This Matters:** Multiple agents are working on this codebase. Modular code enables parallel development, easier testing, and faster debugging.

## Scope
Implement these 5 microservices + common library:
1. **common-lib** - Shared DTOs, utilities, exceptions
2. **user-service** (8081) - Auth, JWT, profiles
3. **api-gateway** (8080) - Routing, JWT validation
4. **match-service** (8082) - Swipes, matches, feed
5. **chat-service** (8083) - Real-time messaging
6. **recommendation-service** (8084) - ML scoring

## Critical Resources
- **Code patterns:** `.claude/CODE_PATTERNS.md` (copy these exactly)
- **API contracts:** `docs/API-SPECIFICATION.md` (implement all endpoints)
- **Architecture:** `.claude/ARCHITECTURE_PATTERNS.md` (JWT flow, events, caching)
- **Database:** `db/init/01-schema.sql` (schema is ready)
- **Reference:** `backend/vaadin-ui-service/` (follow same patterns)

## Implementation Order
```
1. common-lib → 2. user-service → 3. api-gateway → 4. match-service → 5. chat-service → 6. recommendation-service
```

## Per-Service Checklist
For each service, implement in this order:
1. [ ] `pom.xml` with correct dependencies
2. [ ] `Application.java` entry point
3. [ ] `application.yml` configuration
4. [ ] `model/` - JPA entities matching `db/init/01-schema.sql`
5. [ ] `dto/` - Request/Response records with validation
6. [ ] `repository/` - JPA interfaces with custom queries
7. [ ] `service/` - Business logic with @Transactional
8. [ ] `controller/` - REST endpoints matching API spec
9. [ ] `config/` - Security, Redis, RabbitMQ configs
10. [ ] `exception/` - Custom exceptions + GlobalExceptionHandler
11. [ ] Unit tests (70%+ coverage target)
12. [ ] Integration tests for critical paths

## Mandatory Patterns (Non-Negotiable)
```java
// Constructor injection only
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;  // final + constructor
}

// All write operations transactional
@Transactional
public UserResponse register(UserRegistrationRequest request) { }

// All inputs validated
public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request)

// Caching with proper eviction
@Cacheable(value = "users", key = "#userId")
@CacheEvict(value = "users", key = "#userId")
```

## Iteration Loop (Repeat Until Complete)

### Phase 1: Build
```bash
cd backend && mvn clean compile
```
- If compilation fails → fix errors → rebuild
- Use internet to research any dependency or syntax issues

### Phase 2: Test
```bash
mvn test
mvn clean test jacoco:report  # Check target/site/jacoco/index.html
```
- If tests fail → fix implementation → retest
- If coverage < 70% → add more tests → retest

### Phase 3: Integration Verify
```bash
docker-compose up -d
curl localhost:8081/actuator/health  # Each service
```
- If health check fails → check logs → fix → restart
- Test actual API endpoints with curl/Postman

### Phase 4: End-to-End Verify
```bash
# Register user
curl -X POST localhost:8080/api/users/auth/register -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","username":"testuser","password":"Test123!"}'

# Login and get JWT
curl -X POST localhost:8080/api/users/auth/login -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test123!"}'

# Use JWT for protected endpoints
curl -H "Authorization: Bearer <token>" localhost:8080/api/users/profile
```
- If any endpoint fails → debug → fix → retest entire flow

## Success Criteria
- [ ] All 6 projects compile without errors
- [ ] All unit tests pass with 70%+ coverage
- [ ] All endpoints in API-SPECIFICATION.md are implemented
- [ ] JWT auth flow works end-to-end
- [ ] RabbitMQ events fire correctly between services
- [ ] Redis caching works (check with `redis-cli KEYS *`)
- [ ] Health endpoints respond on all services

## When Stuck
1. **Search internet** for Spring Boot 3.2, Java 21, or library-specific solutions
2. **Check logs:** `docker-compose logs -f <service-name>`
3. **Read:** `.claude/TROUBLESHOOTING.md`
4. **Reference:** Copy patterns from `backend/vaadin-ui-service/`

## DO NOT
- Modify `backend/vaadin-ui-service/` (separate agent)
- Use field injection (@Autowired on fields)
- Skip @Transactional on write operations
- Hardcode secrets (use .env)
- Create N+1 queries (use JOIN FETCH)

---
**Iterate until all success criteria are met. Use internet access freely to resolve blockers.**
