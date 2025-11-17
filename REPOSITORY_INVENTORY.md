# POC Dating Application - Repository Inventory

**Generated:** 2025-11-17
**Repository:** POC_Dating
**Purpose:** Comprehensive file inventory and repository structure documentation

---

## 📊 Repository Overview

**Total Files:** 94 files
**Repository Size:** ~1.2 MB
**Primary Language:** Java (Spring Boot microservices)
**Frontend:** Vaadin (Pure Java UI)
**Architecture:** Microservices with API Gateway pattern

---

## 📁 Root Level Documentation

### DATING_APPS_COMPARISON.md
- **Description:** Comprehensive side-by-side technical comparison of Tinder, Bumble, OkCupid, and Hinge architectures with complexity analysis and ideal 2025 stack recommendations
- **Metadata:** 27,939 bytes | 822 lines
- **Important Notes:** Contains architecture grades (Tinder B+, Bumble B-, OkCupid C+, Hinge B), database trade-offs, microservices analysis, recommends Go + PostgreSQL + React Native for new apps

### BUMBLE_TECH_ANALYSIS.md
- **Description:** Technical deep-dive into Bumble's architecture including Bumble 2.0 cloud-native transformation and polyglot backend challenges
- **Metadata:** 13,150 bytes | 372 lines
- **Important Notes:** Critical issue with 6+ programming languages creating operational chaos; documents PHP 8 modernization and iOS scaling challenges

### OKCUPID_TECH_ANALYSIS.md
- **Description:** Analysis of OkCupid's 20-year journey from custom OKWS C++ web server to modern GraphQL stack
- **Metadata:** 18,039 bytes | 506 lines
- **Important Notes:** Red flag - OKWS is 20+ year old C++ infrastructure still partially running; cautionary tale of custom infrastructure becoming technical debt

### HINGE_TECH_ANALYSIS.md
- **Description:** Analysis of Hinge's pragmatic stack using React Native/Flutter, Python/Django backend, PostgreSQL, and Gale-Shapley matching algorithm
- **Metadata:** 20,275 bytes | 557 lines
- **Important Notes:** Most correct database choice (PostgreSQL); cost-effective approach; performance ceiling at 100M+ users

### DEVOPS_TESTING_ML_INFRASTRUCTURE.md
- **Description:** Detailed analysis of CI/CD practices, testing methodologies, A/B testing frameworks, and ML infrastructure for dating apps
- **Metadata:** 18,876 bytes | 699 lines
- **Important Notes:** Documents Tinder's TinVec embeddings, Smart Photos optimization using Epsilon Greedy algorithm; quality infrastructure cost $2-11M/year

### SECURITY_COMPLIANCE_ANALYSIS.md
- **Description:** Comprehensive security architecture covering authentication, encryption (AES-256, TLS 1.3), GDPR/CCPA compliance with real enforcement cases
- **Metadata:** 16,236 bytes | 571 lines
- **Important Notes:** MFA reduces fake profiles by 89%; GDPR fines up to €20M or 4% revenue; Bumble £32M settlement case documented

### README.md
- **Description:** Primary project documentation with architecture overview, quick start guide, technology stack details, and development instructions
- **Metadata:** 33,397 bytes | 888 lines
- **Important Notes:** Documents switch from React to Vaadin (pure Java UI); Spring Boot 3.x microservices; PostgreSQL + Redis + RabbitMQ + Cassandra stack

### PERFORMANCE_SCALE_METRICS.md
- **Description:** Performance metrics and scalability analysis with infrastructure cost estimates and team structures
- **Metadata:** 11,808 bytes | 428 lines
- **Important Notes:** Tinder processes 2B daily swipes with 70M+ users; infrastructure costs $60-180M/year; 500+ microservices add 40-60% overhead

### TINDER_TECH_ANALYSIS.md
- **Description:** Technical deep-dive into Tinder's 500+ microservices architecture with TAG API Gateway and service mesh
- **Metadata:** 13,713 bytes | 418 lines
- **Important Notes:** Over-engineered with 10x more services than needed; native Swift/Kotlin mobile apps; MongoDB/DynamoDB + Redis stack

### TECHNICAL_EVOLUTION_INTEGRATIONS.md
- **Description:** Timeline of technical evolution for major dating apps (2004-2025) documenting migrations and lessons learned
- **Metadata:** 20,905 bytes | 693 lines
- **Important Notes:** Successful migrations: 6-24 months; failed migrations: 4-10+ years; aggressive timelines with 50%+ engineering allocation succeed

---

## 🏗️ Backend Architecture

### Parent Configuration

#### backend/pom.xml
- **Description:** Parent Maven POM centralizing dependency management for all microservices
- **Metadata:** 10,302 bytes | 285 lines
- **Important Notes:** Spring Boot 3.2.0, Java 21, declares 7 modules, manages versions for Spring Cloud, JWT, PostgreSQL, Redis, RabbitMQ, TestContainers

---

## 🔌 API Gateway Service

### backend/api-gateway/Dockerfile
- **Description:** Multi-stage Docker build for API Gateway using Maven builder and Alpine JRE runtime
- **Metadata:** 684 bytes | 17 lines
- **Important Notes:** Port 8080, health checks every 30s, JVM memory -Xmx512m, non-root user execution

### backend/api-gateway/pom.xml
- **Description:** Maven configuration for API Gateway with Spring Cloud Gateway, JWT validation, circuit breaking
- **Metadata:** 3,616 bytes | 108 lines
- **Important Notes:** Routes to 4 backend services (User:8081, Match:8082, Chat:8083, Recommendation:8084), includes Resilience4j, Eureka client

### backend/api-gateway/README.md
- **Description:** Comprehensive documentation covering routing rules, authentication flow, rate limiting, circuit breaker patterns
- **Metadata:** 5,965 bytes | 287 lines
- **Important Notes:** Redis-backed rate limiting, 50% failure rate triggers circuit breaker, distributed tracing with request IDs, WebSocket support via STOMP/RabbitMQ

---

## 📚 Common Library

### backend/common-library/pom.xml
- **Description:** Shared utility library Maven configuration for DTOs, entities, exceptions used across all microservices
- **Metadata:** 2,628 bytes | 69 lines
- **Important Notes:** Minimal dependencies (JPA, Lombok), NOT a microservice itself, enforces DRY principle

### backend/common-library/README.md
- **Description:** Documentation outlining purpose, intended contents, and usage patterns for shared library
- **Metadata:** 2,123 bytes | 77 lines
- **Important Notes:** Contains entities, DTOs, custom exceptions, enums, validators; NO business logic or repositories

---

## 👤 User Service

### backend/user-service/README.md
- **Description:** Complete documentation for user authentication and profile management service
- **Metadata:** 4,723 bytes | 182 lines
- **Important Notes:** JWT tokens (15min access, 7day refresh), BCrypt password hashing, Redis token blacklist, publishes user:registered/updated/deleted events

### backend/user-service/Dockerfile
- **Description:** Multi-stage Dockerfile with extensive inline comments explaining architectural choices
- **Metadata:** 1,992 bytes | 68 lines
- **Important Notes:** Port 8081, Maven 3.9 + JDK 21 builder, Alpine JRE runtime, health checks, non-root execution

### backend/user-service/pom.xml
- **Description:** Service-specific Maven configuration for authentication and profile management
- **Metadata:** 4,168 bytes | 130 lines
- **Important Notes:** Dependencies: PostgreSQL, Redis, JWT (jjwt), RabbitMQ, TestContainers; publishes events to Match and Recommendation services

### backend/user-service/src/main/resources/application.yml
- **Description:** Spring Boot configuration for server, database, cache, messaging, security, and logging
- **Metadata:** 4,636 bytes | 186 lines
- **Important Notes:** Port 8081, HikariCP pool (max 20), Redis TTL 30min, JWT secret configurable, supports dev/test/prod profiles

---

## 💑 Match Service

### backend/match-service/README.md
- **Description:** Complete specification for swipe recording, match detection, feed generation, and scoring algorithm
- **Metadata:** 5,280 bytes | 218 lines
- **Important Notes:** Match scoring: interest match (0-40pts), age compatibility (0-30pts), preference alignment (0-30pts); Redis feed caching 24h TTL

### backend/match-service/Dockerfile
- **Description:** Multi-stage Docker build for Match Service
- **Metadata:** 745 bytes | 19 lines
- **Important Notes:** Port 8082, health checks every 30s, JVM -Xmx512m, Alpine JRE runtime

### backend/match-service/pom.xml
- **Description:** Maven configuration with event-driven architecture dependencies
- **Metadata:** 3,159 bytes | 98 lines
- **Important Notes:** High-frequency event handling, Spring Data Redis, RabbitMQ, PostgreSQL, publishes match:created/ended/swipe:recorded events

---

## 💬 Chat Service

### backend/chat-service/README.md
- **Description:** Documentation for real-time messaging via WebSockets with delivery tracking and typing indicators
- **Metadata:** 5,292 bytes | 228 lines
- **Important Notes:** <100ms message latency (p95), WebSocket + RabbitMQ for distributed handling, Redis for active connections, message status tracking (SENT/DELIVERED/READ)

### backend/chat-service/Dockerfile
- **Description:** Multi-stage Docker build for Chat Service
- **Metadata:** 688 bytes | 17 lines
- **Important Notes:** Port 8083, health checks, JVM memory limits, non-root user

### backend/chat-service/pom.xml
- **Description:** Maven configuration with WebSocket, Redis, RabbitMQ dependencies
- **Metadata:** 3,505 bytes | 112 lines
- **Important Notes:** Spring WebSocket, AMQP for message broker, PostgreSQL persistence, TestContainers for integration testing

---

## 🎯 Recommendation Service

### backend/recommendation-service/README.md
- **Description:** Comprehensive documentation of recommendation engine with algorithm details and caching strategy
- **Metadata:** 5,907 bytes | 221 lines
- **Important Notes:** Rule-based matching formula, collaborative filtering planned, Redis caching (24h TTL), performance: ~500ms initial, <50ms cached

### backend/recommendation-service/Dockerfile
- **Description:** Multi-stage Docker build for Recommendation Service
- **Metadata:** 728 bytes | 18 lines
- **Important Notes:** Port 8084, health monitoring, Alpine base image

### backend/recommendation-service/pom.xml
- **Description:** Maven configuration with Apache Commons Math for future ML implementations
- **Metadata:** 3,636 bytes | 117 lines
- **Important Notes:** PostgreSQL, Redis, RabbitMQ, Apache Commons Math 3.6.1, publishes recommendation:generated/accepted/rejected events

---

## 🎨 Vaadin UI Service

### backend/vaadin-ui-service/README.md
- **Description:** Comprehensive documentation for Vaadin UI Service including architecture, setup, configuration, and troubleshooting
- **Metadata:** 8,600 bytes | 384 lines
- **Important Notes:** Pure Java UI alternative to React, documents all views (Login, Register, Swipe, Matches, Messages, Profile), performance optimization tips

### backend/vaadin-ui-service/pom.xml
- **Description:** Maven configuration with Vaadin 24.3, Spring Cloud OpenFeign, Redis sessions, Spring Security
- **Metadata:** 5,700 bytes | 176 lines
- **Important Notes:** Vaadin Spring Boot Starter, Feign clients for microservice communication, production build profile, TestBench for testing

### backend/vaadin-ui-service/Dockerfile
- **Description:** Alpine Linux-based Docker image for Vaadin UI Service
- **Metadata:** 380 bytes | 16 lines
- **Important Notes:** Port 8090, health checks via wget, Eclipse Temurin JRE 21

### backend/vaadin-ui-service/src/main/resources/application.yml
- **Description:** Spring Boot configuration for Vaadin UI with Redis sessions and backend service URLs
- **Metadata:** 1,100 bytes | 56 lines
- **Important Notes:** Port 8090, Redis session storage (30m timeout), environment variable configuration for backend services, Vaadin development mode

### backend/vaadin-ui-service/frontend/themes/dating-theme/styles.css
- **Description:** Custom CSS theme with gradient backgrounds and component styling
- **Metadata:** 1,900 bytes | 80 lines
- **Important Notes:** Color palette (Primary #667eea, Secondary #764ba2), profile card hover effects, gradient buttons, Lumo dark variant override

### backend/vaadin-ui-service/frontend/themes/dating-theme/theme.json
- **Description:** Vaadin theme configuration JSON
- **Metadata:** ~50 bytes | 3 lines
- **Important Notes:** Configures dating-theme with Lumo variant

### backend/vaadin-ui-service/src/main/java/com/dating/ui/VaadinUIApplication.java
- **Description:** Spring Boot entry point enabling Feign clients and configuring Vaadin theme
- **Metadata:** 894 bytes | 27 lines
- **Important Notes:** @EnableFeignClients for microservices, AppShellConfigurator for theme setup, Lumo DARK variant

---

### Feign Clients (Microservice Communication)

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/client/UserServiceClient.java
- **Description:** Interface for user authentication, profile management, and preferences
- **Metadata:** 1,400 bytes | 36 lines
- **Important Notes:** Methods: login, register, getUser, updateUser, getPreferences, updatePreferences; requires Bearer token

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/client/MatchServiceClient.java
- **Description:** Interface for profile discovery, swiping, and match retrieval
- **Metadata:** 1,100 bytes | 31 lines
- **Important Notes:** Methods: getNextProfile, recordSwipe, getMyMatches; returns SwipeResponse with match status

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/client/ChatServiceClient.java
- **Description:** Interface for conversation management and messaging
- **Metadata:** 1,400 bytes | 34 lines
- **Important Notes:** REST endpoints for messages, real-time via WebSocket (not REST), methods: getConversations, getMessages, sendMessage

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/client/RecommendationServiceClient.java
- **Description:** Interface for personalized recommendations
- **Metadata:** 896 bytes | 24 lines
- **Important Notes:** Methods: getRecommendations with optional limit (default 20), refreshRecommendations

---

### DTOs (Data Transfer Objects)

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/User.java
- **Description:** Complete user profile DTO with personal info, bio, photos, location, preferences
- **Metadata:** 709 bytes | 33 lines
- **Important Notes:** Fields: id, email, username, name, age, gender, bio, photoUrl, location, preferences; Lombok @Builder, @Data

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/Match.java
- **Description:** Match relationship DTO with user IDs and timestamps
- **Metadata:** 432 bytes | 21 lines
- **Important Notes:** Fields: id, user1Id, user2Id, otherUser, createdAt, hasUnreadMessages

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/Message.java
- **Description:** Individual message DTO with sender info and status
- **Metadata:** 453 bytes | 22 lines
- **Important Notes:** Fields: id, conversationId, senderId, senderName, text, createdAt, status (SENT/DELIVERED/READ)

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/Conversation.java
- **Description:** Chat thread DTO with participants and unread count
- **Metadata:** 434 bytes | 21 lines
- **Important Notes:** Fields: id, matchId, otherUser, lastMessage, unreadCount, createdAt

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/AuthResponse.java
- **Description:** Authentication response with tokens and user data
- **Metadata:** 279 bytes | 14 lines
- **Important Notes:** Fields: accessToken, refreshToken, user; returned from login/register

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/LoginRequest.java
- **Description:** Login credentials request
- **Metadata:** 246 bytes | 13 lines
- **Important Notes:** Fields: email, password

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/RegisterRequest.java
- **Description:** New account registration request
- **Metadata:** 421 bytes | 20 lines
- **Important Notes:** Fields: email, username, password, firstName, lastName, age, gender; uses @Builder

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/SwipeRequest.java
- **Description:** Swipe action request
- **Metadata:** 257 bytes | 13 lines
- **Important Notes:** Fields: targetUserId, swipeType (LIKE/PASS/SUPER_LIKE)

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/SwipeResponse.java
- **Description:** Swipe result indicating match status
- **Metadata:** 277 bytes | 14 lines
- **Important Notes:** Fields: match (boolean), matchId, matchedUser

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/SwipeType.java
- **Description:** Enumeration of swipe actions
- **Metadata:** 89 bytes | 7 lines
- **Important Notes:** Values: LIKE, PASS, SUPER_LIKE

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/SendMessageRequest.java
- **Description:** Message text request
- **Metadata:** 222 bytes | 12 lines
- **Important Notes:** Field: text (message content)

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/MessageStatus.java
- **Description:** Message delivery/read states enumeration
- **Metadata:** 92 bytes | 7 lines
- **Important Notes:** Values: SENT, DELIVERED, READ

---

### Security Layer

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/security/SecurityConfig.java
- **Description:** Spring Security configuration with Vaadin integration
- **Metadata:** 1,300 bytes | 39 lines
- **Important Notes:** Extends VaadinWebSecurity, allows public /images/**, LoginView as entry point, BCryptPasswordEncoder bean

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/security/SecurityUtils.java
- **Description:** Session-based security utility for token and user identity management
- **Metadata:** 2,300 bytes | 72 lines
- **Important Notes:** Methods: getCurrentUserId, getCurrentToken, getCurrentUserName, isAuthenticated, setAuthenticationInfo, clearAuthentication; stores in VaadinSession

---

### Service Layer

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/service/UserService.java
- **Description:** Service layer wrapping UserServiceClient with session handling
- **Metadata:** 2,800 bytes | 96 lines
- **Important Notes:** Methods: login, register, getCurrentUser, updateProfile, logout; stores JWT in session via SecurityUtils

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/service/MatchService.java
- **Description:** Service layer for swiping and match operations
- **Metadata:** 2,300 bytes | 86 lines
- **Important Notes:** Methods: getNextProfile, recordSwipe, getMyMatches, getMatch; validates authentication before API calls

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/service/ChatService.java
- **Description:** Service layer for conversations and messages
- **Metadata:** 2,200 bytes | 78 lines
- **Important Notes:** REST fallback for messages, real-time WebSocket in views (TODO), methods: getConversations, getMessages, sendMessage

---

### Views (User Interface)

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/views/LoginView.java
- **Description:** Authentication entry point with email/password form
- **Metadata:** 4,600 bytes | 138 lines
- **Important Notes:** Route /login (anonymous), gradient background, validates credentials, stores token, navigates to SwipeView on success

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/views/RegisterView.java
- **Description:** New user account creation with comprehensive validation
- **Metadata:** 6,400 bytes | 186 lines
- **Important Notes:** Route /register (anonymous), fields: email, username, password, confirm, name, age, gender; validation: age >= 18, password >= 8 chars, password match

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/views/SwipeView.java
- **Description:** Main discovery interface with swipe actions and match notifications
- **Metadata:** 5,800 bytes | 180 lines
- **Important Notes:** Route / (default, authenticated), ProfileCard component, three buttons (Like/Pass/SuperLike), match notification modal

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/views/MatchesView.java
- **Description:** List view displaying all user matches in grid
- **Metadata:** 2,100 bytes | 72 lines
- **Important Notes:** Route /matches (authenticated), Vaadin Grid with columns: Name, Age, Location, Matched On; click navigates to MessagesView

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/views/MessagesView.java
- **Description:** Conversation list with last message preview and unread count
- **Metadata:** 2,400 bytes | 74 lines
- **Important Notes:** Route /messages (authenticated), TODO: ChatView with @Push for real-time not implemented, grid columns: Name, Last Message, Unread

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/views/ProfileView.java
- **Description:** User profile editor with personal information fields
- **Metadata:** 4,000 bytes | 120 lines
- **Important Notes:** Route /profile (authenticated), fields: firstName, lastName, age (18-100), city, bio (500 char max); saves via UserService

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/views/MainLayout.java
- **Description:** Application shell with header and navigation drawer
- **Metadata:** 2,700 bytes | 81 lines
- **Important Notes:** Extends AppLayout, header with logo/username/logout, drawer with 4 nav items (Discover, Matches, Messages, Profile)

---

### Components

#### backend/vaadin-ui-service/src/main/java/com/dating/ui/components/ProfileCard.java
- **Description:** Reusable card component for user profiles
- **Metadata:** 3,900 bytes | 128 lines
- **Important Notes:** Methods: setUser, showNoMoreProfiles; 400x400px image with placeholder fallback, displays name/age/location/bio, CSS hover animations

---

## 🗄️ Database & Infrastructure

### db/init/01-schema.sql
- **Description:** PostgreSQL initialization script with complete relational schema
- **Metadata:** 9,915 bytes | 227 lines
- **Important Notes:** 11 tables (users, user_preferences, swipes, matches, match_scores, messages, refresh_tokens, recommendations, interaction_history, audit_logs), UUID primary keys, extensive indexes, CHECK constraints, JSONB for metadata

### docker-compose.yml
- **Description:** Orchestration for complete microservices stack with 8 services
- **Metadata:** 9,766 bytes | 319 lines
- **Important Notes:** PostgreSQL (5432), Redis (6379), RabbitMQ (5672, 15672), API Gateway (8080), 4 backend services (8081-8084), Vaadin UI (8090); health checks, volume persistence, internal network

---

## 📖 Documentation (docs/)

### docs/ARCHITECTURE.md (✅ ACTIVE)
- **Description:** System architecture documentation for Vaadin approach with microservices design
- **Metadata:** 24 KB | 791 lines
- **Important Notes:** Version 2.0 updated 2025-11-11 for Vaadin transition, defines API Gateway, microservices (User, Match, Chat, Recommendation), Redis caching, Vaadin UI Service (8090)

### docs/DEVELOPMENT.md (✅ ACTIVE)
- **Description:** Complete development setup and workflow guide for Vaadin approach
- **Metadata:** 13 KB | 668 lines
- **Important Notes:** Version 2.0 updated 2025-11-11, transitioned from React to Vaadin, includes Docker Compose, Vaadin dev server, testing with Maven, API testing

### docs/DOCUMENT_INDEX.md (✅ ACTIVE)
- **Description:** Master index with status markers and role-based navigation
- **Metadata:** 10 KB | 306 lines
- **Important Notes:** Version 1.0 created 2025-11-11, shows ACTIVE/REFERENCE/DEPRECATED status, role-based reading paths (developers, architects, DevOps)

### docs/API-SPECIFICATION.md (✅ ACTIVE)
- **Description:** Complete REST API contract specification with examples
- **Metadata:** 12 KB | 716 lines
- **Important Notes:** Base URL localhost:8080/api, comprehensive endpoints for auth, users, matching, chat, recommendations, error codes, rate limiting, cURL/Postman examples

### docs/FRONTEND_OPTIONS_ANALYSIS.md (✅ REFERENCE)
- **Description:** Comparative analysis of 6 frontend options justifying Vaadin selection
- **Metadata:** 24 KB | 771 lines
- **Important Notes:** Version 1.0 created 2025-11-11, crucial decision document, code examples for each option, timelines: Vaadin 3 weeks vs React/TS 3-4 months

### docs/VAADIN_IMPLEMENTATION.md (✅ ACTIVE)
- **Description:** Step-by-step guide for building Vaadin UI Service
- **Metadata:** 34 KB | 1,222 lines
- **Important Notes:** Version 1.0 created 2025-11-11, complete implementation roadmap, pom.xml templates, Feign clients, views, security, styling, testing, deployment

### docs/VAADIN_TRANSITION_SUMMARY.md (✅ COMPLETED)
- **Description:** Transition overview from React to Vaadin with rationale
- **Metadata:** 16 KB | 427 lines
- **Important Notes:** Version 1.0 created 2025-11-11, before/after architecture, verification checklist, learning resources, future migration options

### docs/01-competitor-analysis.md (📋 REFERENCE)
- **Description:** Analysis of dating app competitors identifying market trends and features
- **Metadata:** 11 KB | 301 lines
- **Important Notes:** Foundational research, key features (swiping, matching, messaging), monetization models, security concerns, AI/ML applications

### docs/02-recommended-architecture.md (📋 REFERENCE)
- **Description:** Microservices architecture design with service boundaries and communication patterns
- **Metadata:** 31 KB | 1,053 lines
- **Important Notes:** Architecture diagrams, defines 5 core services, API Gateway pattern, event-driven communication, cloud deployment

### docs/03-tech-stack-recommendations.md (📋 REFERENCE)
- **Description:** Technology selection rationale for backend, data layer, infrastructure, and frontend
- **Metadata:** 32 KB | 1,153 lines
- **Important Notes:** Originally recommended React (now Vaadin), covers Spring Boot 3.2, Java 21, PostgreSQL, Redis, Docker, Kubernetes

### docs/04-differentiators-best-practices.md (📋 REFERENCE)
- **Description:** Unique features and industry best practices for dating apps
- **Metadata:** 32 KB | 1,316 lines
- **Important Notes:** Strategy document for product differentiation, advanced matching, verified profiles, safety/moderation, premium features, analytics

### docs/05-api-specification.md (📋 REFERENCE)
- **Description:** API endpoint specifications with REST design principles
- **Metadata:** 31 KB | 1,446 lines
- **Important Notes:** Detailed endpoint specs (superseded by API-SPECIFICATION.md), authentication flow, user management, matching, chat, recommendations

### docs/06-database-schemas.md (📋 REFERENCE)
- **Description:** PostgreSQL database schema design with ER diagrams
- **Metadata:** 22 KB | 939 lines
- **Important Notes:** Core tables, PostGIS indexes for geospatial, foreign key relationships, soft-delete patterns

### docs/07-matching-algorithm.md (📋 REFERENCE)
- **Description:** Detailed matching algorithm design with compatibility scoring
- **Metadata:** 23 KB | 850 lines
- **Important Notes:** Algorithm combines interests, age compatibility, location proximity (geospatial), profile completion, caching strategy

### docs/08-realtime-messaging.md (📋 REFERENCE)
- **Description:** Real-time messaging system design using WebSocket and message queuing
- **Metadata:** 22 KB | 905 lines
- **Important Notes:** WebSocket with polling fallback, message persistence, typing indicators, read receipts, connection resilience

### docs/09-infrastructure-deployment.md (📋 REFERENCE)
- **Description:** Complete infrastructure setup for Docker, Kubernetes, CI/CD, monitoring
- **Metadata:** 25 KB | 1,212 lines
- **Important Notes:** Docker Compose for dev, Kubernetes for prod, AWS deployment, GitLab CI/CD, Prometheus/Grafana monitoring, ELK logging

### docs/10-backend-architecture-patterns.md (📋 REFERENCE)
- **Description:** Backend code organization using Clean Architecture and DDD patterns
- **Metadata:** 33 KB | 1,266 lines
- **Important Notes:** Originally for Node.js/TypeScript, adapted for Java Spring Boot, repository pattern, dependency injection, testing strategies

### docs/11-mobile-architecture.md (📋 REFERENCE)
- **Description:** React Native mobile app architecture with state management
- **Metadata:** 26 KB | 1,026 lines
- **Important Notes:** Phase 2 mobile development, component structure, navigation, performance optimization, local storage, WebSocket integration

### docs/12-security-implementation.md (📋 REFERENCE)
- **Description:** Comprehensive security with JWT refresh rotation, OAuth 2.0, password hashing
- **Metadata:** 44 KB | 1,594 lines
- **Important Notes:** Critical implementation details, authentication flow, token lifecycle, Argon2 passwords, rate limiting, AES-256 encryption

### docs/13-performance-monitoring.md (📋 REFERENCE)
- **Description:** APM setup with DataDog/New Relic, query optimization, caching strategies
- **Metadata:** 33 KB | 1,334 lines
- **Important Notes:** Advanced optimization, APM configuration, EXPLAIN ANALYZE, index strategies, DataLoader batching, multi-level caching, k6 load testing

### docs/14-java-backend-architecture.md (📋 REFERENCE)
- **Description:** Java/Spring Boot backend architecture with multi-module Maven structure
- **Metadata:** 64 KB | 2,034 lines
- **Important Notes:** Comprehensive Spring Boot guide, Clean Architecture in Java, Lombok, MapStruct, Argon2, PostGIS geospatial, Liquibase, HikariCP, @Async

### docs/15-hybrid-architecture-java-nodejs.md (📋 REFERENCE)
- **Description:** Hybrid architecture combining Java Spring Boot with Node.js services
- **Metadata:** 34 KB | 1,189 lines
- **Important Notes:** Advanced architecture for scale, service distribution, REST/gRPC/Kafka communication, event sourcing, saga pattern, OpenTelemetry tracing

### docs/16-android-kotlin-architecture.md (📋 REFERENCE)
- **Description:** Native Android application architecture using Kotlin and Jetpack Compose
- **Metadata:** 51 KB | 1,585 lines
- **Important Notes:** Phase 2 native Android, Clean Architecture, Jetpack Compose UI, Hilt DI, Retrofit, Room database, Socket.IO WebSocket, testing

---

## 🎨 Frontend (Deprecated)

### frontend/package.json
- **Description:** Project manifest for React-based frontend (DEPRECATED)
- **Metadata:** 927 bytes | 37 lines
- **Important Notes:** ⚠️ DEPRECATED - React 18, TypeScript 5.3, Vite 5.0, Zustand, Tailwind CSS; do NOT use for active development

### frontend/README.md
- **Description:** Comprehensive React frontend architecture documentation (DEPRECATED)
- **Metadata:** 7,844 bytes | 313 lines
- **Important Notes:** ⚠️ DEPRECATED - Extensive guide for React/TypeScript/Vite architecture; kept for reference only

### frontend/DEPRECATION_NOTICE.md
- **Description:** Critical status notice indicating React frontend replaced with Vaadin
- **Metadata:** 7,108 bytes | 206 lines
- **Important Notes:** ⚠️ DEPRECATED as of 2025-11-11, transition rationale: 3-week timeline with Vaadin vs 3-4 months React/TS learning curve, use /backend/vaadin-ui-service/ instead

---

## 📊 Summary Statistics

| Category | Count | Total Size |
|----------|-------|------------|
| **Root Documentation** | 10 files | 194 KB |
| **Backend Services** | 6 services | - |
| **Backend Configuration Files** | 23 files | ~100 KB |
| **Vaadin UI Service Files** | 33 files | ~65 KB |
| **Documentation (docs/)** | 23 files | ~563 KB |
| **Frontend (Deprecated)** | 3 files | ~16 KB |
| **Database & Infrastructure** | 2 files | ~20 KB |
| **Total Files** | 94 files | ~1.2 MB |

---

## 🔑 Key Architecture Insights

### Microservices Architecture
- **API Gateway Pattern:** Single entry point (Port 8080) routing to 4 backend services
- **Service Ports:** User (8081), Match (8082), Chat (8083), Recommendation (8084), Vaadin UI (8090)
- **Event-Driven Communication:** RabbitMQ message broker for asynchronous events
- **Service Discovery:** Netflix Eureka for dynamic service registration

### Technology Stack
- **Backend:** Spring Boot 3.2.0, Java 21
- **Frontend:** Vaadin 24.3 (Pure Java UI)
- **Database:** PostgreSQL 15 with UUID primary keys, extensive indexes, JSONB metadata
- **Caching:** Redis for sessions, token blacklist, feed caching (24h TTL)
- **Messaging:** RabbitMQ for event streaming between services
- **Containerization:** Docker with multi-stage builds, Alpine Linux base images
- **Testing:** JUnit 5, Mockito, TestContainers, Vaadin TestBench

### Security Model
- **Authentication:** JWT tokens (15min access, 7day refresh)
- **Password Hashing:** BCrypt
- **Rate Limiting:** Redis-backed with sliding window
- **Authorization:** Spring Security with Bearer token validation
- **Session Management:** Redis-backed sessions with 30-minute timeout

### Performance Considerations
- **Message Latency:** <100ms (p95) for chat
- **Recommendation Performance:** ~500ms initial, <50ms cached
- **Rate Limiting:** 100-1000 req/min based on user tier
- **Circuit Breaker:** 50% failure rate triggers OPEN state
- **Caching Strategy:** Multi-level (Redis L2 cache, 24h TTL for feeds/recommendations)

### Development Approach
- **Monorepo:** All services in single repository
- **Multi-module Maven:** Parent POM with 7 modules
- **Docker Compose:** Complete local development environment with 8 services
- **Health Checks:** All services expose /actuator/health endpoints
- **Logging:** Centralized with DEBUG level for dating components
- **Profiles:** dev/test/prod with environment-specific configurations

---

## 🎯 Project Status

**Current Phase:** Architecture Planning & Skeleton Implementation
**Last Updated:** 2025-11-11
**Major Decision:** Transitioned from React/TypeScript to Vaadin (Pure Java) for 3-week MVP timeline
**Active Branch:** claude/parallel-multi-agent-system-01QbYzoG4YwzfHLTqFWxawCJ
**Implementation Status:**
- ✅ Backend services: Skeleton implementations complete
- ✅ Vaadin UI Service: Complete working skeleton with all views
- ✅ Database schema: Initialization script ready
- ✅ Docker Compose: Full orchestration configured
- ⏳ WebSocket real-time chat: TODO in Vaadin views
- ⏳ Full microservice implementation: In progress
- ⏳ Production deployment: Not started

---

## 📝 Notes

1. **Frontend Technology Decision:** The project originally planned React/TypeScript frontend but transitioned to Vaadin (Pure Java) on 2025-11-11 due to team expertise alignment and 3-week MVP timeline constraints. The React code remains for reference but is officially deprecated.

2. **Documentation Organization:** All documentation has clear status markers (ACTIVE/REFERENCE/DEPRECATED) and is organized by role-based navigation in DOCUMENT_INDEX.md.

3. **Microservices Maturity:** The architecture follows industry best practices from competitor analysis (Tinder, Bumble, Hinge, OkCupid) while avoiding over-engineering. Recommended 10-20 services initially vs Tinder's 500+.

4. **Database Design:** PostgreSQL chosen over NoSQL (MongoDB/DynamoDB) based on competitor analysis showing PostgreSQL as optimal for dating apps (Hinge's approach validated).

5. **Event-Driven Architecture:** RabbitMQ message broker enables loose coupling between services with published events: user:registered/updated/deleted, match:created/ended, swipe:recorded, recommendation:generated/accepted/rejected.

6. **Testing Strategy:** Comprehensive testing with TestContainers for integration tests, ensuring database, Redis, and RabbitMQ work correctly in realistic environments.

7. **Security Compliance:** Documentation includes GDPR/CCPA compliance analysis with real enforcement cases (Bumble £32M settlement), emphasizing importance of proper security implementation.

8. **Performance Benchmarks:** Documentation includes metrics from major dating apps: Tinder 2B daily swipes, Hinge 50K API calls/min, message latency targets <100ms p95.

---

**End of Repository Inventory**
