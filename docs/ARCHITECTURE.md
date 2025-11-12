# POC Dating Application - System Architecture

**Document Status:** ✅ **ACTIVE** - Updated for Vaadin approach
**Last Updated:** 2025-11-11
**Version:** 2.0
**Status:** Implementation Phase - Vaadin UI

## Document Purpose

Technical architecture overview of the POC Dating Application for developers, architects, and stakeholders. This document reflects the current implementation using **Vaadin** (pure Java) for the frontend.

### Architecture Evolution
- **v1.0 (Initial):** Planned React + TypeScript frontend
- **v2.0 (Current):** Vaadin (Pure Java) frontend - See [FRONTEND_OPTIONS_ANALYSIS.md](FRONTEND_OPTIONS_ANALYSIS.md) for rationale

---

## Table of Contents

1. [High-Level Architecture](#high-level-architecture)
2. [Microservices Overview](#microservices-overview)
3. [Technology Stack](#technology-stack)
4. [Data Flow](#data-flow)
5. [Database Design](#database-design)
6. [Deployment Architecture](#deployment-architecture)
7. [Security Architecture](#security-architecture)
8. [Scalability Considerations](#scalability-considerations)
9. [Future Evolution](#future-evolution)

---

## High-Level Architecture

### System Diagram

```
┌─────────────────────────────────────────────────────────┐
│                   CLIENT LAYER                          │
│  ┌─────────────────────────────────────────────────┐    │
│  │         Web Browser (Any Device)                │    │
│  │         Accesses Vaadin UI directly             │    │
│  └──────────────────┬──────────────────────────────┘    │
└─────────────────────┼───────────────────────────────────┘
                      │
                 HTTPS/WSS
                      │
┌─────────────────────▼───────────────────────────────────┐
│         VAADIN UI SERVICE (Port 8090)                   │
│  - Pure Java web interface (Vaadin Flow)                │
│  - Server-side rendering + client sync                  │
│  - Session management (Redis)                           │
│  - WebSocket (@Push) for real-time updates              │
│  - Spring Security integration                          │
│  - Calls backend services via Feign                     │
└─────────────────────┬───────────────────────────────────┘
                      │ REST/Feign Clients
┌─────────────────────▼───────────────────────────────────┐
│              API GATEWAY (Port 8080)                    │
│  - Request routing & load balancing                     │
│  - JWT token validation                                 │
│  - Rate limiting                                        │
│  - CORS & security headers                              │
│  - Circuit breaker for fault tolerance                  │
└─┬────────────────────────────────────────────────────┬──┘
  │                                                    │
  │ HTTP/WebSocket                                     │
  │                                                    │
  ├─ /api/users/*          ────────────────┐          │
  ├─ /api/matches/*        ────────────────┼────┐     │
  ├─ /api/chat/ws          ────────────────┤    │     │
  └─ /api/recommendations/*────────────────┤    │     │
                                            │    │     │
┌───────────────────────────────────────────▼────▼─────────────────┐
│                    MICROSERVICES LAYER                            │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐  ┌─────────┐ │
│  │   User       │  │    Match     │  │   Chat    │  │Recommend│ │
│  │  Service     │  │   Service    │  │ Service   │  │  Service│ │
│  │  (8081)      │  │   (8082)     │  │  (8083)   │  │  (8084) │ │
│  └──────────────┘  └──────────────┘  └───────────┘  └─────────┘ │
│                                                                   │
└───────────────────────────┬───────────────────────────────────────┘
                            │
                    ┌───────┴──────┐
                    │              │
        ┌───────────▼──┐   ┌───────▼─────────┐
        │  PostgreSQL  │   │  Message Broker │
        │  (Primary)   │   │   (RabbitMQ)    │
        └──────────────┘   └─────────────────┘
                │                │
        ┌───────▼─────────────────▼──────┐
        │  Data Persistence Layer        │
        └────────────────────────────────┘
                    │
        ┌───────────┴───────────┐
        │                       │
    ┌───▼──┐          ┌────────▼─────┐
    │Redis │          │   Cassandra  │
    │Cache │          │   (Optional) │
    └──────┘          └──────────────┘
```

---

## Microservices Overview

### 0. Vaadin UI Service (Port 8090) 🆕

**Responsibility:** Web user interface (frontend) built entirely in Java using Vaadin

**Key Features:**
- Pure Java UI components (no JavaScript required)
- Server-side rendering with automatic client synchronization
- WebSocket support via @Push annotation for real-time updates
- Spring Security integration for authentication
- Session management via Redis
- Feign clients to call backend microservices
- Responsive design (works on desktop, tablet, mobile browsers)

**Technology Stack:**
- Vaadin 24.3 (Vaadin Flow)
- Spring Boot 3.2.0
- Spring Security
- Spring Cloud OpenFeign
- Redis (session storage)
- Lumo theme (customizable)

**Views Implemented:**
- LoginView - User authentication
- RegisterView - New user registration
- SwipeView - Profile browsing and swiping
- ChatView - Real-time messaging
- ProfileView - User profile management
- MatchesView - View all matches
- SettingsView - User preferences

**Integration Pattern:**
```java
// Feign client calling User Service
@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserServiceClient {
    @GetMapping("/api/users/{id}")
    User getUser(@PathVariable String id, @RequestHeader("Authorization") String token);
}

// Used in Vaadin view
@Route("profile")
public class ProfileView extends VerticalLayout {
    public ProfileView(UserService userService) {
        User user = userService.getCurrentUser();
        add(new H2(user.getFirstName()));
    }
}
```

**WebSocket Integration:**
```java
@Push // Enables server push via WebSocket/SSE
@Route("chat")
public class ChatView extends VerticalLayout {
    // Real-time message updates pushed from server
    private void onNewMessage(Message message) {
        getUI().ifPresent(ui -> ui.access(() -> {
            messageList.add(message);
        }));
    }
}
```

**Session Management:**
- Stateful server-side sessions stored in Redis
- Automatic session expiration and cleanup
- Sticky sessions not required (session in Redis, not memory)
- Load balancing friendly

**Security:**
- Spring Security authentication
- JWT tokens stored in session
- Per-request authorization via SecurityUtils
- CSRF protection built-in
- XSS protection via Vaadin's HTML escaping

**Performance Characteristics:**
- Initial page load: 1-2s (server-rendered HTML)
- Subsequent interactions: 100-200ms (server round-trip)
- WebSocket message delivery: <100ms
- Memory per session: 50-100KB (server-side state)
- Concurrent users per instance: ~5,000 (acceptable for POC)

**Pros:**
- 100% Java (team expertise utilized)
- Type-safe frontend-backend integration
- Rapid development (CRUD screens in minutes)
- Built-in components (Grid, Form, Chart, etc.)
- Real-time support (@Push)

**Cons:**
- Server-side rendering load
- Scalability ceiling vs SPA
- Vaadin-specific patterns

**Dependencies:** User Service, Match Service, Chat Service, Recommendation Service (via Feign)

---

### 1. User Service (Port 8081)

**Responsibility:** User identity, authentication, and profile management

**Key Features:**
- User registration and login
- JWT token generation and validation
- Profile CRUD operations
- Preference management
- Password reset
- Account status management

**Database:** PostgreSQL (users, user_preferences, refresh_tokens tables)

**Cache:** Redis (user profiles, preferences, token blacklist)

**Events Published:**
- `user:registered`
- `user:profile-updated`
- `user:deleted`

**Dependencies:** None (foundational service)

---

### 2. Match Service (Port 8082)

**Responsibility:** Swiping, match detection, and feed generation

**Key Features:**
- Record swipes (like/pass/super-like)
- Detect mutual matches
- Generate personalized feed
- Calculate match scores
- Swipe history management

**Database:** PostgreSQL (swipes, matches, match_scores tables)

**Cache:** Redis (feed cache, swipe counts)

**Events Published:**
- `match:created`
- `swipe:recorded`

**Consumed Events:**
- `user:preferences-updated` → regenerate feed

**Dependencies:** User Service (for user data validation)

---

### 3. Chat Service (Port 8083)

**Responsibility:** Real-time messaging between matched users

**Key Features:**
- WebSocket server for instant messaging
- Message persistence
- Delivery status tracking
- Read receipts
- Typing indicators
- Online/offline status
- Conversation management

**Database:** PostgreSQL (messages, conversations tables)

**Cache:** Redis (active connections, typing status, recent messages)

**Message Broker:** RabbitMQ (distribute messages across instances)

**Events Published:**
- `message:sent`
- `message:read`
- `conversation:created`

**Consumed Events:**
- `match:created` → create conversation
- `match:ended` → close conversation

**Dependencies:** User Service, Match Service

---

### 4. Recommendation Service (Port 8084)

**Responsibility:** Personalized recommendations and matching algorithms

**Key Features:**
- Generate recommendations based on preferences
- Calculate compatibility scores
- Analyze user interactions
- A/B test algorithms
- Learn from user behavior

**Database:** PostgreSQL (recommendations, interaction_history tables)

**Cache:** Redis (pre-computed recommendations)

**Events Consumed:**
- `user:registered`
- `user:preferences-updated`
- `swipe:recorded`
- `match:created`

**Dependencies:** User Service, Match Service

---

### 5. API Gateway (Port 8080)

**Responsibility:** Single entry point, routing, security enforcement

**Key Features:**
- Request routing to microservices
- JWT validation on all requests
- Rate limiting per user
- CORS configuration
- Circuit breaker for fault tolerance
- Request/response logging
- Health checks

**Architecture:**
- Spring Cloud Gateway
- Service discovery (Eureka optional)
- Resilience4j for circuit breaking

**Dependencies:** All microservices

---

## Technology Stack

### Backend
| Layer | Technology | Purpose |
|-------|-----------|---------|
| Framework | Spring Boot 3.x | REST APIs, microservices |
| Language | Java 21 | Type-safe, performant |
| Build | Maven 3.8+ | Dependency management |
| API Gateway | Spring Cloud Gateway | Routing, load balancing |
| ORM | Hibernate/JPA | Database access |
| Security | Spring Security + JWT | Authentication |

### Data Persistence
| Component | Technology | Purpose | Notes |
|-----------|-----------|---------|-------|
| Primary DB | PostgreSQL 14+ | Relational data | ACID compliance, runs on localhost for dev |
| Cache | Redis 7 | Session, feed, cache | Optional for advanced features |
| Message Broker | RabbitMQ 3.12 | Async events | Optional for event-driven features |
| (Future) | Cassandra | Time-series data | High-volume message history |

### Database Setup
| Aspect | Development | Production |
|--------|-------------|------------|
| Installation | PostgreSQL on localhost | Containerized in Docker |
| Setup | Run `setup-databases.sql` | Automated via Docker Compose |
| Schema Management | Auto-created via Hibernate `ddl-auto: update` | Managed via migrations |
| Access | Direct localhost:5432 | Via container network |
| Databases | 4 separate DBs (users, matches, chat, recs) | Same structure |

### Frontend (Vaadin UI Service)
| Component | Technology | Purpose |
|-----------|-----------|---------|
| Framework | Vaadin 24.3 | Pure Java web UI |
| Language | Java 21 | Same as backend! |
| UI Components | Vaadin Flow | Built-in components |
| Build | Maven 3.8+ | Same build tool as backend |
| Router | Vaadin Router | Java-based routing |
| State | Server-side | Session management via Redis |
| HTTP Client | Spring Cloud Feign | Type-safe API calls to microservices |
| Styling | Lumo Theme | Customizable CSS variables |
| Testing | Vaadin TestBench | Java-based UI testing |
| Real-time | Vaadin @Push | WebSocket/SSE for live updates |

**Key Advantage:** Entire stack is Java 21 - no language/paradigm switching!

### Frontend (React/TypeScript) - ⚠️ DEPRECATED
| Status | Notes |
|--------|-------|
| ⚠️ NOT USED | Replaced by Vaadin (Java) approach |
| 📋 REFERENCE | See [frontend/DEPRECATION_NOTICE.md](../frontend/DEPRECATION_NOTICE.md) |
| 📄 Analysis | See [FRONTEND_OPTIONS_ANALYSIS.md](FRONTEND_OPTIONS_ANALYSIS.md) for decision rationale |

### DevOps
| Component | Technology | Purpose | Usage |
|-----------|-----------|---------|-------|
| Containerization | Docker | Isolated environments | Production deployment only |
| Orchestration | Docker Compose | Service orchestration | Production/staging, not required for dev |
| Container Registry | (Future) | Image storage | Cloud deployment |
| CI/CD | GitHub Actions | Automated testing, building | Uses H2 dev profile |
| Local Development | Maven | Direct service execution | Recommended approach |

---

## Data Flow

### User Registration Flow

```
1. User submits registration form (web)
   ↓
2. POST /api/users/auth/register
   ↓
3. API Gateway validates request, routes to User Service
   ↓
4. User Service validates email/password
   ↓
5. Hash password with BCrypt
   ↓
6. Insert user into PostgreSQL
   ↓
7. Initialize user preferences in PostgreSQL
   ↓
8. Publish user:registered event to RabbitMQ
   ↓
9. Match Service listens: Initialize empty swipe history
   ↓
10. Recommendation Service listens: Initialize preferences
   ↓
11. Return JWT tokens to client
```

### Swipe & Match Flow

```
1. User swipes on card (like/pass)
   ↓
2. POST /api/matches/swipes
   ↓
3. Match Service records swipe in PostgreSQL
   ↓
4. Match Service publishes swipe:recorded event
   ↓
5. Match Service checks for mutual match:
   - Does target user like this user back?
   - If YES: Create match record
   - If NO: Keep waiting
   ↓
6. If mutual match:
   - Create Match record in PostgreSQL
   - Publish match:created event
   ↓
7. Chat Service listens to match:created:
   - Create Conversation record
   - Set up WebSocket topic
   ↓
8. Both users notified in real-time
   ↓
9. Users can now message each other
```

### Real-Time Messaging Flow

```
1. User A opens chat with User B
   ↓
2. WebSocket connects to /api/chat/ws
   ↓
3. Chat Service authenticates JWT
   ↓
4. User A subscribes to conversation topic
   ↓
5. User A types message → "typing" indicator sent (no persistence)
   ↓
6. User A sends message
   ↓
7. Chat Service:
   - Stores message in PostgreSQL
   - Updates status to SENT
   - Publishes to RabbitMQ message topic
   ↓
8. RabbitMQ routes to Chat Service instance connected to User B
   ↓
9. Chat Service broadcasts to User B's WebSocket
   ↓
10. User B receives message (Status: DELIVERED)
    ↓
11. User B opens message → Chat Service updates to READ
    ↓
12. Read confirmation sent back to User A
```

### Recommendation Generation Flow

```
1. User opens app (daily or on-demand)
   ↓
2. GET /api/recommendations/{userId}
   ↓
3. API Gateway routes to Recommendation Service
   ↓
4. Recommendation Service checks Redis cache:
   - If fresh (< 24h): Return cached recommendations
   - If expired: Regenerate
   ↓
5. Recommendation Algorithm:
   a. Fetch user preferences from User Service
   b. Get all active users
   c. Apply preference filters (age, distance, interests)
   d. Score each candidate (algorithm v1, v2, etc)
   e. Sort by score
   f. Cache top N results in Redis
   ↓
6. Return paginated recommendations
   ↓
7. User interactions (like/pass) stored
   ↓
8. Feed data flows to analytics (Recommendation Service learning)
```

---

## Database Design

### Schema Overview

**Core Tables:**
- `users` - User profiles (indexed by email, username, status)
- `user_preferences` - User matching preferences
- `swipes` - User swipes (high-volume, heavily indexed)
- `matches` - Mutual matches
- `messages` - Chat messages (partitionable by date)

**Supporting Tables:**
- `match_scores` - Pre-computed scores
- `refresh_tokens` - JWT token management
- `recommendations` - Recommendation cache
- `interaction_history` - Analytics data
- `audit_logs` - Change tracking

### Key Indexing Strategy

**High-Query Tables (Must index):**
- Swipes: `(user_id, created_at)`, `(target_user_id)`
- Messages: `(match_id, created_at)`

**User Queries:**
- Users: `(email)`, `(username)`, `(status)`

**Time-Series Data:**
- Interaction history: `(user_id, created_at)`
- Messages: Partition by date (future)

---

## Deployment Architecture

### Local Development (PostgreSQL-First)

**Recommended for development:**

```
1. Install PostgreSQL locally
   ↓
2. Run setup-databases.sql script
   ↓
3. Start services with Maven:
   - User Service (8081): mvn spring-boot:run
   - Match Service (8082): mvn spring-boot:run
   - Chat Service (8083): mvn spring-boot:run
   - Recommendation Service (8084): mvn spring-boot:run
   - Vaadin UI (8090): mvn spring-boot:run (optional)
   ↓
4. Services connect to PostgreSQL on localhost:5432
   ↓
5. Each service auto-creates its own database schema
```

**Benefits:**
- No Docker required for development
- Faster startup times
- Easier debugging (native Java processes)
- Direct database access for inspection
- Lower resource usage

### Alternative: H2 for Quick Testing

```bash
# Use dev profile for in-memory H2 database
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Good for:
# - Quick testing without PostgreSQL setup
# - CI/CD pipeline testing
# - Demo environments
```

### Production Deployment (Docker Compose)

**For production or full-stack testing:**

```
1. docker-compose up
   ↓
2. Containers Start:
   - PostgreSQL (5432)
   - Redis (6379) [optional]
   - RabbitMQ (5672) [optional]
   - User Service (8081)
   - Match Service (8082)
   - Chat Service (8083)
   - Recommendation Service (8084)
   - Vaadin UI (8090)
   ↓
3. All services containerized and orchestrated
```

**See [docs/DEPLOYMENT.md](DEPLOYMENT.md) for detailed deployment instructions.**

### Future: Kubernetes (Cloud Production)

```
Namespace: dating-app
├── Deployments
│   ├── user-service (2+ replicas)
│   ├── match-service (2+ replicas)
│   ├── chat-service (3+ replicas) [WebSocket needs sticky sessions]
│   ├── recommendation-service (2+ replicas)
│   └── vaadin-ui (2+ replicas)
├── StatefulSets
│   ├── PostgreSQL (or managed RDS/Cloud SQL)
│   └── Redis (or managed ElastiCache/MemoryStore)
├── Services
│   ├── ClusterIP (internal)
│   └── LoadBalancer (Vaadin UI public access)
└── ConfigMaps & Secrets
    ├── application configs
    ├── database credentials
    └── JWT secrets
```

---

## Security Architecture

### Authentication

```
1. User Registration
   - Email verification (future)
   - Password: BCrypt with salt (min 12 rounds)

2. Login
   - Validate email + password
   - Generate JWT (15 min expiry)
   - Generate Refresh Token (7 days)
   - Store refresh token hash in PostgreSQL

3. Subsequent Requests
   - Include JWT in Authorization header
   - API Gateway validates signature
   - Check expiration
   - Extract claims (userId, roles)

4. Token Refresh
   - POST /auth/refresh with refresh token
   - Validate refresh token in database
   - Generate new JWT
   - Option to rotate refresh token
```

### Authorization

```
- User Service: Own profile only, Admin endpoints
- Match Service: Can only see own swipes/matches
- Chat Service: Can only access conversations user participates in
- Recommendation Service: Public (but scoped to authenticated user)
```

### Data Protection

```
- In Transit: HTTPS/TLS (enforced in production)
- At Rest: Database encryption (future)
- Secrets: .env file, never in code/git
- API Keys: Not used (JWT instead)
```

### API Security

```
- CORS: Configured to allow only frontend origin
- Rate Limiting: 100 req/min per user (configurable)
- Input Validation: Bean Validation + custom validators
- SQL Injection: JPA parameterized queries
- XSS: Server-side sanitization + CSP headers (frontend)
- CSRF: Not needed (stateless JWT)
```

---

## Scalability Considerations

### Current POC Assumptions
- Single instance per service
- Local Docker Compose
- Limited to ~10k concurrent users

### Scaling Scenarios

#### Scenario 1: Vertical Scaling (Bigger Hardware)
- Increase JVM heap size
- Increase database connections
- Upgrade machine specs
- **Limitation:** Single point of failure, cost inefficiency

#### Scenario 2: Horizontal Scaling (More Instances)

**API Gateway:**
- Run 2-3 instances behind load balancer
- Stateless, easy to scale
- Session affinity needed for WebSocket (chat)

**User Service:**
- Run 2+ instances
- Database connection pooling important
- Cache hits reduce DB load

**Match Service:**
- Run 2+ instances
- Swipe recording must be atomic (database level)
- Feed caching critical for performance

**Chat Service:**
- Run 3+ instances
- WebSocket sticky sessions needed
- RabbitMQ STOMP broker for message distribution
- Most resource-intensive

**Recommendation Service:**
- Run 2+ instances
- Batch processing of recommendations
- Scheduled jobs for optimization

**Data Layer:**
```
PostgreSQL:
- Read replicas (1-2)
- Streaming replication
- Connection pooling (PgBouncer)
- Query optimization with indexes

Redis:
- Cluster mode for high availability
- Replication for backup
- Persistence (AOF or RDB)

RabbitMQ:
- Cluster mode (3-5 nodes)
- Quorum queues for durability
```

### Bottleneck Analysis

| Component | Bottleneck | Solution |
|-----------|-----------|----------|
| Swipe Recording | Database writes | Batch write, sharding by user_id |
| Feed Generation | CPU computation | Pre-compute, cache, paginate |
| Message Delivery | WebSocket connections | Horizontal scale Chat Service |
| User Lookup | Database queries | Cache aggressively |

---

## Future Evolution

### Phase 2: Enhanced Features
```
- Location-based services (PostGIS)
- Video calling integration
- Image uploading/optimization
- Push notifications
- Advanced search and filters
- Premium features (unlimited swipes, etc)
```

### Phase 3: Data & Analytics
```
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Data warehouse (SnowFlake)
- BI dashboards (Tableau)
- User behavior analytics
- A/B testing platform
```

### Phase 4: AI/ML
```
- Recommendation ML models
- Behavior prediction
- Fraud detection
- Spam filtering
- Image moderation
```

### Phase 5: Enterprise
```
- Multi-region deployment
- Disaster recovery
- Advanced monitoring
- Incident response
- SLA compliance
```

---

## Deployment Checklist

- [ ] All microservices build successfully
- [ ] Database schema initialized
- [ ] Environment variables configured
- [ ] Docker images built and tested
- [ ] Docker Compose working end-to-end
- [ ] API endpoints tested via Postman/Swagger
- [ ] WebSocket connections tested
- [ ] Load testing completed
- [ ] Security audit completed
- [ ] Documentation reviewed

---

## Questions & Decisions

**Q: Why microservices for a POC?**
A: Teaches scalability patterns early, easier to add features without affecting others, allows team parallelization.

**Q: Why not GraphQL?**
A: REST is simpler for POC, easier testing, standard HTTP caching.

**Q: Why PostgreSQL not NoSQL?**
A: ACID compliance for matches/swipes, complex queries, relational data structure.

**Q: Why RabbitMQ not Kafka?**
A: Simpler setup, sufficient for POC scale, easier to remove if not needed later.

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-11-11 | Architecture Team | Initial design |

