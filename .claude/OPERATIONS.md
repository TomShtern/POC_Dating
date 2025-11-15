# OPERATIONS.md - POC Dating Application Operations Guide

**Document Purpose:** Comprehensive guide for operational aspects of the POC Dating Application.

**Last Updated:** 2025-11-15
**Audience:** DevOps Engineers, QA, Backend Developers, Operations Team

---

## Table of Contents

1. [Testing Strategy](#testing-strategy)
2. [Deployment](#deployment)
3. [Security](#security)
4. [Performance](#performance)
5. [Monitoring & Debugging](#monitoring--debugging)
6. [Troubleshooting](#troubleshooting)
7. [Operations Checklist](#operations-checklist)

---

## Testing Strategy

### Overview

The testing pyramid for POC Dating follows industry best practices:

```
         ▲
        / \
       /   \ UI Tests (5%)
      /-----\  - Vaadin TestBench
     /       \
    /         \
   /-----------\
  /    Integration Tests (25%)
 /   - TestContainers
/-----------\
    Unit Tests (70%)
    - JUnit 5 + Mockito
```

### Test Coverage Expectations

| Layer | Coverage | Target | Tools |
|-------|----------|--------|-------|
| **Unit Tests** | 70%+ | Individual methods/classes | JUnit 5, Mockito |
| **Integration Tests** | Critical paths | Database + service interactions | TestContainers, Spring Boot Test |
| **UI Tests** | Main flows | User interactions | Vaadin TestBench |
| **Performance Tests** | Baseline | Load testing (future) | JMH, Gatling |

### Unit Test Patterns

#### Pattern 1: Service Method Testing with Mocks

```java
package com.dating.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UserService userService;

    private UserRegistrationRequest testRequest;

    @BeforeEach
    void setUp() {
        testRequest = new UserRegistrationRequest(
            "john.doe@example.com",
            "johndoe",
            "SecurePassword123!"
        );
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void testRegisterUser_Success() {
        // Arrange
        String hashedPassword = "hashed_password_abc123";
        when(passwordEncoder.encode(testRequest.getPassword()))
            .thenReturn(hashedPassword);

        User savedUser = User.builder()
            .id(UUID.randomUUID())
            .email(testRequest.getEmail())
            .username(testRequest.getUsername())
            .passwordHash(hashedPassword)
            .status(UserStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();

        when(userRepository.save(any(User.class)))
            .thenReturn(savedUser);

        // Act
        UserResponse response = userService.register(testRequest);

        // Assert
        assertNotNull(response);
        assertEquals("john.doe@example.com", response.getEmail());
        assertEquals("johndoe", response.getUsername());
        assertEquals(UserStatus.ACTIVE, response.getStatus());

        // Verify interactions
        verify(passwordEncoder, times(1)).encode(testRequest.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
        verify(rabbitTemplate, times(1))
            .convertAndSend(eq("user.exchange"), eq("user.registered"), any());
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void testRegisterUser_EmailAlreadyExists() {
        // Arrange
        when(userRepository.findByEmail(testRequest.getEmail()))
            .thenReturn(Optional.of(new User()));

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.register(testRequest);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid email format")
    void testRegisterUser_InvalidEmail() {
        // Arrange
        UserRegistrationRequest invalidRequest = new UserRegistrationRequest(
            "invalid-email",
            "johndoe",
            "password123"
        );

        // Act & Assert
        assertThrows(InvalidEmailException.class, () -> {
            userService.register(invalidRequest);
        });
    }
}
```

#### Pattern 2: Repository Testing with Slice Tests

```java
@DataJpaTest
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .email("test@example.com")
            .username("testuser")
            .passwordHash("hashed_pwd")
            .status(UserStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();
    }

    @Test
    @DisplayName("Should find user by email")
    void testFindByEmail_Success() {
        // Arrange
        entityManager.persistAndFlush(testUser);

        // Act
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void testFindByEmail_NotFound() {
        // Act
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("Should enforce unique email constraint")
    void testUniqueEmailConstraint() {
        // Arrange
        entityManager.persistAndFlush(testUser);

        User duplicateUser = User.builder()
            .email("test@example.com")  // Same email
            .username("anotheruser")
            .passwordHash("hashed_pwd")
            .status(UserStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> {
            entityManager.persistAndFlush(duplicateUser);
        });
    }
}
```

#### Pattern 3: Controller Testing with MockMvc

```java
@WebMvcTest(UserController.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("Should register user with valid request")
    void testRegisterUser_Success() throws Exception {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest(
            "john@example.com",
            "johndoe",
            "password123"
        );

        UserResponse response = UserResponse.builder()
            .id(UUID.randomUUID())
            .email("john@example.com")
            .username("johndoe")
            .status(UserStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();

        when(userService.register(any(UserRegistrationRequest.class)))
            .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/users/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.email").value("john@example.com"))
            .andExpect(jsonPath("$.username").value("johndoe"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService, times(1)).register(any(UserRegistrationRequest.class));
    }

    @Test
    @DisplayName("Should return 400 for invalid email")
    void testRegisterUser_InvalidEmail() throws Exception {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest(
            "invalid-email",
            "johndoe",
            "password123"
        );

        // Act & Assert
        mockMvc.perform(post("/api/users/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(userService, never()).register(any(UserRegistrationRequest.class));
    }
}
```

### Integration Test Patterns

#### Pattern 1: Database Integration with TestContainers

```java
@SpringBootTest
@Testcontainers
@DisplayName("UserService Integration Tests")
class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("dating_test_db")
        .withUsername("test_user")
        .withPassword("test_password")
        .withInitScript("init-test-db.sql");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Test
    @DisplayName("Should register and persist user to database")
    void testRegisterUser_PersistsToDatabase() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest(
            "integration@test.com",
            "integrationtest",
            "Password123!"
        );

        // Act
        UserResponse response = userService.register(request);

        // Assert
        Optional<User> savedUser = userRepository.findByEmail("integration@test.com");
        assertTrue(savedUser.isPresent());

        User user = savedUser.get();
        assertEquals(response.getId(), user.getId());
        assertTrue(passwordEncoder.matches("Password123!", user.getPasswordHash()));
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    @DisplayName("Should prevent duplicate email registration")
    void testDuplicateEmailRegistration_Fails() {
        // Arrange
        UserRegistrationRequest firstRequest = new UserRegistrationRequest(
            "duplicate@test.com",
            "user1",
            "password123"
        );

        UserRegistrationRequest secondRequest = new UserRegistrationRequest(
            "duplicate@test.com",
            "user2",
            "password456"
        );

        // Act
        userService.register(firstRequest);

        // Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.register(secondRequest);
        });
    }

    @Test
    @DisplayName("Should cache user profile in Redis")
    void testUserProfileCaching() {
        // Arrange
        User user = User.builder()
            .email("cached@test.com")
            .username("cacheduser")
            .passwordHash(passwordEncoder.encode("password123"))
            .status(UserStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();

        User saved = userRepository.save(user);

        // Act - First call (hits database)
        UserResponse response1 = userService.getUserById(saved.getId());

        // Act - Second call (hits cache)
        UserResponse response2 = userService.getUserById(saved.getId());

        // Assert
        assertEquals(response1.getId(), response2.getId());
        assertEquals(response1.getUsername(), response2.getUsername());
    }
}
```

#### Pattern 2: RabbitMQ Event Integration Testing

```java
@SpringBootTest
@TestcontainersTest
class UserEventIntegrationTest {

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-management-alpine");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UserService userService;

    @SpyBean
    private UserEventListener eventListener;

    @Test
    @DisplayName("Should publish and consume user registration event")
    void testUserRegistrationEvent() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest(
            "event@test.com",
            "eventuser",
            "password123"
        );

        // Act
        UserResponse response = userService.register(request);

        // Assert - Verify event was consumed
        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                verify(eventListener, times(1))
                    .handleUserRegistered(argThat(event ->
                        event.getUserId().equals(response.getId())
                    ));
            });
    }
}
```

### Vaadin UI Testing

#### Pattern: Vaadin Component Testing

```java
@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("SwipeView UI Tests")
class SwipeViewTest {

    @Autowired
    private TestBench testBench;

    @Autowired
    private MockUserService mockUserService;

    private SwipeViewElement swipeView;

    @BeforeEach
    void setUp() {
        // Initialize TestBench driver and navigate to view
        swipeView = testBench.$(SwipeViewElement.class).first();
    }

    @Test
    @DisplayName("Should display user profile card")
    void testProfileCardDisplayed() {
        // Assert
        assertTrue(swipeView.getProfileCard().isDisplayed());
        assertNotNull(swipeView.getProfileCard().getProfileImage());
        assertNotNull(swipeView.getProfileCard().getUserName());
    }

    @Test
    @DisplayName("Should handle swipe like action")
    void testSwipeLikeAction() {
        // Arrange
        swipeView.getProfileCard().hover();

        // Act
        swipeView.getLikeButton().click();

        // Assert
        assertEquals("Profile liked", swipeView.getNotification().getText());
        assertTrue(swipeView.getNextProfileCard().isDisplayed());
    }

    @Test
    @DisplayName("Should handle swipe pass action")
    void testSwipePassAction() {
        // Act
        swipeView.getPassButton().click();

        // Assert
        assertEquals("Profile passed", swipeView.getNotification().getText());
        assertTrue(swipeView.getNextProfileCard().isDisplayed());
    }
}

// Vaadin Element Helper Class
public class SwipeViewElement extends Component {
    public ImageElement getProfileImage() {
        return $(ImageElement.class).first();
    }

    public TextElement getUserName() {
        return $(TextElement.class).id("username");
    }

    public ButtonElement getLikeButton() {
        return $(ButtonElement.class).id("like-btn");
    }

    public ButtonElement getPassButton() {
        return $(ButtonElement.class).id("pass-btn");
    }

    public DivElement getProfileCard() {
        return $(DivElement.class).id("profile-card");
    }

    public DivElement getNextProfileCard() {
        return $(DivElement.class).id("next-profile-card");
    }

    public NotificationElement getNotification() {
        return $(NotificationElement.class).first();
    }
}
```

### Running Tests

#### Run All Tests

```bash
# From backend directory
cd /home/user/POC_Dating/backend

# Run all tests
mvn clean test

# Run with detailed output
mvn clean test -X

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run specific test method
mvn test -Dtest=UserServiceTest#testRegisterUser_Success

# Run tests in specific module
mvn test -pl user-service
```

#### Run Tests with Coverage Report

```bash
# Generate coverage report
cd /home/user/POC_Dating/backend
mvn clean test jacoco:report

# View coverage HTML report
open target/site/jacoco/index.html

# Check coverage from command line
mvn clean test jacoco:report && \
  cat target/site/jacoco-aggregate/index.html | \
  grep -oP 'Covered:\s+\K[^<]+'
```

#### Run Integration Tests Only

```bash
# Run integration tests (slower, requires containers)
mvn clean verify -Pintegration

# Run without unit tests
mvn clean verify -DskipUnitTests=true
```

#### Continuous Testing

```bash
# Watch mode - rerun tests on file changes
mvn test -DwatchDelay=500 -Dapache.commons.io.version=2.5

# Or use Maven watch extension
cd /home/user/POC_Dating/backend && mvn clean test -w
```

---

## Deployment

### Deployment Checklist

Before deploying any changes to production:

```
Pre-Deployment Verification:
  [ ] All unit tests pass (coverage > 70%)
  [ ] All integration tests pass
  [ ] Code review completed
  [ ] No security vulnerabilities detected
  [ ] Database migrations tested on staging
  [ ] API backwards compatibility verified
  [ ] Load testing completed (if applicable)
  [ ] Documentation updated
  [ ] Rollback plan documented

Deployment Steps:
  [ ] Backup current database
  [ ] Backup current Docker images
  [ ] Stop non-critical services
  [ ] Deploy to staging environment
  [ ] Run smoke tests on staging
  [ ] Get approval from stakeholders
  [ ] Deploy to production
  [ ] Verify all services started
  [ ] Run health checks
  [ ] Monitor logs for 30 minutes
  [ ] Notify team of successful deployment

Post-Deployment:
  [ ] Verify user-facing features work
  [ ] Check database integrity
  [ ] Review error logs
  [ ] Monitor performance metrics
  [ ] Document any issues
  [ ] Update deployment log
```

### Docker Operations

#### Building Services

```bash
# Build all services
cd /home/user/POC_Dating/backend
mvn clean install

# Build specific service Docker image
mvn -pl user-service docker:build

# Build with custom tag
mvn -pl user-service docker:build -Ddocker.image.tag=v1.2.3
```

#### Docker Compose Commands

```bash
# Start all services
docker-compose -f /home/user/POC_Dating/docker-compose.yml up -d

# Start specific services
docker-compose -f /home/user/POC_Dating/docker-compose.yml up -d postgres redis

# Start with rebuilding images
docker-compose -f /home/user/POC_Dating/docker-compose.yml up -d --build

# View running services
docker-compose -f /home/user/POC_Dating/docker-compose.yml ps

# View service logs
docker-compose -f /home/user/POC_Dating/docker-compose.yml logs -f user-service

# Stop all services
docker-compose -f /home/user/POC_Dating/docker-compose.yml down

# Remove volumes (DANGEROUS - deletes data)
docker-compose -f /home/user/POC_Dating/docker-compose.yml down -v

# Restart specific service
docker-compose -f /home/user/POC_Dating/docker-compose.yml restart user-service

# Scale service instances
docker-compose -f /home/user/POC_Dating/docker-compose.yml up -d --scale user-service=3
```

#### Service Startup Order

The services must start in this specific order to ensure dependencies are available:

```
1. PostgreSQL (8 minutes to initialize)
   ↓
2. Redis (1 minute)
   ↓
3. RabbitMQ (2 minutes)
   ↓
4. API Gateway (2 minutes) ← Waits for above
   ↓
5. User Service (1 minute) ← Registers with Gateway
   ↓
6. Match Service (1 minute) ← Registers with Gateway
   ↓
7. Chat Service (1 minute) ← Registers with Gateway
   ↓
8. Recommendation Service (1 minute) ← Registers with Gateway
   ↓
9. Vaadin UI Service (1 minute) ← Waits for all services
```

**Auto-startup order (docker-compose):**
```yaml
services:
  postgres:
    build: ./db
    environment:
      POSTGRES_DB: dating_db
    # No dependencies - starts first

  redis:
    image: redis:7-alpine
    depends_on:
      - postgres

  rabbitmq:
    image: rabbitmq:3.12-management-alpine
    depends_on:
      - postgres

  api-gateway:
    build: ./backend/api-gateway
    depends_on:
      - postgres
      - redis
      - rabbitmq

  user-service:
    build: ./backend/user-service
    depends_on:
      - api-gateway

  match-service:
    build: ./backend/match-service
    depends_on:
      - api-gateway

  chat-service:
    build: ./backend/chat-service
    depends_on:
      - api-gateway

  recommendation-service:
    build: ./backend/recommendation-service
    depends_on:
      - api-gateway

  vaadin-ui-service:
    build: ./backend/vaadin-ui-service
    depends_on:
      - api-gateway
      - user-service
      - match-service
      - chat-service
```

### Environment Configuration

#### Environment Variables

```bash
# Copy example to actual environment file
cp /home/user/POC_Dating/.env.example /home/user/POC_Dating/.env
```

#### .env File Structure

```env
# PostgreSQL Configuration
POSTGRES_DB=dating_db
POSTGRES_USER=dating_user
POSTGRES_PASSWORD=secure_password_change_in_production
POSTGRES_HOST=postgres
POSTGRES_PORT=5432

# Redis Configuration
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=          # Leave empty for development

# RabbitMQ Configuration
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/

# API Gateway Configuration
API_GATEWAY_PORT=8080
API_GATEWAY_CONTEXT_PATH=/api

# Service Ports
USER_SERVICE_PORT=8081
MATCH_SERVICE_PORT=8082
CHAT_SERVICE_PORT=8083
RECOMMENDATION_SERVICE_PORT=8084
VAADIN_UI_PORT=8090

# Security
JWT_SECRET_KEY=your_secret_key_min_256_bits_long_for_production
JWT_EXPIRATION_MS=900000           # 15 minutes
JWT_REFRESH_EXPIRATION_MS=604800000 # 7 days

# Application Configuration
SPRING_PROFILE=development         # development, staging, production
LOG_LEVEL=INFO
MAX_SWIPES_PER_DAY=100
MAX_SUPER_LIKES_PER_DAY=5

# Feature Flags
FEATURE_RECOMMENDATIONS_ENABLED=true
FEATURE_SUPER_LIKES_ENABLED=true
FEATURE_VERIFIED_BADGES_ENABLED=false

# Email Configuration
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=noreply@dating.app
MAIL_PASSWORD=email_password

# External Services (Future)
STORAGE_BUCKET_NAME=dating-app-uploads
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
```

#### Environment-Specific Configurations

**Development (.env.development)**
```env
SPRING_PROFILE=development
LOG_LEVEL=DEBUG
DATABASE_POOL_SIZE=10
CACHE_TTL_MINUTES=5
```

**Staging (.env.staging)**
```env
SPRING_PROFILE=staging
LOG_LEVEL=INFO
DATABASE_POOL_SIZE=20
CACHE_TTL_MINUTES=60
```

**Production (.env.production)**
```env
SPRING_PROFILE=production
LOG_LEVEL=WARN
DATABASE_POOL_SIZE=30
CACHE_TTL_MINUTES=3600
```

### Manual Service Startup (Non-Docker)

```bash
# Terminal 1: Start PostgreSQL
docker-compose -f /home/user/POC_Dating/docker-compose.yml up postgres redis rabbitmq

# Terminal 2: Start API Gateway
cd /home/user/POC_Dating/backend/api-gateway
mvn spring-boot:run

# Terminal 3: Start User Service
cd /home/user/POC_Dating/backend/user-service
mvn spring-boot:run

# Terminal 4: Start Match Service
cd /home/user/POC_Dating/backend/match-service
mvn spring-boot:run

# Terminal 5: Start Chat Service
cd /home/user/POC_Dating/backend/chat-service
mvn spring-boot:run

# Terminal 6: Start Recommendation Service
cd /home/user/POC_Dating/backend/recommendation-service
mvn spring-boot:run

# Terminal 7: Start Vaadin UI Service
cd /home/user/POC_Dating/backend/vaadin-ui-service
mvn spring-boot:run

# Access Application
# Vaadin UI: http://localhost:8090
# API Gateway: http://localhost:8080
```

---

## Security

### Authentication Details

#### JWT Token Flow

```
User Login Request
    ↓
User Service validates credentials (BCrypt check)
    ↓
Generate JWT Token (15 min expiry)
    ├─ Header: {"alg": "HS256", "typ": "JWT"}
    ├─ Payload: {
    │   "sub": "user_id_uuid",
    │   "email": "user@example.com",
    │   "roles": ["USER"],
    │   "iat": 1699000000,
    │   "exp": 1699000900
    │ }
    └─ Signature: HMAC-SHA256(header.payload, secret_key)
    ↓
Generate Refresh Token (7 day expiry)
    ├─ Store hash in PostgreSQL
    └─ Return to client
    ↓
Return Tokens to Client
    ├─ Authorization header: "Bearer {jwt_token}"
    └─ Refresh token in HTTP-only cookie
```

#### Token Management

```java
// Generate Token
public String generateToken(User user) {
    Instant now = Instant.now();
    Instant expiryTime = now.plus(JWT_EXPIRATION_MS, ChronoUnit.MILLIS);

    return Jwts.builder()
        .setSubject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("roles", user.getRoles())
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(expiryTime))
        .signWith(SignatureAlgorithm.HS256, JWT_SECRET_KEY)
        .compact();
}

// Validate Token
public boolean validateToken(String token) {
    try {
        Jwts.parser()
            .setSigningKey(JWT_SECRET_KEY)
            .parseClaimsJws(token);
        return true;
    } catch (JwtException | IllegalArgumentException e) {
        log.error("JWT validation error: {}", e.getMessage());
        return false;
    }
}

// Extract User ID from Token
public UUID extractUserId(String token) {
    Claims claims = Jwts.parser()
        .setSigningKey(JWT_SECRET_KEY)
        .parseClaimsJws(token)
        .getBody();

    return UUID.fromString(claims.getSubject());
}
```

#### API Gateway Authentication Filter

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        // Extract JWT from Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Validate token
        if (!jwtTokenProvider.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid token\"}");
            return;
        }

        // Extract user ID and add to request
        UUID userId = jwtTokenProvider.extractUserId(token);
        request.setAttribute("X-User-Id", userId.toString());

        // Pass to downstream services
        filterChain.doFilter(request, response);
    }
}
```

### Authorization Patterns

#### Role-Based Access Control (RBAC)

```java
@Component
public class AuthorizationService {

    public boolean canAccessUserProfile(UUID requestingUserId, UUID targetUserId) {
        // Users can only access their own profiles
        // Admins can access any profile
        if (isAdmin(requestingUserId)) {
            return true;
        }
        return requestingUserId.equals(targetUserId);
    }

    public boolean canSendMessage(UUID senderId, UUID recipientId) {
        // Users can only message matched users
        Optional<Match> match = matchRepository.findMatch(senderId, recipientId);
        return match.isPresent() && match.get().getStatus() == MatchStatus.ACTIVE;
    }

    public boolean canDeleteUser(UUID requestingUserId, UUID targetUserId) {
        // Users can delete own account
        // Admins can delete any account
        if (isAdmin(requestingUserId)) {
            return true;
        }
        return requestingUserId.equals(targetUserId);
    }

    private boolean isAdmin(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getRoles().contains("ADMIN");
    }
}
```

#### Method-Level Authorization

```java
@Service
@RequiredArgsConstructor
public class UserService {

    // Only user themselves or admins can access
    @PreAuthorize("hasRole('ADMIN') or #userId == principal.id")
    public UserResponse getUser(UUID userId) {
        return userRepository.findById(userId)
            .map(UserMapper::toResponse)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }

    // Only user themselves can update
    @PreAuthorize("#userId == principal.id")
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setBio(request.getBio());

        return UserMapper.toResponse(userRepository.save(user));
    }
}
```

### Data Protection

#### Password Hashing

```java
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with 12 rounds (stronger than default 10)
        return new BCryptPasswordEncoder(12);
    }
}

// Usage
@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;

    public UserResponse register(UserRegistrationRequest request) {
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
            .email(request.getEmail())
            .username(request.getUsername())
            .passwordHash(hashedPassword)
            .status(UserStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();

        return UserMapper.toResponse(userRepository.save(user));
    }

    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }
}
```

#### Sensitive Data Logging Prevention

```java
@Slf4j
@Service
public class UserService {

    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        // ✓ GOOD: Log only non-sensitive info
        log.info("User registration initiated for email: {}",
            maskEmail(request.getEmail()));

        // ✗ BAD: Never log passwords
        // log.info("Password: {}", request.getPassword());

        // ✗ BAD: Don't log full requests with sensitive data
        // log.info("Request: {}", request);

        User user = createUser(request);
        User saved = userRepository.save(user);

        // ✓ GOOD: Log only safe response data
        log.info("User registered successfully with ID: {}", saved.getId());

        return UserMapper.toResponse(saved);
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
```

### API Security Measures

#### Input Validation

```java
@PostMapping("/auth/register")
public ResponseEntity<UserResponse> register(
        @Valid @RequestBody UserRegistrationRequest request) {
    // @Valid annotation triggers Bean Validation
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(userService.register(request));
}

@Data
public class UserRegistrationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must contain uppercase, lowercase, number, special char, min 8 chars")
    private String password;

    @Min(value = 18, message = "Must be 18 or older")
    @Max(value = 120, message = "Invalid age")
    private Integer age;
}
```

#### CORS Configuration

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("https://dating.app", "https://www.dating.app")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

#### Rate Limiting (API Gateway)

```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Integer> redisTemplate;
    private static final int MAX_REQUESTS = 100;
    private static final long WINDOW_MS = 60000; // 1 minute

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getAttribute("X-User-Id").toString();
        String key = "rate-limit:" + userId;

        Integer requestCount = redisTemplate.opsForValue().get(key);
        if (requestCount == null) {
            requestCount = 0;
        }

        if (requestCount >= MAX_REQUESTS) {
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Rate limit exceeded\"}");
            return;
        }

        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofMillis(WINDOW_MS));

        filterChain.doFilter(request, response);
    }
}
```

---

## Performance

### Caching Strategy

#### Redis Cache Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .entryTtl(Duration.ofHours(1));

        return RedisCacheManager.create(factory);
    }
}
```

#### Cache Key Strategy

```
User Profile Caching:
├─ Key: user:{userId}:profile
├─ TTL: 1 hour
├─ Hit Ratio Target: 85%
└─ Size: ~2KB per entry

Feed Generation:
├─ Key: feed:{userId}:page:{pageNum}
├─ TTL: 24 hours
├─ Hit Ratio Target: 75%
└─ Size: ~50KB per page

User Preferences:
├─ Key: preferences:{userId}
├─ TTL: 24 hours
├─ Hit Ratio Target: 90%
└─ Size: ~500 bytes

Recommendation Cache:
├─ Key: recommendations:{userId}
├─ TTL: 24 hours
├─ Hit Ratio Target: 80%
└─ Size: ~100KB

Session Data:
├─ Key: session:{sessionId}
├─ TTL: 30 minutes
├─ Hit Ratio Target: 95%
└─ Size: ~5KB
```

#### Cache Implementation Examples

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    // Method-level caching
    @Cacheable(value = "users", key = "#userId", unless = "#result == null")
    public UserResponse getUserById(UUID userId) {
        log.debug("Cache miss - fetching user from database: {}", userId);
        return userRepository.findById(userId)
            .map(UserMapper::toResponse)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }

    // Cache eviction on update
    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        user.setFirstName(request.getFirstName());
        user.setBio(request.getBio());

        log.info("User updated and cache invalidated: {}", userId);
        return UserMapper.toResponse(userRepository.save(user));
    }

    // Manual cache management
    public void invalidateUserCache(UUID userId) {
        Cache cache = cacheManager.getCache("users");
        if (cache != null) {
            cache.evict(userId);
            log.debug("Cache manually invalidated for user: {}", userId);
        }
    }

    // Cache statistics
    public CacheStats getCacheStatistics(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache instanceof RedisCache) {
            // Get Redis cache metrics
            return new CacheStats(
                // Hit count, miss count, etc.
            );
        }
        return null;
    }
}
```

### Database Optimization

#### Connection Pooling

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20          # Max connections
      minimum-idle: 5                # Min idle connections
      connection-timeout: 20000      # 20 seconds
      idle-timeout: 600000           # 10 minutes
      max-lifetime: 1800000          # 30 minutes
      connection-test-query: "SELECT 1"
      auto-commit: true
```

#### Query Optimization

```java
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // GOOD: Explicit fetch strategy avoids N+1
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.matches WHERE u.id = :id")
    Optional<User> findByIdWithMatches(@Param("id") UUID id);

    // GOOD: Pagination for large result sets
    @Query("SELECT u FROM User u WHERE u.status = :status")
    Page<User> findByStatus(
        @Param("status") UserStatus status,
        Pageable pageable);

    // GOOD: Composite index for common queries
    @Query(value = "SELECT u.* FROM users u " +
           "WHERE u.status = :status AND u.created_at > :date " +
           "ORDER BY u.created_at DESC LIMIT :limit",
           nativeQuery = true)
    List<User> findActiveUsersSince(
        @Param("status") String status,
        @Param("date") Instant date,
        @Param("limit") int limit);

    // BAD: Lazy loading causes N+1 queries
    // Optional<User> findByEmail(String email);
}
```

#### Indexing Strategy

```sql
-- User table indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_status_created ON users(status, created_at DESC);

-- Swipes table indexes (high-volume)
CREATE INDEX idx_swipes_user_id ON swipes(user_id);
CREATE INDEX idx_swipes_target_user_id ON swipes(target_user_id);
CREATE INDEX idx_swipes_user_created ON swipes(user_id, created_at DESC);
CREATE INDEX idx_swipes_composite ON swipes(user_id, target_user_id, created_at);

-- Matches table indexes
CREATE INDEX idx_matches_user1 ON matches(user1_id);
CREATE INDEX idx_matches_user2 ON matches(user2_id);
CREATE UNIQUE INDEX idx_matches_pair ON matches(
    LEAST(user1_id, user2_id),
    GREATEST(user1_id, user2_id)
);

-- Messages table indexes
CREATE INDEX idx_messages_match_id ON messages(match_id);
CREATE INDEX idx_messages_match_created ON messages(match_id, created_at DESC);
```

### Performance Targets

| Operation | Target | Tolerance |
|-----------|--------|-----------|
| User login | <300ms | <500ms |
| Feed generation | <1000ms | <2000ms |
| Single profile fetch | <100ms | <200ms |
| Match detection | <500ms | <1000ms |
| Message delivery | <100ms | <500ms |
| Image upload | <5000ms | <10000ms |
| Database query | <100ms | <200ms |
| API response | <500ms | <1000ms |
| Cache hit | <10ms | <50ms |
| Cache miss | <200ms | <500ms |

#### Performance Testing Query

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitorService {

    public void logOperationDuration(String operation, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        log.info("Operation: {} - Duration: {}ms", operation, duration);

        if (duration > 1000) {
            log.warn("SLOW OPERATION: {} took {}ms", operation, duration);
        }
    }
}

// Usage
@GetMapping("/{id}")
public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
    long startTime = System.currentTimeMillis();

    UserResponse response = userService.getUserById(id);

    performanceMonitorService.logOperationDuration("getUser", startTime);
    return ResponseEntity.ok(response);
}
```

### Bottleneck Analysis

```bash
# Monitor database slow queries
docker exec -it dating_postgres psql -U dating_user -d dating_db

# Enable slow query log (queries > 100ms)
ALTER SYSTEM SET log_min_duration_statement = 100;
SELECT pg_reload_conf();

# View slow queries
docker-compose logs postgres | grep "duration:"

# Analyze query execution plan
EXPLAIN ANALYZE SELECT u.* FROM users u
WHERE u.status = 'ACTIVE' AND u.created_at > NOW() - INTERVAL '7 days'
ORDER BY u.created_at DESC LIMIT 20;

# Check Redis memory usage
docker exec -it dating_redis redis-cli
INFO memory
MEMORY STATS
```

---

## Monitoring & Debugging

### Health Checks

#### Service Health Endpoints

```bash
# Check all services at once
for service in api-gateway user-service match-service chat-service recommendation-service vaadin-ui-service; do
  echo "=== $service ==="
  curl -s http://localhost:${PORT}}/actuator/health | jq .
done

# API Gateway Health
curl -s http://localhost:8080/actuator/health | jq .

# User Service Health
curl -s http://localhost:8081/actuator/health | jq .

# Detailed health info
curl -s http://localhost:8080/actuator/health/liveness | jq .
curl -s http://localhost:8080/actuator/health/readiness | jq .
```

#### Health Check Response

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "result": 1
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.0"
      }
    },
    "rabbitmq": {
      "status": "UP",
      "details": {
        "version": "3.12.0"
      }
    },
    "livenessState": {
      "status": "UP"
    },
    "readinessState": {
      "status": "UP"
    }
  }
}
```

### Logging Configuration

#### Logging Levels

```yaml
# application.yml
logging:
  level:
    root: INFO
    com.dating: DEBUG
    com.dating.security: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  pattern:
    console: "%d{ISO8601} %5p %40.40logger{39} : %msg%n"
    file: "%d{ISO8601} %5p %40.40logger{39} : %msg%n"
  file:
    name: logs/application.log
    max-size: 10MB
    max-history: 30
    total-size-cap: 1GB
```

#### Structured Logging

```java
@Slf4j
@Service
public class UserService {

    public UserResponse register(UserRegistrationRequest request) {
        // Structured logging with MDC
        MDC.put("email", maskEmail(request.getEmail()));
        MDC.put("operation", "user_registration");
        MDC.put("timestamp", Instant.now().toString());

        try {
            log.info("User registration initiated");
            User user = createUser(request);
            log.info("User registered successfully: {}", user.getId());
            return UserMapper.toResponse(user);
        } catch (Exception e) {
            log.error("User registration failed", e);
            MDC.put("error", e.getMessage());
            throw new RegistrationException("Registration failed", e);
        } finally {
            MDC.clear();
        }
    }
}
```

#### Log Aggregation (Future)

```yaml
# ELK Stack / Datadog / CloudWatch integration
spring:
  cloud:
    gcp:
      logging:
        enabled: true

logging:
  cloud:
    enabled: true
```

### Metrics Collection

#### Micrometer Metrics

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final MeterRegistry meterRegistry;

    public UserResponse register(UserRegistrationRequest request) {
        Timer timer = Timer.start(meterRegistry);

        try {
            User user = createUser(request);
            meterRegistry.counter("users.registered").increment();
            return UserMapper.toResponse(user);
        } catch (Exception e) {
            meterRegistry.counter("users.registration.failed").increment();
            throw e;
        } finally {
            timer.stop(Timer.builder("user.registration.time")
                .description("Time taken to register a user")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry));
        }
    }
}
```

#### Prometheus Metrics

```bash
# View Prometheus metrics
curl -s http://localhost:8080/actuator/prometheus | head -50

# Example metrics
jvm_memory_used_bytes
jvm_gc_memory_promoted_bytes_total
http_server_requests_seconds_count
http_server_requests_seconds_sum
spring_data_repository_invocations_total
```

#### Metrics Dashboard (Grafana)

```
Key Metrics to Monitor:
├─ JVM Metrics
│  ├─ Memory usage (heap/non-heap)
│  ├─ GC time and pause duration
│  └─ Thread count
├─ Application Metrics
│  ├─ Request count by endpoint
│  ├─ Response time (P50, P95, P99)
│  ├─ Error rate
│  └─ User registration rate
├─ Database Metrics
│  ├─ Connection pool utilization
│  ├─ Query response time
│  ├─ Slow query count
│  └─ Transaction duration
└─ Cache Metrics
   ├─ Hit ratio
   ├─ Eviction rate
   └─ Memory usage
```

---

## Troubleshooting

### Service Won't Start

```bash
# Check Docker logs
docker-compose logs postgres

# Check port availability
lsof -i :8080

# Kill blocking process
kill -9 <PID>

# Check environment variables
docker-compose config | grep -A 20 "user-service:"

# Rebuild service
docker-compose up -d --build user-service
```

### Database Connection Errors

```bash
# Verify PostgreSQL is running
docker-compose ps postgres

# Test connection
docker exec -it dating_postgres psql \
  -U dating_user -d dating_db -c "SELECT 1;"

# Check credentials in .env
cat .env | grep POSTGRES

# View PostgreSQL logs
docker-compose logs postgres | tail -50

# Reset database (DEVELOPMENT ONLY)
docker-compose down -v
docker volume rm poc_dating_postgres_data
docker-compose up postgres
```

### High Memory Usage

```bash
# Check JVM heap usage
jps -lv

# Monitor with jstat
jstat -gc -h10 <pid> 1000

# Generate heap dump
jmap -dump:live,format=b,file=heap.bin <pid>

# Analyze with Eclipse MAT or jhat
jhat heap.bin
```

### Slow Queries

```sql
-- Find slow queries
SELECT query, calls, mean_time
FROM pg_stat_statements
WHERE mean_time > 100
ORDER BY mean_time DESC
LIMIT 10;

-- Analyze specific query
EXPLAIN ANALYZE
SELECT u.* FROM users u
WHERE u.status = 'ACTIVE'
AND u.created_at > NOW() - INTERVAL '7 days'
ORDER BY u.created_at DESC
LIMIT 20;

-- Check table statistics
ANALYZE users;
```

### Cache Issues

```bash
# Monitor Redis
docker exec -it dating_redis redis-cli

# Check memory usage
INFO memory

# View all keys
KEYS *

# Check specific cache
GET user:123:profile

# Clear specific cache
DEL user:123:profile

# Clear all caches
FLUSHDB

# Check TTL
TTL user:123:profile
```

---

## Operations Checklist

### Daily Operations

```
Morning (Start of Day):
  [ ] Check service health endpoints
  [ ] Review error logs from previous night
  [ ] Verify database backups completed
  [ ] Check cache hit ratios
  [ ] Confirm all services are running
  [ ] Monitor user registration metrics

During Business Hours:
  [ ] Monitor API response times
  [ ] Watch for error spikes
  [ ] Track user session count
  [ ] Verify message delivery latency
  [ ] Check database connection pool utilization
  [ ] Monitor cache memory usage

Evening (End of Day):
  [ ] Generate daily metrics report
  [ ] Review performance trends
  [ ] Check scheduled maintenance tasks
  [ ] Backup database
  [ ] Archive logs
  [ ] Plan next day operations
```

### Weekly Operations

```
Monday:
  [ ] Review weekly performance metrics
  [ ] Analyze error logs and patterns
  [ ] Check database growth rate
  [ ] Verify backup integrity
  [ ] Update runbooks if needed

Wednesday:
  [ ] Run security scanning
  [ ] Check dependency updates
  [ ] Performance baseline comparison
  [ ] User feedback review

Friday:
  [ ] Capacity planning review
  [ ] Test disaster recovery plan
  [ ] Review upcoming maintenance windows
  [ ] Generate weekly report
```

### Monthly Operations

```
First Week:
  [ ] Full security audit
  [ ] Backup testing and validation
  [ ] Dependency vulnerability scan
  [ ] Update documentation
  [ ] Capacity planning review

Second Week:
  [ ] Performance optimization analysis
  [ ] Cost analysis
  [ ] Review error budgets
  [ ] Plan infrastructure improvements

Third Week:
  [ ] Load testing
  [ ] Disaster recovery drill
  [ ] Security penetration test (quarterly)

Fourth Week:
  [ ] Post-incident review if applicable
  [ ] Generate monthly report
  [ ] Plan next month's operations
  [ ] Archive old logs and metrics
```

---

## Quick Reference

### Common Commands

```bash
# Start development environment
docker-compose up -d

# Stop all services
docker-compose down

# View logs for service
docker-compose logs -f user-service

# Connect to database
docker exec -it dating_postgres psql -U dating_user -d dating_db

# Run tests
mvn clean test

# Build services
mvn clean install

# Health check
curl http://localhost:8080/actuator/health | jq .

# View metrics
curl http://localhost:8080/actuator/prometheus | head -50
```

### Important Ports

| Service | Port | Purpose |
|---------|------|---------|
| PostgreSQL | 5432 | Database |
| Redis | 6379 | Cache |
| RabbitMQ | 5672 | Message broker |
| RabbitMQ Management | 15672 | Admin console |
| API Gateway | 8080 | Main entry point |
| User Service | 8081 | Authentication |
| Match Service | 8082 | Matching algorithm |
| Chat Service | 8083 | WebSocket messaging |
| Recommendation | 8084 | ML recommendations |
| Vaadin UI | 8090 | Web interface |

---

**Document Version:** 1.0.0
**Last Review:** 2025-11-15
**Next Review:** 2025-12-15

For questions or updates to this guide, contact the Operations team.
