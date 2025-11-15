# CLAUDE.md - AI Assistant Guide for POC Dating Application

**Purpose:** High-level context and critical patterns for producing quality code in this codebase.

**Last Updated:** 2025-11-15
**Status:** Active Development - Vaadin UI Implementation Phase

---

## 🎯 Project Context & Goals

### What is This Project?
A proof-of-concept dating application demonstrating **enterprise-quality Java microservices** with:
- **100% Java stack** (backend + frontend)
- **Vaadin 24.3** for web UI (server-side rendering, no React/TypeScript)
- **Spring Boot 3.2.0** microservices architecture
- **PostgreSQL + Redis + RabbitMQ** data layer
- **3-week MVP timeline** (drives all technical decisions)

### Critical Decision: Why Vaadin Over React?
**The team chose pure Java (Vaadin) to:**
- ✅ **Leverage existing expertise** - Team knows Java deeply, not React/TypeScript
- ✅ **Eliminate context switching** - Same language for frontend + backend
- ✅ **Maintain type safety** - End-to-end compile-time checking
- ✅ **Ship faster** - Vaadin CRUD screens in minutes vs React hours
- ⚠️ **Trade-off accepted:** Server-side rendering load vs SPA performance

**Implication for AI assistants:** Never suggest React/Angular/Vue. Use Vaadin patterns for all UI code.

📖 **Deep rationale:** `docs/FRONTEND_OPTIONS_ANALYSIS.md`

---

## 🏗️ Architecture Mental Model

### Service Topology
```
Browser → Vaadin UI (8090) → API Gateway (8080) → Microservices
                                    ↓
              User(8081) | Match(8082) | Chat(8083) | Recommend(8084)
                                    ↓
                    PostgreSQL + Redis + RabbitMQ
```

### Service Responsibilities (Critical to understand)
| Service | Port | **Core Responsibility** | **Key Pattern** |
|---------|------|------------------------|-----------------|
| **Vaadin UI** | 8090 | Web interface (Java only) | Server-side sessions in Redis |
| **API Gateway** | 8080 | JWT validation, routing | **Validates ALL tokens** |
| **User Service** | 8081 | Auth, profiles | **JWT generation happens HERE** |
| **Match Service** | 8082 | Swipes, feed generation | **Heavy caching (24h TTL)** |
| **Chat Service** | 8083 | Real-time messaging | **WebSocket + RabbitMQ pub/sub** |
| **Recommendation** | 8084 | ML scoring | **Pre-computed recommendations** |

### Technology Stack (Non-Negotiable)
- **Java 21** (use modern features: records, pattern matching, virtual threads)
- **Spring Boot 3.2.0** (not 2.x - different security model)
- **Maven** (not Gradle - team decision)
- **JUnit 5 + Mockito** (testing standard)
- **Vaadin 24.3** (not React - critical!)

---

## 💎 Code Quality Standards

### 1. Naming Conventions (Enforced)
```
✅ Classes:     UserService, MatchController (PascalCase)
✅ Methods:     getUserById, createMatch (camelCase)
✅ Constants:   MAX_SWIPES_PER_DAY (UPPER_SNAKE_CASE)
✅ DTOs:        UserRegistrationRequest, UserResponse (Request/Response suffix)
✅ Packages:    com.dating.user.service (lowercase, no underscores)
```

### 2. Project Structure (Must Follow)
```
backend/{service}/src/main/java/com/dating/{service}/
├── Application.java           # Spring Boot entry point
├── config/                    # @Configuration classes
├── controller/                # @RestController (REST endpoints)
├── service/                   # @Service (business logic)
├── repository/                # @Repository (JPA interfaces)
├── model/                     # @Entity (JPA entities)
├── dto/                       # Request/Response POJOs
├── exception/                 # Custom exceptions
└── util/                      # Helper classes
```

### 3. Dependency Injection (Critical Pattern)
```java
// ✅ ALWAYS use constructor injection (required by this codebase)
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class UserService {
    private final UserRepository userRepository;  // final = immutable
    private final PasswordEncoder passwordEncoder;
}

// ❌ NEVER use field injection (causes circular dependency issues)
@Autowired private UserRepository userRepository;  // DON'T DO THIS
```

### 4. Transaction Management
```java
// ✅ Use @Transactional for all write operations
@Service
@RequiredArgsConstructor
public class UserService {

    @Transactional  // Ensures atomic database operations
    public UserResponse register(UserRegistrationRequest request) {
        User user = createUser(request);
        userRepository.save(user);
        publishEvent(new UserRegisteredEvent(user.getId()));
        return mapToResponse(user);
    }
}
```

### 5. Exception Handling (Global Pattern)
```java
// ✅ All services use custom exceptions + @ControllerAdvice
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }
}
```

### 6. Validation (Required)
```java
// ✅ Always validate input with @Valid + Bean Validation
@PostMapping("/auth/register")
public ResponseEntity<UserResponse> register(
        @Valid @RequestBody UserRegistrationRequest request) {  // @Valid triggers validation
    // If validation fails, returns 400 Bad Request automatically
}

// DTO with validation annotations
public record UserRegistrationRequest(
    @NotBlank @Email String email,
    @Size(min = 3, max = 50) String username,
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$") String password
) {}
```

---

## 🔑 Critical Architectural Patterns

### 1. Caching Strategy (Performance-Critical)
```java
// Redis caching with strict TTL hierarchy:
// - User profiles: 1 hour (frequently updated)
// - Match feeds: 24 hours (expensive to compute)
// - Recommendations: 24 hours (ML-generated)
// - Sessions: 30 minutes (security)

@Cacheable(value = "users", key = "#userId")  // Auto-caches
public User getUserById(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));
}

@CacheEvict(value = "users", key = "#userId")  // Auto-invalidates
public void updateUser(UUID userId, UpdateUserRequest request) {
    // Update logic
}
```

📖 **Deep dive:** `.claude/ARCHITECTURE_PATTERNS.md` (Event-driven, JWT, Real-time messaging)

### 2. JWT Authentication Flow (Security-Critical)
```
CRITICAL: Only User Service generates JWTs. API Gateway validates them.
Flow: Login → User Service → JWT (15min) + Refresh Token (7d) → Redis blacklist
Every request: API Gateway validates JWT → extracts userId → X-User-Id header
Services trust X-User-Id header (already validated by gateway)
```

### 3. Event-Driven Communication (Decoupling Pattern)
```java
// Services communicate via RabbitMQ events, not direct HTTP calls
// Example: User registration triggers multiple downstream actions

// User Service publishes
rabbitTemplate.convertAndSend("user.exchange", "user.registered", event);

// Match Service consumes
@RabbitListener(queues = "match.user.registered.queue")
public void handleUserRegistered(UserRegisteredEvent event) {
    initializeSwipeHistory(event.getUserId());
}
```

### 4. Database Indexing (Performance-Critical)
```sql
-- High-volume tables MUST have composite indexes for common queries
CREATE INDEX idx_swipes_user_time ON swipes(user_id, created_at);  -- Feed queries
CREATE INDEX idx_messages_match_time ON messages(match_id, created_at);  -- Chat history
CREATE UNIQUE INDEX idx_matches_pair ON matches(user1_id, user2_id);  -- Duplicate prevention
```

---

## 🛠️ Common Tasks (Quick Reference)

### Add a REST Endpoint
```java
// 1. Controller: @PostMapping("/api/users/...")
// 2. Service: @Transactional business logic
// 3. Repository: JPA query if needed
// 4. Tests: Unit + Integration
// 5. Update docs/API-SPECIFICATION.md
```
📖 **Full examples:** `.claude/CODE_PATTERNS.md` (REST Controllers, Services, Repositories)

### Add a Vaadin View
```java
@Route(value = "myview", layout = MainLayout.class)
@PageTitle("My View | POC Dating")
public class MyView extends VerticalLayout {
    public MyView(MyService service) {
        // Vaadin components (Button, TextField, Grid, etc.)
        add(new H2("Title"), createForm());
    }
}
```
📖 **Vaadin patterns:** `.claude/CODE_PATTERNS.md` (Vaadin Views section)

### Debug a Service
```bash
# 1. Check logs:     docker-compose logs -f user-service
# 2. Check health:   curl localhost:8081/actuator/health
# 3. Check database: docker exec -it dating_postgres psql -U dating_user -d dating_db
# 4. Check Redis:    docker exec -it dating_redis redis-cli
```
📖 **Full troubleshooting:** `.claude/TROUBLESHOOTING.md`

### Run Tests
```bash
mvn test                          # All tests
mvn test -Dtest=UserServiceTest  # Specific class
mvn clean test jacoco:report     # With coverage (target/site/jacoco/index.html)
```
📖 **Testing patterns:** `.claude/OPERATIONS.md` (Testing Strategy section)

---

## ⚡ Performance & Security Critical Rules

### Performance Targets (P95)
- **API response:** <500ms
- **Feed generation:** <1s (cache hit <100ms)
- **Message delivery:** <100ms (WebSocket)
- **Database query:** <100ms (enforce with indexes)

### Security Non-Negotiables
- ✅ **BCrypt password hashing** (12 rounds minimum)
- ✅ **JWT expiry: 15 minutes** (refresh token: 7 days)
- ✅ **Never log passwords, tokens, or PII**
- ✅ **All inputs validated** with @Valid + Bean Validation
- ✅ **SQL injection impossible** (JPA parameterized queries only)
- ✅ **XSS prevention** (Vaadin auto-escapes HTML)

---

## 🚨 Common Pitfalls (Critical to Avoid)

### 1. Circular Dependencies
```
❌ SYMPTOM: BeanCurrentlyInCreationException on startup
✅ FIX: Use constructor injection + event-driven communication (not direct service calls)
```

### 2. Cache Invalidation
```
❌ SYMPTOM: Stale data after update
✅ FIX: Add @CacheEvict annotation to all update/delete methods
```

### 3. N+1 Query Problem
```
❌ SYMPTOM: 100 queries for 100 users (1 + 100 = N+1)
✅ FIX: Use @EntityGraph or JOIN FETCH in JPQL
```

### 4. JWT Token Expiration
```
❌ SYMPTOM: 401 Unauthorized after 15 minutes
✅ FIX: Implement token refresh endpoint (already in User Service)
```

📖 **Full troubleshooting guide:** `.claude/TROUBLESHOOTING.md`

---

## 📚 Documentation Progressive Disclosure

### Level 1: Orientation (Start Here)
- **This file (CLAUDE.md)** - High-level context and critical patterns

### Level 2: Implementation Guides
- **`.claude/CODE_PATTERNS.md`** - REST controllers, services, repositories, Vaadin views, entities, DTOs, testing
- **`.claude/DEV_GUIDE.md`** - Build commands, IDE setup, git workflow, database management, debugging

### Level 3: Deep Dives
- **`.claude/ARCHITECTURE_PATTERNS.md`** - Event-driven, caching, JWT flow, feed algorithm, real-time messaging, database schema
- **`.claude/OPERATIONS.md`** - Testing strategy, deployment, security implementation, performance optimization, monitoring
- **`.claude/TROUBLESHOOTING.md`** - Common pitfalls, debugging workflows, service-specific issues, emergency procedures

### Level 4: Project Documentation
- **`docs/ARCHITECTURE.md`** - System design and service interactions
- **`docs/API-SPECIFICATION.md`** - REST API contracts
- **`docs/DATABASE-SCHEMA.md`** - Complete database design
- **`docs/VAADIN_IMPLEMENTATION.md`** - Vaadin setup and development guide

---

## 🎯 AI Assistant Best Practices

### DO (High Priority)
1. **Read existing code first** - Search for similar patterns before implementing
2. **Follow Java 21 conventions** - Use records, pattern matching, modern syntax
3. **Use Lombok** - @Data, @RequiredArgsConstructor, @Slf4j reduce boilerplate
4. **Write tests** - 70%+ coverage expected (unit + integration)
5. **Update docs** - Change API? Update `docs/API-SPECIFICATION.md`
6. **Validate inputs** - Always use @Valid with Bean Validation
7. **Use constructor injection** - Final fields, no @Autowired on fields
8. **Add indexes** - Every foreign key, composite indexes for common queries
9. **Cache aggressively** - Use @Cacheable for reads, @CacheEvict for writes
10. **Log meaningfully** - Use @Slf4j, log.info/warn/error (never System.out)

### DON'T (Critical to Avoid)
1. ❌ **Suggest React/Angular/Vue** - This is a Vaadin project (Java UI)
2. ❌ **Use field injection** - Causes circular dependencies
3. ❌ **Skip @Transactional** - Write operations must be atomic
4. ❌ **Hardcode configuration** - Use .env or application.yml
5. ❌ **Break API contracts** - Version endpoints if breaking changes needed
6. ❌ **Commit secrets** - Never commit .env, passwords, keys
7. ❌ **Bypass validation** - Every DTO must have @Valid annotations
8. ❌ **Create N+1 queries** - Use JOIN FETCH or @EntityGraph
9. ❌ **Log sensitive data** - No passwords, tokens, or PII in logs
10. ❌ **Skip tests** - All new code requires unit tests minimum

---

## 🚀 Quick Start Commands

```bash
# Full setup (first time)
cp .env.example .env && cd backend && mvn clean install && cd .. && docker-compose up -d

# Development mode (services run locally)
docker-compose up postgres redis rabbitmq  # Infrastructure only
cd backend/user-service && mvn spring-boot:run  # In separate terminals

# Build & test
mvn clean install          # Build all services
mvn test                   # Run all tests
mvn clean test jacoco:report  # Coverage report

# Database operations
docker exec -it dating_postgres psql -U dating_user -d dating_db  # Connect to DB
docker-compose down -v && docker-compose up -d  # Reset database (DEV ONLY)

# Debugging
docker-compose logs -f user-service  # View logs
curl localhost:8081/actuator/health  # Health check
```

📖 **Full dev guide:** `.claude/DEV_GUIDE.md`

---

## 📊 Testing Standards

### Coverage Requirements
- **Unit tests:** 70%+ code coverage (JaCoCo report)
- **Integration tests:** All critical paths (auth, matching, messaging)
- **UI tests:** Main user flows (Vaadin TestBench)

### Test Pattern (Required)
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @InjectMocks private UserService userService;

    @Test
    void testRegisterUser_Success() {
        // Arrange: Set up mocks
        when(userRepository.save(any())).thenReturn(user);

        // Act: Call method
        UserResponse response = userService.register(request);

        // Assert: Verify behavior
        assertNotNull(response);
        verify(userRepository, times(1)).save(any());
    }
}
```

📖 **Full testing guide:** `.claude/OPERATIONS.md` (Testing Strategy)

---

## 🔄 Git Workflow

### Branching
- `main` - Production
- `develop` - Integration
- `feature/name` - New features
- `bugfix/name` - Bug fixes
- `claude/*` - AI assistant branches

### Commit Messages (Conventional Commits)
```
feat: add user registration endpoint
fix: resolve JWT expiration bug
docs: update API specification
test: add UserService unit tests
chore: update Spring Boot to 3.2.1
```

📖 **Full git guide:** `.claude/DEV_GUIDE.md` (Git Workflow)

---

## 📞 When Stuck

1. **Check logs:** `docker-compose logs -f <service>`
2. **Check health:** `curl localhost:<port>/actuator/health`
3. **Read guides:**
   - Code patterns → `.claude/CODE_PATTERNS.md`
   - Troubleshooting → `.claude/TROUBLESHOOTING.md`
   - Architecture → `.claude/ARCHITECTURE_PATTERNS.md`
4. **Search docs:** `docs/` directory has API specs, database schema, architecture diagrams

---

**Remember:** This is a 3-week MVP demonstrating enterprise Java patterns. Prioritize:
1. **Code quality** over speed (this is a learning project)
2. **Type safety** (100% Java stack)
3. **Testability** (70%+ coverage)
4. **Maintainability** (clear patterns, good docs)

**Last Updated:** 2025-11-15
**Version:** 2.0 (Streamlined with progressive disclosure)
