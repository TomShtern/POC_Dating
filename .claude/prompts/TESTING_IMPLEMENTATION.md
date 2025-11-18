# Testing & Quality Assurance Implementation Prompt

## Context
You are implementing the complete test suite for a POC Dating application. Currently **0 tests exist** but **70%+ coverage is required**. The Java backend and Vaadin UI are being developed by other agents - you write tests for their code. You have **full internet access** to research testing patterns, mocking strategies, and test frameworks.

## ⚠️ CRITICAL: Code Quality Requirements

**WRITE CLEAN, MAINTAINABLE, MODULAR TESTS.**

This is non-negotiable. Every test file must be:
- **MODULAR** - One test class per production class, focused test methods, reusable fixtures
- **MAINTAINABLE** - Clear test names, AAA pattern (Arrange-Act-Assert), easy to update
- **CLEAN** - No test interdependencies, no hardcoded magic values, DRY test utilities

**Modularity Rules:**
```java
// ✅ GOOD: Reusable test utilities
@TestConfiguration
public class TestDataFactory {
    public static UserRegistrationRequest validRegistration() {
        return new UserRegistrationRequest("test@test.com", "testuser", "Test123!");
    }
}

// ✅ GOOD: Focused test methods
@Test
void register_duplicateEmail_throwsConflictException() {
    // Arrange
    when(userRepository.existsByEmail(anyString())).thenReturn(true);

    // Act & Assert
    assertThrows(EmailAlreadyExistsException.class,
        () -> userService.register(validRegistration()));
}

// ❌ BAD: 50-line test methods testing multiple things
```

**Why This Matters:** Multiple agents are working on this codebase. Modular tests enable parallel development, pinpoint failures, and document behavior.

## Scope
1. **Unit tests** for all services (70%+ coverage)
2. **Integration tests** for API endpoints
3. **Repository tests** for database queries
4. **Security tests** for auth flows
5. **Contract tests** for service boundaries
6. **Performance tests** for critical paths

## Critical Resources
- **Testing patterns:** `.claude/CODE_PATTERNS.md` (Testing section)
- **Operations guide:** `.claude/OPERATIONS.md` (Testing Strategy)
- **API spec:** `docs/API-SPECIFICATION.md` (what to test)
- **Architecture:** `.claude/ARCHITECTURE_PATTERNS.md` (understand flows)

## Test Stack
```xml
<!-- Required dependencies per service pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>rabbitmq</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.redis.testcontainers</groupId>
    <artifactId>testcontainers-redis-junit</artifactId>
    <scope>test</scope>
</dependency>
```

## Implementation Tasks

### Task 1: Unit Tests (Per Service)
Each service needs tests in `src/test/java/com/dating/{service}/`:

#### User Service Tests
```java
// service/UserServiceTest.java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;
    @InjectMocks private UserService userService;

    @Test
    void register_ValidInput_ReturnsUser() { }
    @Test
    void register_DuplicateEmail_ThrowsException() { }
    @Test
    void login_ValidCredentials_ReturnsTokens() { }
    @Test
    void login_InvalidPassword_ThrowsException() { }
    @Test
    void getProfile_ExistingUser_ReturnsProfile() { }
    @Test
    void updateProfile_ValidInput_UpdatesAndEvictsCache() { }
}

// controller/AuthControllerTest.java
@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private UserService userService;

    @Test
    void register_ValidRequest_Returns201() { }
    @Test
    void register_InvalidEmail_Returns400() { }
    @Test
    void login_ValidCredentials_Returns200WithTokens() { }
}
```

#### Match Service Tests
```java
// service/MatchServiceTest.java
- testSwipe_Like_CreatesSwipeRecord()
- testSwipe_MutualLike_CreatesMatch()
- testSwipe_DailyLimitExceeded_ThrowsException()
- testGetFeed_ReturnsFilteredCandidates()
- testGetMatches_ReturnsActiveMatches()
- testUnmatch_DeactivatesMatch()
```

#### Chat Service Tests
```java
// service/ChatServiceTest.java
- testSendMessage_ValidMatch_SendsAndPublishes()
- testSendMessage_NotMatched_ThrowsException()
- testGetHistory_ReturnsPaginatedMessages()
- testMarkAsRead_UpdatesReadStatus()
```

#### Recommendation Service Tests
```java
// service/RecommendationServiceTest.java
- testCalculateScore_ReturnsNormalizedScore()
- testGetRecommendations_ReturnsSortedByScore()
- testUpdateScores_BatchUpdatesScores()
```

### Task 2: Integration Tests
Create `src/test/java/com/dating/{service}/integration/`:

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserServiceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void fullAuthFlow_RegisterLoginRefresh() {
        // Register
        var registerResponse = restTemplate.postForEntity("/api/users/auth/register",
            new UserRegistrationRequest("test@test.com", "testuser", "Test123!"),
            UserResponse.class);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

        // Login
        var loginResponse = restTemplate.postForEntity("/api/users/auth/login",
            new LoginRequest("test@test.com", "Test123!"),
            AuthResponse.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody().accessToken());

        // Access protected resource
        var headers = new HttpHeaders();
        headers.setBearerAuth(loginResponse.getBody().accessToken());
        var profileResponse = restTemplate.exchange("/api/users/profile",
            HttpMethod.GET, new HttpEntity<>(headers), UserResponse.class);
        assertEquals(HttpStatus.OK, profileResponse.getStatusCode());
    }
}
```

### Task 3: Repository Tests
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers
class UserRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_ExistingUser_ReturnsUser() { }

    @Test
    void findFeedCandidates_ReturnsFilteredAndSortedUsers() { }
}
```

### Task 4: Security Tests
```java
@SpringBootTest
class SecurityIntegrationTest {
    @Test
    void protectedEndpoint_NoToken_Returns401() { }

    @Test
    void protectedEndpoint_ExpiredToken_Returns401() { }

    @Test
    void protectedEndpoint_ValidToken_Returns200() { }

    @Test
    void adminEndpoint_UserRole_Returns403() { }
}
```

### Task 5: Event Tests
```java
@SpringBootTest
@Testcontainers
class EventIntegrationTest {
    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");

    @Test
    void userRegistered_PublishesEvent_MatchServiceReceives() { }

    @Test
    void matchCreated_PublishesEvent_ChatServiceReceives() { }
}
```

### Task 6: Performance Tests
```java
@SpringBootTest
class PerformanceTest {
    @Test
    @Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
    void getFeed_Under500ms() { }

    @Test
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void sendMessage_Under100ms() { }
}
```

## Iteration Loop (Repeat Until Complete)

### Phase 1: Write Tests
Start with one service, write all unit tests:
```bash
cd backend/user-service
# Create test files following patterns above
```

### Phase 2: Run Tests
```bash
mvn test
```
- If tests fail → analyze failure → fix test or implementation → rerun
- Use internet to research testing patterns for specific scenarios

### Phase 3: Check Coverage
```bash
mvn clean test jacoco:report
# Open target/site/jacoco/index.html
```
- If coverage < 70% → identify uncovered code → add tests → recheck
- Focus on: service methods, controller endpoints, error paths

### Phase 4: Integration Tests
```bash
mvn verify -Pintegration-tests
```
- If integration tests fail → check containers → fix config → rerun
- Ensure testcontainers can start (Docker required)

### Phase 5: Full Suite
```bash
cd backend && mvn clean verify
```
- All services must pass
- Coverage report shows 70%+ per service

## Coverage Targets
| Package | Minimum | Focus Areas |
|---------|---------|-------------|
| service/ | 80% | Business logic, edge cases |
| controller/ | 70% | Request validation, responses |
| repository/ | 60% | Custom queries |
| config/ | 50% | Security config |
| Overall | 70% | Combined |

## Test Naming Convention
```java
// Pattern: methodName_condition_expectedResult
void register_validInput_returnsCreatedUser()
void register_duplicateEmail_throwsConflictException()
void login_invalidPassword_throwsUnauthorizedException()
void getFeed_emptyDatabase_returnsEmptyList()
```

## Success Criteria
- [ ] Every service has unit tests for all service methods
- [ ] Every controller has MockMvc tests
- [ ] Every repository has @DataJpaTest tests
- [ ] Integration tests cover auth flow end-to-end
- [ ] Event publishing/consuming is tested
- [ ] Security configurations are tested
- [ ] JaCoCo shows 70%+ coverage per service
- [ ] All tests pass in CI (mvn verify)
- [ ] Tests run in <5 minutes total

## When Stuck
1. **Search internet** for JUnit 5, Mockito, Testcontainers, Spring Boot testing
2. **Check:** Stack traces for root cause
3. **Reference:** Spring Boot testing documentation
4. **Debug:** Use @Disabled temporarily to isolate failures

## DO NOT
- Skip error path testing (exceptions are critical)
- Use @SpringBootTest for unit tests (too slow)
- Hardcode test data that expires (dates, tokens)
- Leave @Disabled tests in final commit
- Mock everything (integration tests need real beans)
- Skip security tests (auth is critical)

---
**Iterate until all tests pass with 70%+ coverage. Use internet access freely to research testing patterns.**
