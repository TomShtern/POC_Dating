# CLAUDE.md - AI Assistant Guide for POC Dating Application

**Document Purpose:** Guide AI assistants (Claude, GitHub Copilot, etc.) in understanding and working with this codebase effectively.

**Last Updated:** 2025-11-15
**Status:** Active Development - Vaadin UI Implementation Phase

---

## 🎯 Quick Orientation

### What is This Project?
A proof-of-concept dating application built with:
- **Architecture:** Java Spring Boot microservices
- **Frontend:** Vaadin 24.3 (Pure Java, server-side rendering)
- **Data:** PostgreSQL + Redis + RabbitMQ
- **Deployment:** Docker & Docker Compose

### Key Decision: Why Vaadin Instead of React?
The team chose **Vaadin** (pure Java) over React/TypeScript to:
- ✅ Leverage team's Java expertise (3-week MVP timeline)
- ✅ Maintain type safety across entire stack
- ✅ Reduce context switching between languages
- 📋 See `docs/FRONTEND_OPTIONS_ANALYSIS.md` for detailed rationale

### Project Timeline
- **Research Phase:** ✅ Complete (comprehensive competitor analysis)
- **Architecture Phase:** ✅ Complete (microservices designed)
- **Implementation Phase:** 🚧 In Progress (Vaadin UI skeleton implemented)
- **Testing Phase:** ⬜ Not Started

---

## 📁 Repository Structure

```
POC_Dating/
├── backend/                          # All Java microservices
│   ├── pom.xml                      # Parent POM (dependency management)
│   ├── common-library/              # Shared code (DTOs, entities, exceptions)
│   ├── user-service/                # Auth, profiles, preferences (8081)
│   ├── match-service/               # Swipes, matching algorithm (8082)
│   ├── chat-service/                # WebSocket messaging (8083)
│   ├── recommendation-service/      # ML recommendations (8084)
│   ├── api-gateway/                 # Request routing, auth (8080)
│   └── vaadin-ui-service/           # 🆕 Pure Java web UI (8090)
│
├── frontend/                         # ⚠️ DEPRECATED - React reference only
│
├── db/
│   └── init/                        # PostgreSQL schema (01-schema.sql)
│
├── docs/                            # 📚 Comprehensive documentation
│   ├── ARCHITECTURE.md              # System design (Vaadin-updated)
│   ├── DEVELOPMENT.md               # Dev setup & workflow
│   ├── VAADIN_IMPLEMENTATION.md     # Vaadin-specific guide
│   ├── FRONTEND_OPTIONS_ANALYSIS.md # Frontend decision rationale
│   ├── API-SPECIFICATION.md         # REST API contracts
│   ├── DATABASE-SCHEMA.md           # Database design
│   └── DOCUMENT_INDEX.md            # Documentation map
│
├── .env.example                      # Environment template
├── docker-compose.yml                # Local dev orchestration
├── README.md                         # Project overview
└── CLAUDE.md                         # 👈 This file
```

---

## 🏗️ Architecture Overview

### Microservices Structure

```
Web Browser → Vaadin UI (8090) → API Gateway (8080) → Microservices
                                         ↓
                    ┌────────────────────┼────────────────────┐
                    ↓                    ↓                    ↓
              User Service        Match Service        Chat Service
                (8081)              (8082)               (8083)
                    ↓                    ↓                    ↓
               PostgreSQL + Redis + RabbitMQ
```

### Service Responsibilities

| Service | Port | Purpose | Database | Key Features |
|---------|------|---------|----------|--------------|
| **Vaadin UI** | 8090 | Web interface | Redis (sessions) | Pure Java UI, @Push for real-time |
| **API Gateway** | 8080 | Routing, auth | Redis (rate limiting) | JWT validation, circuit breaker |
| **User Service** | 8081 | Auth, profiles | PostgreSQL | Registration, login, profiles |
| **Match Service** | 8082 | Swipes, matches | PostgreSQL | Feed generation, match detection |
| **Chat Service** | 8083 | Messaging | PostgreSQL | WebSocket, real-time messaging |
| **Recommendation** | 8084 | Recommendations | PostgreSQL | Scoring algorithm, caching |

### Technology Stack

**Backend:**
- Java 21 (modern language features)
- Spring Boot 3.2.0 (microservices framework)
- Maven 3.8+ (build tool)
- PostgreSQL 15 (primary database)
- Redis 7 (caching, sessions)
- RabbitMQ 3.12 (event bus)

**Frontend:**
- Vaadin 24.3 (Pure Java web UI)
- Spring Cloud Feign (backend API calls)
- Redis (session storage)
- Lumo Theme (customizable styling)

**Testing:**
- JUnit 5 + Mockito (unit tests)
- TestContainers (integration tests)
- Vaadin TestBench (UI tests)

**DevOps:**
- Docker & Docker Compose
- GitHub Actions (future CI/CD)

---

## 🚀 Development Workflows

### Quick Start

```bash
# 1. Clone repository
git clone <repo-url>
cd POC_Dating

# 2. Setup environment
cp .env.example .env

# 3. Build all services
cd backend
mvn clean install

# 4. Start infrastructure + services
cd ..
docker-compose up -d

# 5. Access application
# Vaadin UI: http://localhost:8090
# API Gateway: http://localhost:8080
```

### Development Mode (without Docker)

```bash
# Terminal 1: Start databases
docker-compose up postgres redis rabbitmq

# Terminal 2: Start backend service
cd backend/user-service
mvn spring-boot:run

# Terminal 3: Start Vaadin UI
cd backend/vaadin-ui-service
mvn spring-boot:run

# Access: http://localhost:8090
```

### Build Commands

```bash
# Build all services
cd backend && mvn clean install

# Build specific service
cd backend/user-service && mvn clean package

# Run tests
mvn test

# Run with coverage
mvn clean test jacoco:report
# View: target/site/jacoco/index.html

# Skip tests (faster builds)
mvn clean install -DskipTests
```

### Database Management

```bash
# Connect to PostgreSQL
docker exec -it dating_postgres psql -U dating_user -d dating_db

# Common psql commands
\dt                 # List tables
\d users            # Describe users table
SELECT * FROM users LIMIT 5;

# Reset database (DEV ONLY - deletes all data!)
docker-compose down -v
docker volume rm poc_dating_postgres_data
docker-compose up -d
```

### Git Workflow

**Branching Strategy:**
- `main` - Production-ready code
- `develop` - Integration branch
- `feature/feature-name` - Feature development
- `bugfix/bug-name` - Bug fixes
- `claude/claude-md-*` - Claude AI branches

**Commit Message Format:**
```
feat: add user registration endpoint
fix: resolve JWT expiration bug
docs: update API specification
style: format code with prettier
refactor: extract duplicate logic
test: add unit tests for UserService
chore: update dependencies
```

**Typical Workflow:**
```bash
# Start from develop
git checkout develop
git pull origin develop

# Create feature branch
git checkout -b feature/my-feature

# Make changes, commit frequently
git add .
git commit -m "feat: implement feature"

# Push and create PR
git push -u origin feature/my-feature
```

---

## 🎨 Code Conventions

### Java Coding Standards

**File Organization:**
```
com.dating.user/
├── UserServiceApplication.java    # Spring Boot main class
├── config/                         # Configuration classes
├── controller/                     # REST controllers
├── service/                        # Business logic
├── repository/                     # JPA repositories
├── model/                          # JPA entities
├── dto/                            # Data Transfer Objects
├── exception/                      # Custom exceptions
└── util/                           # Utility classes
```

**Naming Conventions:**
- **Classes:** PascalCase (UserService, MatchController)
- **Methods:** camelCase (getUserById, createMatch)
- **Constants:** UPPER_SNAKE_CASE (MAX_SWIPES_PER_DAY)
- **Packages:** lowercase (com.dating.user.service)
- **DTOs:** Suffix with "DTO" or "Request/Response" (UserRegistrationRequest)

**REST Controller Pattern:**
```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/auth/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody UserRegistrationRequest request) {
        UserResponse user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
}
```

**Service Layer Pattern:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        log.info("Registering user: {}", request.getEmail());
        // Business logic here
    }
}
```

**Exception Handling:**
```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }
}
```

### Vaadin View Pattern

```java
@Route(value = "swipe", layout = MainLayout.class)
@PageTitle("Swipe | POC Dating")
public class SwipeView extends VerticalLayout {

    private final MatchService matchService;

    public SwipeView(MatchService matchService) {
        this.matchService = matchService;

        // Build UI
        add(createHeader());
        add(createProfileCard());
        add(createActionButtons());
    }

    private Component createProfileCard() {
        // Vaadin component composition
    }
}
```

### Database Conventions

**Entity Pattern:**
```java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_username", columnList = "username")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String username;

    // Timestamps
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
```

**Repository Pattern:**
```java
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.status = :status AND u.createdAt > :date")
    List<User> findActiveUsersSince(@Param("status") String status,
                                     @Param("date") Instant date);
}
```

---

## 🔑 Key Architectural Patterns

### 1. Event-Driven Architecture

**Pattern:** Microservices communicate asynchronously via RabbitMQ events.

**Example:**
```java
// User Service publishes event
@Service
public class UserService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void register(UserRegistrationRequest request) {
        User user = createUser(request);

        // Publish event
        rabbitTemplate.convertAndSend("user.exchange",
            "user.registered",
            new UserRegisteredEvent(user.getId()));
    }
}

// Match Service consumes event
@Component
public class UserEventListener {

    @RabbitListener(queues = "match.user.registered.queue")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("User registered: {}", event.getUserId());
        // Initialize empty swipe history
    }
}
```

### 2. Caching Strategy

**Pattern:** Redis caching with TTL for frequently accessed data.

```java
@Service
public class UserService {

    @Cacheable(value = "users", key = "#userId")
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @CacheEvict(value = "users", key = "#userId")
    public void updateUser(UUID userId, UpdateUserRequest request) {
        // Update logic
    }
}
```

**Cache Keys:**
- `user:{userId}:profile` - TTL: 1 hour
- `match:{userId}:feed` - TTL: 24 hours
- `recommendation:{userId}` - TTL: 24 hours
- `session:{sessionId}` - TTL: 30 minutes

### 3. JWT Authentication Flow

```
1. User submits credentials → User Service
2. Validate credentials (BCrypt password check)
3. Generate JWT (15 min expiry) + Refresh Token (7 days)
4. Store refresh token hash in PostgreSQL
5. Return tokens to client
6. Client includes JWT in Authorization header: "Bearer {token}"
7. API Gateway validates JWT on every request
8. Extract userId from JWT claims, add to X-User-Id header
9. Downstream services trust X-User-Id header
```

### 4. Feed Generation Algorithm

**Location:** `match-service/src/main/java/com/dating/match/service/FeedService.java`

```java
public List<UserProfile> generateFeed(UUID userId) {
    // 1. Get user preferences
    UserPreferences prefs = userService.getPreferences(userId);

    // 2. Apply filters (age, distance, already swiped)
    List<User> candidates = userRepository.findCandidates(
        prefs.getMinAge(), prefs.getMaxAge(),
        prefs.getMaxDistance(), userId);

    // 3. Score each candidate
    List<ScoredProfile> scored = candidates.stream()
        .map(c -> scoreProfile(c, prefs))
        .sorted(Comparator.comparing(ScoredProfile::getScore).reversed())
        .collect(Collectors.toList());

    // 4. Cache top 100 in Redis (24h TTL)
    cacheService.cacheFeed(userId, scored);

    // 5. Return paginated results
    return scored.stream()
        .limit(20)
        .map(ScoredProfile::getProfile)
        .collect(Collectors.toList());
}
```

### 5. Real-Time Messaging Flow

```
1. Client opens ChatView (Vaadin)
2. Vaadin @Push establishes WebSocket/SSE connection
3. User A types message
4. Message sent to Chat Service REST endpoint
5. Chat Service:
   a. Validates sender has permission to message
   b. Stores message in PostgreSQL (status: SENT)
   c. Publishes to RabbitMQ topic
6. RabbitMQ routes to Chat Service instance connected to User B
7. Chat Service pushes message via WebSocket
8. User B's Vaadin UI updates in real-time (ui.access())
9. User B's client sends READ receipt
10. Status updated: SENT → DELIVERED → READ
```

---

## 📊 Database Schema

### Key Tables

**users**
- `id` (UUID, PK)
- `email` (VARCHAR, UNIQUE, INDEX)
- `username` (VARCHAR, UNIQUE, INDEX)
- `password_hash` (VARCHAR)
- `first_name`, `last_name`, `bio`, `date_of_birth`
- `status` (ENUM: ACTIVE, INACTIVE, BANNED)
- `created_at`, `updated_at` (TIMESTAMP)

**swipes** (High-volume table)
- `id` (UUID, PK)
- `user_id` (UUID, FK → users, INDEX)
- `target_user_id` (UUID, FK → users, INDEX)
- `swipe_type` (ENUM: LIKE, PASS, SUPER_LIKE)
- `created_at` (TIMESTAMP, INDEX)
- **Composite Index:** (user_id, created_at)

**matches**
- `id` (UUID, PK)
- `user1_id` (UUID, FK → users)
- `user2_id` (UUID, FK → users)
- `matched_at` (TIMESTAMP)
- `status` (ENUM: ACTIVE, ENDED)
- **Composite Index:** (user1_id, user2_id)

**messages**
- `id` (UUID, PK)
- `match_id` (UUID, FK → matches, INDEX)
- `sender_id` (UUID, FK → users)
- `content` (TEXT)
- `status` (ENUM: SENT, DELIVERED, READ)
- `created_at` (TIMESTAMP)
- **Composite Index:** (match_id, created_at)

**Indexing Strategy:**
- Index all foreign keys
- Composite indexes for common query patterns
- Time-based indexes for sorting/pagination

---

## 🧪 Testing Guidelines

### Unit Test Pattern

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void testRegisterUser_Success() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest(
            "test@example.com", "testuser", "password123");

        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        UserResponse response = userService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }
}
```

### Integration Test Pattern

```java
@SpringBootTest
@Testcontainers
class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("dating_test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testRegisterAndFindUser() {
        // Test with real database
        UserRegistrationRequest request = new UserRegistrationRequest(
            "integration@test.com", "testuser", "password123");

        UserResponse registered = userService.register(request);

        Optional<User> found = userRepository.findByEmail("integration@test.com");
        assertTrue(found.isPresent());
        assertEquals(registered.getId(), found.get().getId());
    }
}
```

### Test Coverage Expectations

- **Unit Tests:** 70%+ code coverage
- **Integration Tests:** Critical paths (auth, matching, messaging)
- **UI Tests:** Vaadin TestBench for main user flows
- **Performance Tests:** Load testing (future)

---

## 🛠️ Common AI Assistant Tasks

### Task: Add a New REST Endpoint

**Steps:**
1. Read the relevant service's controller
2. Add new method following existing patterns
3. Add corresponding service layer method
4. Add repository method if needed
5. Update tests
6. Document in API-SPECIFICATION.md

**Example:**
```java
// 1. Add to UserController.java
@GetMapping("/{id}/stats")
public ResponseEntity<UserStatsResponse> getUserStats(@PathVariable UUID id) {
    UserStatsResponse stats = userService.getUserStats(id);
    return ResponseEntity.ok(stats);
}

// 2. Add to UserService.java
public UserStatsResponse getUserStats(UUID userId) {
    User user = getUserById(userId);
    long matchCount = matchRepository.countByUserId(userId);
    long messageCount = messageRepository.countBySenderId(userId);
    return new UserStatsResponse(matchCount, messageCount);
}

// 3. Add tests
@Test
void testGetUserStats() {
    // Test implementation
}
```

### Task: Add a Database Migration

**Steps:**
1. Create new SQL file in `db/init/` with version number
2. Add ALTER/CREATE statements
3. Test locally
4. Update DATABASE-SCHEMA.md
5. Commit

**Example:**
```sql
-- db/init/02-add-location-columns.sql

ALTER TABLE users
ADD COLUMN location_latitude FLOAT,
ADD COLUMN location_longitude FLOAT;

CREATE INDEX idx_users_location ON users (location_latitude, location_longitude);
```

### Task: Add a New Vaadin View

**Steps:**
1. Create view class in `vaadin-ui-service/src/main/java/com/dating/ui/views/`
2. Add @Route annotation
3. Extend appropriate layout (VerticalLayout, etc.)
4. Create Feign client service if needed
5. Add navigation link to MainLayout
6. Test view rendering

**Example:**
```java
@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Settings | POC Dating")
public class SettingsView extends VerticalLayout {

    private final UserService userService;

    public SettingsView(UserService userService) {
        this.userService = userService;

        add(new H2("Account Settings"));
        add(createNotificationSettings());
        add(createPrivacySettings());
    }
}
```

### Task: Debug a Service

**Commands:**
```bash
# View logs
docker-compose logs -f user-service

# Check service health
curl http://localhost:8081/actuator/health

# Connect to database
docker exec -it dating_postgres psql -U dating_user -d dating_db

# Check Redis cache
docker exec -it dating_redis redis-cli
KEYS user:*
GET user:123:profile

# Check RabbitMQ queues
# Visit: http://localhost:15672 (guest/guest)
```

### Task: Update Dependencies

**Steps:**
1. Update version in `backend/pom.xml` (parent POM)
2. Run `mvn clean install` to verify
3. Run tests to catch breaking changes
4. Update CHANGELOG if significant
5. Commit with message: "chore: update [dependency] to [version]"

---

## 🚨 Common Pitfalls & Solutions

### Pitfall 1: Database Connection Errors

**Symptom:** Service fails to start with connection refused.

**Solution:**
```bash
# Check PostgreSQL is running
docker-compose ps

# Verify credentials in .env
cat .env | grep POSTGRES

# Restart PostgreSQL
docker-compose restart postgres
```

### Pitfall 2: Port Already in Use

**Symptom:** "Address already in use" error.

**Solution:**
```bash
# Find process using port
lsof -i :8081

# Kill process
kill -9 <PID>

# Or change port in docker-compose.yml
```

### Pitfall 3: JWT Token Expiration

**Symptom:** 401 Unauthorized after 15 minutes.

**Solution:**
- Client should automatically refresh tokens
- Check JWT_EXPIRATION_MS in .env
- Implement token refresh endpoint

### Pitfall 4: Cache Invalidation Issues

**Symptom:** Stale data shown after update.

**Solution:**
```java
// Add @CacheEvict annotation
@CacheEvict(value = "users", key = "#userId")
public void updateUser(UUID userId, UpdateUserRequest request) {
    // Update logic
}

// Or clear cache manually
cacheManager.getCache("users").clear();
```

### Pitfall 5: Circular Dependencies

**Symptom:** "BeanCurrentlyInCreationException" on startup.

**Solution:**
- Use constructor injection (not field injection)
- Break circular dependency with events (RabbitMQ)
- Use @Lazy annotation as last resort

---

## 📚 Documentation Map

**Before Starting Any Task:**
1. Read this CLAUDE.md (orientation)
2. Read relevant service README (`backend/{service}/README.md`)
3. Check docs/ directory for specific guides

**Key Documentation Files:**

| File | Purpose | Read When... |
|------|---------|--------------|
| **README.md** | Project overview | First time setup |
| **CLAUDE.md** | AI assistant guide | Starting any AI-assisted task |
| **docs/ARCHITECTURE.md** | System design | Understanding overall system |
| **docs/DEVELOPMENT.md** | Dev workflow | Setting up dev environment |
| **docs/VAADIN_IMPLEMENTATION.md** | Vaadin guide | Working on UI |
| **docs/API-SPECIFICATION.md** | API contracts | Adding/modifying endpoints |
| **docs/DATABASE-SCHEMA.md** | DB design | Working with database |
| **backend/{service}/README.md** | Service details | Working on specific service |

---

## 🎯 AI Assistant Best Practices

### DO:
✅ Read existing code patterns before writing new code
✅ Follow established naming conventions
✅ Add tests for new functionality
✅ Update documentation when changing architecture
✅ Use Lombok annotations (@Data, @RequiredArgsConstructor)
✅ Add clear JavaDoc for complex methods
✅ Use appropriate HTTP status codes
✅ Validate input with @Valid and Bean Validation
✅ Handle exceptions with @ControllerAdvice
✅ Use meaningful commit messages
✅ Check service health endpoints after changes

### DON'T:
❌ Introduce new libraries without discussion
❌ Change database schema without migration scripts
❌ Break existing API contracts without versioning
❌ Commit secrets or credentials
❌ Skip writing tests
❌ Use field injection (use constructor injection)
❌ Hardcode configuration (use .env or application.yml)
❌ Create circular dependencies between services
❌ Bypass security filters
❌ Use blocking calls in async contexts

---

## 🔐 Security Considerations

### Authentication
- JWT tokens with 15-minute expiry
- Refresh tokens with 7-day expiry
- BCrypt password hashing (12 rounds)
- Token blacklist in Redis

### Authorization
- Role-based access control (future)
- Resource-based permissions (users can only access own data)
- API Gateway validates all requests

### Data Protection
- HTTPS in production (TLS 1.3)
- Environment variables for secrets
- Never log sensitive data (passwords, tokens)
- SQL injection prevention (JPA parameterized queries)
- XSS prevention (Vaadin auto-escapes HTML)

---

## 🚀 Performance Optimization

### Caching Strategy
- **User profiles:** 1 hour TTL (frequently accessed)
- **Recommendation feeds:** 24 hours TTL (expensive to compute)
- **Session data:** 30 minutes TTL (active users)
- **Token blacklist:** Token expiry TTL (security)

### Database Optimization
- Proper indexing on frequently queried columns
- Connection pooling (HikariCP)
- Pagination for large result sets
- N+1 query prevention (JPA fetch strategies)

### API Performance
- Response time target: <500ms (P95)
- Feed generation: <1s
- Message delivery: <100ms
- Database queries: <100ms

---

## 📊 Monitoring & Debugging

### Health Checks
```bash
# API Gateway
curl http://localhost:8080/actuator/health

# User Service
curl http://localhost:8081/actuator/health

# Check all services
for port in 8080 8081 8082 8083 8084; do
  echo "Port $port: $(curl -s http://localhost:$port/actuator/health | jq .status)"
done
```

### Logging
```bash
# View service logs
docker-compose logs -f user-service

# Search logs for errors
docker-compose logs user-service | grep ERROR

# Follow logs with filter
docker-compose logs -f user-service | grep "userId=123"
```

### Database Queries
```bash
# Connect to PostgreSQL
docker exec -it dating_postgres psql -U dating_user -d dating_db

# Check slow queries (enable in application.yml)
SELECT * FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;
```

---

## 🔄 Deployment Checklist

Before deploying changes:

- [ ] All tests pass (`mvn test`)
- [ ] Code coverage >70% (`mvn jacoco:report`)
- [ ] No security vulnerabilities (`mvn dependency:check`)
- [ ] Documentation updated
- [ ] Database migrations tested
- [ ] API changes backward compatible
- [ ] Environment variables documented in .env.example
- [ ] Docker images build successfully
- [ ] Integration tests pass
- [ ] Performance benchmarks met

---

## 📞 Getting Help

### Debugging Workflow
1. Check service logs (`docker-compose logs -f <service>`)
2. Verify service health endpoints
3. Check database connectivity
4. Review recent code changes
5. Search documentation (`docs/` directory)
6. Check GitHub issues (if public repo)

### Useful Commands
```bash
# Restart all services
docker-compose restart

# Rebuild specific service
docker-compose up -d --build user-service

# View all running containers
docker-compose ps

# Clean and rebuild everything
docker-compose down -v
mvn clean install
docker-compose up -d --build
```

---

## 📅 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-11-15 | Initial CLAUDE.md creation |

---

## 🎓 Learning Resources

**Internal Documentation:**
- `/docs/` - Comprehensive technical guides
- Service READMEs - Service-specific details
- Research documents - Competitor analysis

**External Resources:**
- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [Vaadin Docs](https://vaadin.com/docs/latest/)
- [PostgreSQL Docs](https://www.postgresql.org/docs/)
- [Docker Docs](https://docs.docker.com/)

---

**Remember:** This is a learning project demonstrating enterprise patterns. Code quality, maintainability, and documentation are priorities. When in doubt, ask for clarification before making significant changes.

**Happy Coding! 🚀**
