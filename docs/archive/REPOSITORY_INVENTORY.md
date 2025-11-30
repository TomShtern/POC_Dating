# POC Dating Application - Repository Inventory

**Generated:** 2025-11-18
**Repository:** POC_Dating
**Purpose:** Comprehensive file inventory and repository structure documentation

> **Quick Navigation:** Use the collapsible sections below to browse different parts of the codebase. Click on any file link to navigate directly to the source.

---

## Table of Contents

- [Repository Overview](#-repository-overview)
- [Root Level Documentation](#-root-level-documentation)
- [Backend Architecture](#%EF%B8%8F-backend-architecture)
- [API Gateway Service](#-api-gateway-service)
- [Common Library](#-common-library)
- [User Service](#-user-service)
- [Match Service](#-match-service)
- [Chat Service](#-chat-service)
- [Recommendation Service](#-recommendation-service)
- [Vaadin UI Service](#-vaadin-ui-service)
- [Database & Infrastructure](#%EF%B8%8F-database--infrastructure)
- [Documentation](#-documentation-docs)
- [Frontend (Deprecated)](#-frontend-deprecated)
- [Summary Statistics](#-summary-statistics)
- [Key Architecture Insights](#-key-architecture-insights)
- [Project Status](#-project-status)

---

## 📊 Repository Overview

| Metric | Value |
|--------|-------|
| **Total Files** | 94 files |
| **Repository Size** | ~1.2 MB |
| **Primary Language** | Java (Spring Boot microservices) |
| **Frontend** | Vaadin (Pure Java UI) |
| **Architecture** | Microservices with API Gateway pattern |

---

## 📁 Root Level Documentation

<details>
<summary><strong>Click to expand root documentation files (10 files)</strong></summary>

### [DATING_APPS_COMPARISON.md](../research/DATING_APPS_COMPARISON.md)
Comprehensive side-by-side technical comparison of Tinder, Bumble, OkCupid, and Hinge architectures with complexity analysis and ideal 2025 stack recommendations.
- **Metadata:** 27,939 bytes | 822 lines
- **Key Notes:** Architecture grades (Tinder B+, Bumble B-, OkCupid C+, Hinge B), recommends Go + PostgreSQL + React Native

### [BUMBLE_TECH_ANALYSIS.md](../research/BUMBLE_TECH_ANALYSIS.md)
Technical deep-dive into Bumble's architecture including Bumble 2.0 cloud-native transformation and polyglot backend challenges.
- **Metadata:** 13,150 bytes | 372 lines
- **Key Notes:** Critical issue with 6+ programming languages creating operational chaos

### [OKCUPID_TECH_ANALYSIS.md](../research/OKCUPID_TECH_ANALYSIS.md)
Analysis of OkCupid's 20-year journey from custom OKWS C++ web server to modern GraphQL stack.
- **Metadata:** 18,039 bytes | 506 lines
- **Key Notes:** Cautionary tale of custom infrastructure becoming technical debt

### [HINGE_TECH_ANALYSIS.md](../research/HINGE_TECH_ANALYSIS.md)
Analysis of Hinge's pragmatic stack using React Native/Flutter, Python/Django backend, PostgreSQL, and Gale-Shapley matching algorithm.
- **Metadata:** 20,275 bytes | 557 lines
- **Key Notes:** Most correct database choice (PostgreSQL); performance ceiling at 100M+ users

### [DEVOPS_TESTING_ML_INFRASTRUCTURE.md](../research/DEVOPS_TESTING_ML_INFRASTRUCTURE.md)
Detailed analysis of CI/CD practices, testing methodologies, A/B testing frameworks, and ML infrastructure for dating apps.
- **Metadata:** 18,876 bytes | 699 lines
- **Key Notes:** Tinder's TinVec embeddings, Smart Photos optimization; quality infrastructure cost $2-11M/year

### [SECURITY_COMPLIANCE_ANALYSIS.md](../research/SECURITY_COMPLIANCE_ANALYSIS.md)
Comprehensive security architecture covering authentication, encryption (AES-256, TLS 1.3), GDPR/CCPA compliance with real enforcement cases.
- **Metadata:** 16,236 bytes | 571 lines
- **Key Notes:** MFA reduces fake profiles by 89%; GDPR fines up to €20M or 4% revenue

### [README.md](../../README.md)
Primary project documentation with architecture overview, quick start guide, technology stack details, and development instructions.
- **Metadata:** 33,397 bytes | 888 lines
- **Key Notes:** Documents switch from React to Vaadin (pure Java UI); Spring Boot 3.x microservices

### [PERFORMANCE_SCALE_METRICS.md](../research/PERFORMANCE_SCALE_METRICS.md)
Performance metrics and scalability analysis with infrastructure cost estimates and team structures.
- **Metadata:** 11,808 bytes | 428 lines
- **Key Notes:** Tinder processes 2B daily swipes; infrastructure costs $60-180M/year

### [TINDER_TECH_ANALYSIS.md](../research/TINDER_TECH_ANALYSIS.md)
Technical deep-dive into Tinder's 500+ microservices architecture with TAG API Gateway and service mesh.
- **Metadata:** 13,713 bytes | 418 lines
- **Key Notes:** Over-engineered with 10x more services than needed

### [TECHNICAL_EVOLUTION_INTEGRATIONS.md](../research/TECHNICAL_EVOLUTION_INTEGRATIONS.md)
Timeline of technical evolution for major dating apps (2004-2025) documenting migrations and lessons learned.
- **Metadata:** 20,905 bytes | 693 lines
- **Key Notes:** Successful migrations: 6-24 months; aggressive timelines with 50%+ engineering allocation succeed

</details>

---

## 🏗️ Backend Architecture

<details>
<summary><strong>Parent Configuration (1 file)</strong></summary>

### [backend/pom.xml](backend/pom.xml)
Parent Maven POM centralizing dependency management for all microservices.
- **Metadata:** 10,302 bytes | 285 lines
- **Key Notes:** Spring Boot 3.2.0, Java 21, declares 7 modules, manages versions for Spring Cloud, JWT, PostgreSQL, Redis, RabbitMQ, TestContainers

</details>

---

## 🔌 API Gateway Service

<details>
<summary><strong>Click to expand API Gateway files (3 files)</strong></summary>

### [backend/api-gateway/Dockerfile](backend/api-gateway/Dockerfile)
Multi-stage Docker build for API Gateway using Maven builder and Alpine JRE runtime.
- **Metadata:** 684 bytes | 17 lines
- **Key Notes:** Port 8080, health checks every 30s, JVM memory -Xmx512m, non-root user execution

### [backend/api-gateway/pom.xml](backend/api-gateway/pom.xml)
Maven configuration for API Gateway with Spring Cloud Gateway, JWT validation, circuit breaking.
- **Metadata:** 3,616 bytes | 108 lines
- **Key Notes:** Routes to 4 backend services (User:8081, Match:8082, Chat:8083, Recommendation:8084), includes Resilience4j, Eureka client

### [backend/api-gateway/README.md](backend/api-gateway/README.md)
Comprehensive documentation covering routing rules, authentication flow, rate limiting, circuit breaker patterns.
- **Metadata:** 5,965 bytes | 287 lines
- **Key Notes:** Redis-backed rate limiting, 50% failure rate triggers circuit breaker, WebSocket support via STOMP/RabbitMQ

</details>

---

## 📚 Common Library

<details>
<summary><strong>Click to expand Common Library files (2 files)</strong></summary>

### [backend/common-library/pom.xml](backend/common-library/pom.xml)
Shared utility library Maven configuration for DTOs, entities, exceptions used across all microservices.
- **Metadata:** 2,628 bytes | 69 lines
- **Key Notes:** Minimal dependencies (JPA, Lombok), NOT a microservice itself, enforces DRY principle

### [backend/common-library/README.md](backend/common-library/README.md)
Documentation outlining purpose, intended contents, and usage patterns for shared library.
- **Metadata:** 2,123 bytes | 77 lines
- **Key Notes:** Contains entities, DTOs, custom exceptions, enums, validators; NO business logic or repositories

</details>

---

## 👤 User Service

<details>
<summary><strong>Click to expand User Service files (4 files)</strong></summary>

### [backend/user-service/README.md](backend/user-service/README.md)
Complete documentation for user authentication and profile management service.
- **Metadata:** 4,723 bytes | 182 lines
- **Key Notes:** JWT tokens (15min access, 7day refresh), BCrypt password hashing, publishes user:registered/updated/deleted events

### [backend/user-service/Dockerfile](backend/user-service/Dockerfile)
Multi-stage Dockerfile with extensive inline comments explaining architectural choices.
- **Metadata:** 1,992 bytes | 68 lines
- **Key Notes:** Port 8081, Maven 3.9 + JDK 21 builder, Alpine JRE runtime, health checks

### [backend/user-service/pom.xml](backend/user-service/pom.xml)
Service-specific Maven configuration for authentication and profile management.
- **Metadata:** 4,168 bytes | 130 lines
- **Key Notes:** Dependencies: PostgreSQL, Redis, JWT (jjwt), RabbitMQ, TestContainers

### [backend/user-service/src/main/resources/application.yml](backend/user-service/src/main/resources/application.yml)
Spring Boot configuration for server, database, cache, messaging, security, and logging.
- **Metadata:** 4,636 bytes | 186 lines
- **Key Notes:** Port 8081, HikariCP pool (max 20), Redis TTL 30min, supports dev/test/prod profiles

</details>

---

## 💑 Match Service

<details>
<summary><strong>Click to expand Match Service files (3 files)</strong></summary>

### [backend/match-service/README.md](backend/match-service/README.md)
Complete specification for swipe recording, match detection, feed generation, and scoring algorithm.
- **Metadata:** 5,280 bytes | 218 lines
- **Key Notes:** Match scoring: interest (0-40pts), age (0-30pts), preference (0-30pts); Redis feed caching 24h TTL

### [backend/match-service/Dockerfile](backend/match-service/Dockerfile)
Multi-stage Docker build for Match Service.
- **Metadata:** 745 bytes | 19 lines
- **Key Notes:** Port 8082, health checks every 30s, JVM -Xmx512m, Alpine JRE runtime

### [backend/match-service/pom.xml](backend/match-service/pom.xml)
Maven configuration with event-driven architecture dependencies.
- **Metadata:** 3,159 bytes | 98 lines
- **Key Notes:** High-frequency event handling, Spring Data Redis, RabbitMQ, publishes match:created/ended events

</details>

---

## 💬 Chat Service

<details>
<summary><strong>Click to expand Chat Service files (3 files)</strong></summary>

### [backend/chat-service/README.md](backend/chat-service/README.md)
Documentation for real-time messaging via WebSockets with delivery tracking and typing indicators.
- **Metadata:** 5,292 bytes | 228 lines
- **Key Notes:** <100ms message latency (p95), WebSocket + RabbitMQ for distributed handling, message status tracking

### [backend/chat-service/Dockerfile](backend/chat-service/Dockerfile)
Multi-stage Docker build for Chat Service.
- **Metadata:** 688 bytes | 17 lines
- **Key Notes:** Port 8083, health checks, JVM memory limits, non-root user

### [backend/chat-service/pom.xml](backend/chat-service/pom.xml)
Maven configuration with WebSocket, Redis, RabbitMQ dependencies.
- **Metadata:** 3,505 bytes | 112 lines
- **Key Notes:** Spring WebSocket, AMQP for message broker, PostgreSQL persistence, TestContainers

</details>

---

## 🎯 Recommendation Service

<details>
<summary><strong>Click to expand Recommendation Service files (3 files)</strong></summary>

### [backend/recommendation-service/README.md](backend/recommendation-service/README.md)
Comprehensive documentation of recommendation engine with algorithm details and caching strategy.
- **Metadata:** 5,907 bytes | 221 lines
- **Key Notes:** Rule-based matching formula, collaborative filtering planned, performance: ~500ms initial, <50ms cached

### [backend/recommendation-service/Dockerfile](backend/recommendation-service/Dockerfile)
Multi-stage Docker build for Recommendation Service.
- **Metadata:** 728 bytes | 18 lines
- **Key Notes:** Port 8084, health monitoring, Alpine base image

### [backend/recommendation-service/pom.xml](backend/recommendation-service/pom.xml)
Maven configuration with Apache Commons Math for future ML implementations.
- **Metadata:** 3,636 bytes | 117 lines
- **Key Notes:** PostgreSQL, Redis, RabbitMQ, Apache Commons Math 3.6.1, publishes recommendation events

</details>

---

## 🎨 Vaadin UI Service

<details>
<summary><strong>Click to expand Vaadin UI Service configuration (6 files)</strong></summary>

### [backend/vaadin-ui-service/README.md](backend/vaadin-ui-service/README.md)
Comprehensive documentation for Vaadin UI Service including architecture, setup, configuration, and troubleshooting.
- **Metadata:** 8,600 bytes | 384 lines
- **Key Notes:** Pure Java UI alternative to React, documents all views (Login, Register, Swipe, Matches, Messages, Profile)

### [backend/vaadin-ui-service/pom.xml](backend/vaadin-ui-service/pom.xml)
Maven configuration with Vaadin 24.3, Spring Cloud OpenFeign, Redis sessions, Spring Security.
- **Metadata:** 5,700 bytes | 176 lines
- **Key Notes:** Vaadin Spring Boot Starter, Feign clients for microservice communication, TestBench for testing

### [backend/vaadin-ui-service/Dockerfile](backend/vaadin-ui-service/Dockerfile)
Alpine Linux-based Docker image for Vaadin UI Service.
- **Metadata:** 380 bytes | 16 lines
- **Key Notes:** Port 8090, health checks via wget, Eclipse Temurin JRE 21

### [backend/vaadin-ui-service/src/main/resources/application.yml](backend/vaadin-ui-service/src/main/resources/application.yml)
Spring Boot configuration for Vaadin UI with Redis sessions and backend service URLs.
- **Metadata:** 1,100 bytes | 56 lines
- **Key Notes:** Port 8090, Redis session storage (30m timeout), Vaadin development mode

### [backend/vaadin-ui-service/frontend/themes/dating-theme/styles.css](backend/vaadin-ui-service/frontend/themes/dating-theme/styles.css)
Custom CSS theme with gradient backgrounds and component styling.
- **Metadata:** 1,900 bytes | 80 lines
- **Key Notes:** Color palette (Primary #667eea, Secondary #764ba2), profile card hover effects

### [backend/vaadin-ui-service/frontend/themes/dating-theme/theme.json](backend/vaadin-ui-service/frontend/themes/dating-theme/theme.json)
Vaadin theme configuration JSON.
- **Metadata:** ~50 bytes | 3 lines
- **Key Notes:** Configures dating-theme with Lumo variant

</details>

<details>
<summary><strong>Click to expand Vaadin UI main application (1 file)</strong></summary>

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/VaadinUIApplication.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/VaadinUIApplication.java)
Spring Boot entry point enabling Feign clients and configuring Vaadin theme.
- **Metadata:** 894 bytes | 27 lines
- **Key Notes:** @EnableFeignClients for microservices, AppShellConfigurator for theme setup, Lumo DARK variant

</details>

<details>
<summary><strong>Click to expand Feign Clients (4 files)</strong></summary>

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/client/UserServiceClient.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/client/UserServiceClient.java)
Interface for user authentication, profile management, and preferences.
- **Metadata:** 1,400 bytes | 36 lines
- **Key Notes:** Methods: login, register, getUser, updateUser, getPreferences, updatePreferences

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/client/MatchServiceClient.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/client/MatchServiceClient.java)
Interface for profile discovery, swiping, and match retrieval.
- **Metadata:** 1,100 bytes | 31 lines
- **Key Notes:** Methods: getNextProfile, recordSwipe, getMyMatches; returns SwipeResponse

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/client/ChatServiceClient.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/client/ChatServiceClient.java)
Interface for conversation management and messaging.
- **Metadata:** 1,400 bytes | 34 lines
- **Key Notes:** REST endpoints for messages, real-time via WebSocket (not REST)

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/client/RecommendationServiceClient.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/client/RecommendationServiceClient.java)
Interface for personalized recommendations.
- **Metadata:** 896 bytes | 24 lines
- **Key Notes:** Methods: getRecommendations with optional limit (default 20), refreshRecommendations

</details>

<details>
<summary><strong>Click to expand DTOs - Data Transfer Objects (12 files)</strong></summary>

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/User.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/User.java)
Complete user profile DTO with personal info, bio, photos, location, preferences.
- **Metadata:** 709 bytes | 33 lines
- **Key Notes:** Fields: id, email, username, name, age, gender, bio, photoUrl, location, preferences

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/Match.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/Match.java)
Match relationship DTO with user IDs and timestamps.
- **Metadata:** 432 bytes | 21 lines
- **Key Notes:** Fields: id, user1Id, user2Id, otherUser, createdAt, hasUnreadMessages

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/Message.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/Message.java)
Individual message DTO with sender info and status.
- **Metadata:** 453 bytes | 22 lines
- **Key Notes:** Fields: id, conversationId, senderId, senderName, text, createdAt, status

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/Conversation.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/Conversation.java)
Chat thread DTO with participants and unread count.
- **Metadata:** 434 bytes | 21 lines
- **Key Notes:** Fields: id, matchId, otherUser, lastMessage, unreadCount, createdAt

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/AuthResponse.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/AuthResponse.java)
Authentication response with tokens and user data.
- **Metadata:** 279 bytes | 14 lines
- **Key Notes:** Fields: accessToken, refreshToken, user; returned from login/register

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/LoginRequest.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/LoginRequest.java)
Login credentials request.
- **Metadata:** 246 bytes | 13 lines
- **Key Notes:** Fields: email, password

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/RegisterRequest.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/RegisterRequest.java)
New account registration request.
- **Metadata:** 421 bytes | 20 lines
- **Key Notes:** Fields: email, username, password, firstName, lastName, age, gender

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/SwipeRequest.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/SwipeRequest.java)
Swipe action request.
- **Metadata:** 257 bytes | 13 lines
- **Key Notes:** Fields: targetUserId, swipeType (LIKE/PASS/SUPER_LIKE)

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/SwipeResponse.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/SwipeResponse.java)
Swipe result indicating match status.
- **Metadata:** 277 bytes | 14 lines
- **Key Notes:** Fields: match (boolean), matchId, matchedUser

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/SwipeType.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/SwipeType.java)
Enumeration of swipe actions.
- **Metadata:** 89 bytes | 7 lines
- **Key Notes:** Values: LIKE, PASS, SUPER_LIKE

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/SendMessageRequest.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/SendMessageRequest.java)
Message text request.
- **Metadata:** 222 bytes | 12 lines
- **Key Notes:** Field: text (message content)

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/MessageStatus.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/dto/MessageStatus.java)
Message delivery/read states enumeration.
- **Metadata:** 92 bytes | 7 lines
- **Key Notes:** Values: SENT, DELIVERED, READ

</details>

<details>
<summary><strong>Click to expand Security Layer (2 files)</strong></summary>

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/security/SecurityConfig.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/security/SecurityConfig.java)
Spring Security configuration with Vaadin integration.
- **Metadata:** 1,300 bytes | 39 lines
- **Key Notes:** Extends VaadinWebSecurity, allows public /images/**, LoginView as entry point

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/security/SecurityUtils.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/security/SecurityUtils.java)
Session-based security utility for token and user identity management.
- **Metadata:** 2,300 bytes | 72 lines
- **Key Notes:** Methods: getCurrentUserId, getCurrentToken, isAuthenticated, setAuthenticationInfo

</details>

<details>
<summary><strong>Click to expand Service Layer (3 files)</strong></summary>

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/service/UserService.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/service/UserService.java)
Service layer wrapping UserServiceClient with session handling.
- **Metadata:** 2,800 bytes | 96 lines
- **Key Notes:** Methods: login, register, getCurrentUser, updateProfile, logout

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/service/MatchService.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/service/MatchService.java)
Service layer for swiping and match operations.
- **Metadata:** 2,300 bytes | 86 lines
- **Key Notes:** Methods: getNextProfile, recordSwipe, getMyMatches, getMatch

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/service/ChatService.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/service/ChatService.java)
Service layer for conversations and messages.
- **Metadata:** 2,200 bytes | 78 lines
- **Key Notes:** REST fallback for messages, real-time WebSocket in views (TODO)

</details>

<details>
<summary><strong>Click to expand Views - User Interface (7 files)</strong></summary>

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/views/LoginView.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/views/LoginView.java)
Authentication entry point with email/password form.
- **Metadata:** 4,600 bytes | 138 lines
- **Key Notes:** Route /login (anonymous), gradient background, stores token, navigates to SwipeView

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/views/RegisterView.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/views/RegisterView.java)
New user account creation with comprehensive validation.
- **Metadata:** 6,400 bytes | 186 lines
- **Key Notes:** Route /register (anonymous), validation: age >= 18, password >= 8 chars

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/views/SwipeView.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/views/SwipeView.java)
Main discovery interface with swipe actions and match notifications.
- **Metadata:** 5,800 bytes | 180 lines
- **Key Notes:** Route / (default, authenticated), ProfileCard component, Like/Pass/SuperLike buttons

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/views/MatchesView.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/views/MatchesView.java)
List view displaying all user matches in grid.
- **Metadata:** 2,100 bytes | 72 lines
- **Key Notes:** Route /matches (authenticated), Vaadin Grid with columns: Name, Age, Location, Matched On

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/views/MessagesView.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/views/MessagesView.java)
Conversation list with last message preview and unread count.
- **Metadata:** 2,400 bytes | 74 lines
- **Key Notes:** Route /messages (authenticated), TODO: ChatView with @Push for real-time

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/views/ProfileView.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/views/ProfileView.java)
User profile editor with personal information fields.
- **Metadata:** 4,000 bytes | 120 lines
- **Key Notes:** Route /profile (authenticated), fields: firstName, lastName, age, city, bio

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/views/MainLayout.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/views/MainLayout.java)
Application shell with header and navigation drawer.
- **Metadata:** 2,700 bytes | 81 lines
- **Key Notes:** Extends AppLayout, header with logo/username/logout, 4 nav items

</details>

<details>
<summary><strong>Click to expand Components (1 file)</strong></summary>

### [backend/vaadin-ui-service/src/main/java/com/dating/ui/components/ProfileCard.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/components/ProfileCard.java)
Reusable card component for user profiles.
- **Metadata:** 3,900 bytes | 128 lines
- **Key Notes:** Methods: setUser, showNoMoreProfiles; 400x400px image with placeholder fallback

</details>

---

## 🗄️ Database & Infrastructure

<details>
<summary><strong>Click to expand infrastructure files (2 files)</strong></summary>

### [db/init/01-schema.sql](db/init/01-schema.sql)
PostgreSQL initialization script with complete relational schema.
- **Metadata:** 9,915 bytes | 227 lines
- **Key Notes:** 11 tables (users, user_preferences, swipes, matches, match_scores, messages, refresh_tokens, recommendations, interaction_history, audit_logs), UUID primary keys, extensive indexes

### [docker-compose.yml](docker-compose.yml)
Orchestration for complete microservices stack with 8 services.
- **Metadata:** 9,766 bytes | 319 lines
- **Key Notes:** PostgreSQL (5432), Redis (6379), RabbitMQ (5672, 15672), API Gateway (8080), 4 backend services (8081-8084), Vaadin UI (8090)

</details>

---

## 📖 Documentation (docs/)

<details>
<summary><strong>Click to expand Active Documentation (7 files) - ✅ Current & Maintained</strong></summary>

### [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) ✅ ACTIVE
System architecture documentation for Vaadin approach with microservices design.
- **Metadata:** 24 KB | 791 lines
- **Key Notes:** Version 2.0 updated 2025-11-11 for Vaadin transition, defines API Gateway, microservices, Redis caching

### [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) ✅ ACTIVE
Complete development setup and workflow guide for Vaadin approach.
- **Metadata:** 13 KB | 668 lines
- **Key Notes:** Version 2.0 updated 2025-11-11, includes Docker Compose, Vaadin dev server, testing with Maven

### [docs/DOCUMENT_INDEX.md](docs/DOCUMENT_INDEX.md) ✅ ACTIVE
Master index with status markers and role-based navigation.
- **Metadata:** 10 KB | 306 lines
- **Key Notes:** Version 1.0 created 2025-11-11, role-based reading paths (developers, architects, DevOps)

### [docs/API-SPECIFICATION.md](docs/API-SPECIFICATION.md) ✅ ACTIVE
Complete REST API contract specification with examples.
- **Metadata:** 12 KB | 716 lines
- **Key Notes:** Base URL localhost:8080/api, comprehensive endpoints, error codes, rate limiting, cURL examples

### [docs/FRONTEND_OPTIONS_ANALYSIS.md](docs/FRONTEND_OPTIONS_ANALYSIS.md) ✅ REFERENCE
Comparative analysis of 6 frontend options justifying Vaadin selection.
- **Metadata:** 24 KB | 771 lines
- **Key Notes:** Crucial decision document, timelines: Vaadin 3 weeks vs React/TS 3-4 months

### [docs/VAADIN_IMPLEMENTATION.md](docs/VAADIN_IMPLEMENTATION.md) ✅ ACTIVE
Step-by-step guide for building Vaadin UI Service.
- **Metadata:** 34 KB | 1,222 lines
- **Key Notes:** Complete implementation roadmap, pom.xml templates, Feign clients, views, security, testing

### [docs/VAADIN_TRANSITION_SUMMARY.md](docs/VAADIN_TRANSITION_SUMMARY.md) ✅ COMPLETED
Transition overview from React to Vaadin with rationale.
- **Metadata:** 16 KB | 427 lines
- **Key Notes:** Before/after architecture, verification checklist, learning resources, future migration options

</details>

<details>
<summary><strong>Click to expand Reference Documentation (16 files) - 📋 Historical Context</strong></summary>

### [docs/01-competitor-analysis.md](docs/01-competitor-analysis.md) 📋 REFERENCE
Analysis of dating app competitors identifying market trends and features.
- **Metadata:** 11 KB | 301 lines
- **Key Notes:** Foundational research, key features, monetization models, security concerns

### [docs/02-recommended-architecture.md](docs/02-recommended-architecture.md) 📋 REFERENCE
Microservices architecture design with service boundaries and communication patterns.
- **Metadata:** 31 KB | 1,053 lines
- **Key Notes:** Architecture diagrams, defines 5 core services, API Gateway pattern

### [docs/03-tech-stack-recommendations.md](docs/03-tech-stack-recommendations.md) 📋 REFERENCE
Technology selection rationale for backend, data layer, infrastructure, and frontend.
- **Metadata:** 32 KB | 1,153 lines
- **Key Notes:** Originally recommended React (now Vaadin), covers Spring Boot 3.2, Java 21

### [docs/04-differentiators-best-practices.md](docs/04-differentiators-best-practices.md) 📋 REFERENCE
Unique features and industry best practices for dating apps.
- **Metadata:** 32 KB | 1,316 lines
- **Key Notes:** Strategy document for product differentiation, advanced matching, safety

### [docs/05-api-specification.md](docs/05-api-specification.md) 📋 REFERENCE
API endpoint specifications with REST design principles.
- **Metadata:** 31 KB | 1,446 lines
- **Key Notes:** Superseded by [docs/API-SPECIFICATION.md](docs/API-SPECIFICATION.md)

### [docs/06-database-schemas.md](docs/06-database-schemas.md) 📋 REFERENCE
PostgreSQL database schema design with ER diagrams.
- **Metadata:** 22 KB | 939 lines
- **Key Notes:** Core tables, PostGIS indexes for geospatial, soft-delete patterns

### [docs/07-matching-algorithm.md](docs/07-matching-algorithm.md) 📋 REFERENCE
Detailed matching algorithm design with compatibility scoring.
- **Metadata:** 23 KB | 850 lines
- **Key Notes:** Algorithm combines interests, age compatibility, location proximity, caching strategy

### [docs/08-realtime-messaging.md](docs/08-realtime-messaging.md) 📋 REFERENCE
Real-time messaging system design using WebSocket and message queuing.
- **Metadata:** 22 KB | 905 lines
- **Key Notes:** WebSocket with polling fallback, typing indicators, read receipts

### [docs/09-infrastructure-deployment.md](docs/09-infrastructure-deployment.md) 📋 REFERENCE
Complete infrastructure setup for Docker, Kubernetes, CI/CD, monitoring.
- **Metadata:** 25 KB | 1,212 lines
- **Key Notes:** Docker Compose for dev, Kubernetes for prod, Prometheus/Grafana monitoring

### [docs/10-backend-architecture-patterns.md](docs/10-backend-architecture-patterns.md) 📋 REFERENCE
Backend code organization using Clean Architecture and DDD patterns.
- **Metadata:** 33 KB | 1,266 lines
- **Key Notes:** Adapted for Java Spring Boot, repository pattern, testing strategies

### [docs/11-mobile-architecture.md](docs/11-mobile-architecture.md) 📋 REFERENCE
React Native mobile app architecture with state management.
- **Metadata:** 26 KB | 1,026 lines
- **Key Notes:** Phase 2 mobile development, performance optimization, WebSocket integration

### [docs/12-security-implementation.md](docs/12-security-implementation.md) 📋 REFERENCE
Comprehensive security with JWT refresh rotation, OAuth 2.0, password hashing.
- **Metadata:** 44 KB | 1,594 lines
- **Key Notes:** Critical implementation details, Argon2 passwords, AES-256 encryption

### [docs/13-performance-monitoring.md](docs/13-performance-monitoring.md) 📋 REFERENCE
APM setup with DataDog/New Relic, query optimization, caching strategies.
- **Metadata:** 33 KB | 1,334 lines
- **Key Notes:** EXPLAIN ANALYZE, index strategies, multi-level caching, k6 load testing

### [docs/14-java-backend-architecture.md](docs/14-java-backend-architecture.md) 📋 REFERENCE
Java/Spring Boot backend architecture with multi-module Maven structure.
- **Metadata:** 64 KB | 2,034 lines
- **Key Notes:** Comprehensive guide, Clean Architecture in Java, Lombok, MapStruct, Liquibase

### [docs/15-hybrid-architecture-java-nodejs.md](docs/15-hybrid-architecture-java-nodejs.md) 📋 REFERENCE
Hybrid architecture combining Java Spring Boot with Node.js services.
- **Metadata:** 34 KB | 1,189 lines
- **Key Notes:** Advanced architecture for scale, event sourcing, saga pattern, OpenTelemetry

### [docs/16-android-kotlin-architecture.md](docs/16-android-kotlin-architecture.md) 📋 REFERENCE
Native Android application architecture using Kotlin and Jetpack Compose.
- **Metadata:** 51 KB | 1,585 lines
- **Key Notes:** Phase 2 native Android, Clean Architecture, Hilt DI, Retrofit, Room database

</details>

---

## 🎨 Frontend (Deprecated)

<details>
<summary><strong>Click to expand deprecated frontend files (3 files) - ⚠️ DO NOT USE</strong></summary>

### [frontend/package.json](frontend/package.json) ⚠️ DEPRECATED
Project manifest for React-based frontend (DEPRECATED).
- **Metadata:** 927 bytes | 37 lines
- **Key Notes:** React 18, TypeScript 5.3, Vite 5.0; do NOT use for active development

### [frontend/README.md](frontend/README.md) ⚠️ DEPRECATED
Comprehensive React frontend architecture documentation (DEPRECATED).
- **Metadata:** 7,844 bytes | 313 lines
- **Key Notes:** Extensive guide for React/TypeScript/Vite architecture; kept for reference only

### [frontend/DEPRECATION_NOTICE.md](frontend/DEPRECATION_NOTICE.md) ⚠️ DEPRECATED
Critical status notice indicating React frontend replaced with Vaadin.
- **Metadata:** 7,108 bytes | 206 lines
- **Key Notes:** Transition rationale: 3-week timeline with Vaadin vs 3-4 months React/TS learning curve. Use [backend/vaadin-ui-service/](backend/vaadin-ui-service/) instead

</details>

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

| Component | Details | Documentation |
|-----------|---------|---------------|
| **API Gateway Pattern** | Single entry point (Port 8080) routing to 4 backend services | → See [backend/api-gateway/README.md](backend/api-gateway/README.md) |
| **Service Ports** | User (8081), Match (8082), Chat (8083), Recommendation (8084), Vaadin UI (8090) | → See [docker-compose.yml](docker-compose.yml) |
| **Event-Driven Communication** | RabbitMQ message broker for asynchronous events | → See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| **Service Discovery** | Netflix Eureka for dynamic service registration | → See [backend/api-gateway/pom.xml](backend/api-gateway/pom.xml) |

### Technology Stack

| Layer | Technology | Documentation |
|-------|------------|---------------|
| **Backend** | Spring Boot 3.2.0, Java 21 | → See [backend/pom.xml](backend/pom.xml) |
| **Frontend** | Vaadin 24.3 (Pure Java UI) | → See [docs/VAADIN_IMPLEMENTATION.md](docs/VAADIN_IMPLEMENTATION.md) |
| **Database** | PostgreSQL 15 with UUID primary keys, extensive indexes, JSONB | → See [db/init/01-schema.sql](db/init/01-schema.sql) |
| **Caching** | Redis for sessions, token blacklist, feed caching (24h TTL) | → See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| **Messaging** | RabbitMQ for event streaming between services | → See [docs/08-realtime-messaging.md](docs/08-realtime-messaging.md) |
| **Containerization** | Docker with multi-stage builds, Alpine Linux base images | → See [docker-compose.yml](docker-compose.yml) |
| **Testing** | JUnit 5, Mockito, TestContainers, Vaadin TestBench | → See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) |

### Security Model

| Feature | Implementation | Documentation |
|---------|----------------|---------------|
| **Authentication** | JWT tokens (15min access, 7day refresh) | → See [backend/user-service/README.md](backend/user-service/README.md) |
| **Password Hashing** | BCrypt | → See [docs/12-security-implementation.md](docs/12-security-implementation.md) |
| **Rate Limiting** | Redis-backed with sliding window | → See [backend/api-gateway/README.md](backend/api-gateway/README.md) |
| **Authorization** | Spring Security with Bearer token validation | → See [backend/vaadin-ui-service/src/main/java/com/dating/ui/security/SecurityConfig.java](backend/vaadin-ui-service/src/main/java/com/dating/ui/security/SecurityConfig.java) |
| **Session Management** | Redis-backed sessions with 30-minute timeout | → See [backend/vaadin-ui-service/src/main/resources/application.yml](backend/vaadin-ui-service/src/main/resources/application.yml) |

### Performance Considerations

| Metric | Target | Documentation |
|--------|--------|---------------|
| **Message Latency** | <100ms (p95) for chat | → See [backend/chat-service/README.md](backend/chat-service/README.md) |
| **Recommendation Performance** | ~500ms initial, <50ms cached | → See [backend/recommendation-service/README.md](backend/recommendation-service/README.md) |
| **Rate Limiting** | 100-1000 req/min based on user tier | → See [backend/api-gateway/README.md](backend/api-gateway/README.md) |
| **Circuit Breaker** | 50% failure rate triggers OPEN state | → See [backend/api-gateway/README.md](backend/api-gateway/README.md) |
| **Caching Strategy** | Multi-level (Redis L2 cache, 24h TTL) | → See [docs/13-performance-monitoring.md](docs/13-performance-monitoring.md) |

### Development Approach

| Aspect | Details | Documentation |
|--------|---------|---------------|
| **Monorepo** | All services in single repository | - |
| **Multi-module Maven** | Parent POM with 7 modules | → See [backend/pom.xml](backend/pom.xml) |
| **Docker Compose** | Complete local development environment with 8 services | → See [docker-compose.yml](docker-compose.yml) |
| **Health Checks** | All services expose /actuator/health endpoints | → See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) |
| **Logging** | Centralized with DEBUG level for dating components | → See [backend/user-service/src/main/resources/application.yml](backend/user-service/src/main/resources/application.yml) |
| **Profiles** | dev/test/prod with environment-specific configurations | → See service application.yml files |

---

## 🎯 Project Status

**Current Phase:** Architecture Planning & Skeleton Implementation
**Last Updated:** 2025-11-11
**Major Decision:** Transitioned from React/TypeScript to Vaadin (Pure Java) for 3-week MVP timeline

### Implementation Status

| Component | Status | Details |
|-----------|--------|---------|
| Backend services | ✅ Complete | Skeleton implementations complete |
| Vaadin UI Service | ✅ Complete | All views implemented |
| Database schema | ✅ Complete | Initialization script ready |
| Docker Compose | ✅ Complete | Full orchestration configured |
| WebSocket real-time chat | ⏳ In Progress | TODO in Vaadin views |
| Full microservice implementation | ⏳ In Progress | Business logic pending |
| Production deployment | ⏳ Not Started | Kubernetes/AWS pending |

---

## 📝 Notes

1. **Frontend Technology Decision:** The project originally planned React/TypeScript frontend but transitioned to Vaadin (Pure Java) on 2025-11-11 due to team expertise alignment and 3-week MVP timeline constraints. See [docs/FRONTEND_OPTIONS_ANALYSIS.md](../FRONTEND_OPTIONS_ANALYSIS.md) for detailed rationale.

2. **Documentation Organization:** All documentation has clear status markers (ACTIVE/REFERENCE/DEPRECATED) and is organized by role-based navigation in [docs/DOCUMENT_INDEX.md](../DOCUMENT_INDEX.md).

3. **Microservices Maturity:** The architecture follows industry best practices from competitor analysis (Tinder, Bumble, Hinge, OkCupid) while avoiding over-engineering. Recommended 10-20 services initially vs Tinder's 500+. See [DATING_APPS_COMPARISON.md](../research/DATING_APPS_COMPARISON.md).

4. **Database Design:** PostgreSQL chosen over NoSQL (MongoDB/DynamoDB) based on competitor analysis showing PostgreSQL as optimal for dating apps (Hinge's approach validated). See [HINGE_TECH_ANALYSIS.md](../research/HINGE_TECH_ANALYSIS.md).

5. **Event-Driven Architecture:** RabbitMQ message broker enables loose coupling between services. See [docs/ARCHITECTURE.md](../ARCHITECTURE.md) for event flow diagrams.

6. **Testing Strategy:** Comprehensive testing with TestContainers for integration tests. See [docs/DEVELOPMENT.md](../DEVELOPMENT.md) for testing guidelines.

7. **Security Compliance:** Documentation includes GDPR/CCPA compliance analysis with real enforcement cases. See [SECURITY_COMPLIANCE_ANALYSIS.md](../research/SECURITY_COMPLIANCE_ANALYSIS.md).

8. **Performance Benchmarks:** Documentation includes metrics from major dating apps. See [PERFORMANCE_SCALE_METRICS.md](../research/PERFORMANCE_SCALE_METRICS.md).

---

## 🔄 Auto-Regeneration

This inventory can be automatically regenerated using the pre-commit hook. To install:

```bash
# The hook is located at .git/hooks/pre-commit
# It runs scripts/generate-inventory.sh before each commit
chmod +x .git/hooks/pre-commit
chmod +x scripts/generate-inventory.sh
```

The inventory will be regenerated whenever files are added or removed from the repository.

---
