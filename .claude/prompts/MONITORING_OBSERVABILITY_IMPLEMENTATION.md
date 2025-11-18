# Monitoring & Observability Implementation Prompt

## Context
You are implementing simple, effective monitoring for a POC Dating application. The user is new to monitoring, so prioritize simplicity and clarity. You have **full internet access** to research Spring Boot Actuator, logging patterns, and monitoring tools.

**Scale:** 100-10K users (small scale - don't over-engineer)

**Philosophy:** Start with built-in Spring Boot features. Add external tools (Prometheus/Grafana) only if needed.

## ⚠️ CRITICAL: Code Quality Requirements

**WRITE CLEAN, MAINTAINABLE, SIMPLE MONITORING CODE.**

This is non-negotiable. Every monitoring component must be:
- **SIMPLE** - Use built-in Spring Boot features first, minimal external dependencies
- **USEFUL** - Only track metrics that help debug real problems
- **DOCUMENTED** - Comments explaining what each metric means and why it matters
- **BEGINNER-FRIENDLY** - Clear naming, easy to understand dashboards

**Simplicity Rules:**
```java
// ✅ GOOD: Simple, meaningful custom metric
@Service
@RequiredArgsConstructor
public class MatchService {
    private final MeterRegistry meterRegistry;  // Spring Boot's built-in metrics
    private final Counter matchesCreated;

    public MatchService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        // Track successful matches - helps understand user engagement
        this.matchesCreated = Counter.builder("matches.created.total")
            .description("Total number of matches created")
            .register(meterRegistry);
    }

    public Match createMatch(UUID user1, UUID user2) {
        Match match = // ... create match
        matchesCreated.increment();  // Simple counter
        return match;
    }
}

// ❌ BAD: Over-engineered metrics nobody will look at
meterRegistry.timer("match.creation.latency.percentile.p99.histogram.bucket")...
```

**Logging Rules:**
```java
// ✅ GOOD: Structured, actionable logs
@Slf4j
@Service
public class UserService {

    public User register(UserRegistrationRequest request) {
        log.info("User registration started: email={}", request.email());

        try {
            User user = createUser(request);
            log.info("User registered successfully: userId={}, email={}",
                user.getId(), user.getEmail());
            return user;
        } catch (Exception e) {
            log.error("User registration failed: email={}, error={}",
                request.email(), e.getMessage(), e);
            throw e;
        }
    }
}

// ❌ BAD: Useless logs
log.info("Entering method");
log.info("Step 1 complete");
log.info("Step 2 complete");  // Doesn't help debug anything
```

**Why This Matters:** Good monitoring helps you find and fix problems quickly. Bad monitoring is noise that wastes time.

## Scope
Implement monitoring in this order (simplest first):

1. **Health checks** - Are services running?
2. **Structured logging** - What happened and when?
3. **Key metrics** - How is the system performing?
4. **Dashboards** - Visualize metrics (optional, if time permits)

## Implementation Tasks

### Task 1: Health Checks (Built-in)

**application.yml for each service:**
```yaml
# Enable all actuator endpoints for development
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: always  # Show component health
      probes:
        enabled: true  # Kubernetes-style probes
  health:
    db:
      enabled: true
    redis:
      enabled: true
    rabbit:
      enabled: true

# Application info
info:
  app:
    name: ${spring.application.name}
    version: 1.0.0
    description: POC Dating Application
```

**Custom health indicator (example):**
```java
@Component
public class ExternalApiHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // Check if external API is reachable
        boolean apiUp = checkExternalApi();

        if (apiUp) {
            return Health.up()
                .withDetail("externalApi", "Available")
                .build();
        } else {
            return Health.down()
                .withDetail("externalApi", "Unavailable")
                .withDetail("action", "Check network connectivity")
                .build();
        }
    }
}
```

### Task 2: Structured Logging

**logback-spring.xml (in src/main/resources):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Console output for development -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- JSON output for production (easier to parse) -->
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>requestId</includeMdcKeyName>
        </encoder>
    </appender>

    <!-- Use JSON in production, console in development -->
    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="JSON"/>
        </root>
    </springProfile>

    <springProfile name="!prod">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <!-- Reduce noise from libraries -->
    <logger name="org.springframework" level="WARN"/>
    <logger name="org.hibernate" level="WARN"/>
    <logger name="com.dating" level="DEBUG"/>
</configuration>
```

**Request tracking filter:**
```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Generate unique request ID for tracing
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);

        // Extract user ID from JWT if present
        String userId = extractUserId(request);
        if (userId != null) {
            MDC.put("userId", userId);
        }

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            log.info("Request completed: method={}, path={}, status={}, duration={}ms",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);

            MDC.clear();
        }
    }
}
```

### Task 3: Key Metrics (Only the Important Ones)

**MetricsConfig.java:**
```java
@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            @Value("${spring.application.name}") String appName) {
        return registry -> registry.config()
            .commonTags("application", appName);
    }
}
```

**Service-level metrics (add to each service):**
```java
// User Service - Track registrations and logins
@Service
@Slf4j
public class UserService {
    private final Counter registrations;
    private final Counter loginAttempts;
    private final Counter loginFailures;

    public UserService(MeterRegistry meterRegistry, /* other deps */) {
        // METRIC: Total user registrations
        // WHY: Track user growth over time
        this.registrations = Counter.builder("users.registrations.total")
            .description("Total user registrations")
            .register(meterRegistry);

        // METRIC: Login attempts
        // WHY: Detect brute force attacks (many attempts, many failures)
        this.loginAttempts = Counter.builder("auth.login.attempts.total")
            .description("Total login attempts")
            .register(meterRegistry);

        this.loginFailures = Counter.builder("auth.login.failures.total")
            .description("Failed login attempts")
            .register(meterRegistry);
    }
}

// Match Service - Track swipes and matches
@Service
public class MatchService {
    private final Counter swipesLike;
    private final Counter swipesPass;
    private final Counter matchesCreated;

    // METRIC: Swipe ratio (likes vs passes)
    // WHY: Understand user behavior, detect bots (unusual ratios)

    // METRIC: Match rate
    // WHY: Core product metric - are people finding matches?
}

// Chat Service - Track messages
@Service
public class ChatService {
    private final Counter messagesSent;

    // METRIC: Messages sent
    // WHY: Engagement metric - are matches leading to conversations?
}
```

**Recommended metrics by service:**

| Service | Metric | Why Track It |
|---------|--------|-------------|
| User | registrations.total | User growth |
| User | login.attempts/failures | Security monitoring |
| Match | swipes.like/pass | User engagement |
| Match | matches.created | Core product metric |
| Match | feed.generation.time | Performance |
| Chat | messages.sent | Engagement depth |
| Chat | messages.read | Conversation quality |

### Task 4: Simple Dashboard (Optional)

**If you want visualization, use Spring Boot Admin (simpler than Grafana):**

```xml
<!-- Add to a new admin-service or existing service -->
<dependency>
    <groupId>de.codecentric</groupId>
    <artifactId>spring-boot-admin-starter-server</artifactId>
    <version>3.2.0</version>
</dependency>
```

**Or document how to view metrics via curl:**
```bash
# Health check
curl localhost:8081/actuator/health | jq

# All metrics
curl localhost:8081/actuator/metrics | jq

# Specific metric
curl localhost:8081/actuator/metrics/users.registrations.total | jq

# Prometheus format (for Grafana later)
curl localhost:8081/actuator/prometheus
```

## Iteration Loop (Repeat Until Complete)

### Phase 1: Health Checks
```bash
# Start a service
cd backend/user-service && mvn spring-boot:run

# Test health endpoint
curl localhost:8081/actuator/health | jq
```
- If health check fails → check database/Redis connection → fix config
- All components should show "UP"

### Phase 2: Logging
```bash
# Make a request and check logs
curl -X POST localhost:8081/api/users/auth/register ...

# Verify logs show: requestId, userId, duration
```
- If logs are missing context → check MDC filter → fix
- If too verbose → adjust log levels

### Phase 3: Metrics
```bash
# Check metrics endpoint
curl localhost:8081/actuator/metrics | jq

# Verify custom metrics appear
curl localhost:8081/actuator/metrics/users.registrations.total | jq
```
- If custom metrics missing → check Counter registration → fix

### Phase 4: End-to-End
- Register a user → check registration counter increased
- Login → check login attempt counter
- Create match → check matches counter
- Send message → check messages counter

## Success Criteria
- [ ] All services have /actuator/health endpoint working
- [ ] Health checks show database, Redis, RabbitMQ status
- [ ] Logs include requestId and userId for tracing
- [ ] Logs are structured (can be parsed by log tools)
- [ ] Key business metrics are tracked (registrations, matches, messages)
- [ ] Metrics are accessible via /actuator/metrics
- [ ] Documentation explains what each metric means

## When Stuck
1. **Search internet** for Spring Boot Actuator, Micrometer metrics
2. **Check:** /actuator endpoints for available data
3. **Read:** https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html

## DO NOT
- Add metrics for everything (only track what helps debug problems)
- Use complex distributed tracing (overkill for this scale)
- Set up Prometheus/Grafana immediately (start with Actuator)
- Log sensitive data (passwords, tokens, PII)
- Forget to document what metrics mean

## Future Enhancements (Not Now)
- Prometheus + Grafana dashboards
- Alerting rules (PagerDuty, Slack)
- Distributed tracing (Zipkin/Jaeger)
- Log aggregation (ELK stack)

---
**Iterate until health checks pass and metrics are visible. Use internet access freely to resolve Spring Boot Actuator issues.**
