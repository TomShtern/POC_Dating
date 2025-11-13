# Implementation Guide - POC Dating Application

## Overview

The project skeleton is now complete with stubs and placeholders. This guide helps you understand what needs to be implemented and where.

---

## Backend Implementation Guide

### Structure Created

```
backend/
├── common-library/
│   └── src/main/java/com/dating/common/
│       ├── entity/
│       │   └── User.java (STUB)
│       ├── dto/
│       │   └── UserResponse.java (STUB)
│       └── exception/
│           └── DatingAppException.java (STUB)
├── user-service/
│   └── src/main/java/com/dating/user/
│       ├── controller/
│       │   ├── AuthController.java (STUB)
│       │   └── UserController.java (STUB)
│       ├── service/
│       │   └── AuthService.java (STUB - interface)
│       └── repository/
│           └── UserRepository.java (STUB - interface)
├── match-service/
│   └── src/main/java/com/dating/match/
│       └── controller/
│           ├── SwipeController.java (STUB)
│           └── FeedController.java (STUB)
├── chat-service/
│   └── src/main/java/com/dating/chat/
│       └── handler/
│           └── WebSocketHandler.java (STUB)
├── recommendation-service/
│   └── src/main/java/com/dating/recommendation/
│       └── controller/
│           └── RecommendationController.java (STUB)
└── api-gateway/
    └── src/main/java/com/dating/gateway/
        └── config/
            └── GatewayConfiguration.java (STUB)
```

### What Each File Needs

#### Common Library (Shared Code)

**User.java** (Entity)
- [ ] Add @Entity, @Table, @Data annotations
- [ ] Implement all fields from database schema
- [ ] Add relationships (@OneToOne, @OneToMany)
- [ ] Add validation annotations
- [ ] Add constructors

**UserResponse.java** (DTO)
- [ ] Add all fields (id, email, username, firstName, etc.)
- [ ] Create constructors for different use cases
- [ ] Add helper methods (getAge(), isActive(), etc.)

**DatingAppException.java** (Base Exception)
- [ ] Add fields: errorCode, statusCode
- [ ] Create multiple constructors
- [ ] Add getters for error details
- [ ] Create specific exception subclasses:
  - [ ] UserNotFoundException
  - [ ] UnauthorizedException
  - [ ] ValidationException
  - [ ] MatchNotFoundException

#### User Service

**AuthController.java**
- [ ] Inject services (@Autowired)
- [ ] Implement POST /register endpoint
- [ ] Implement POST /login endpoint
- [ ] Implement POST /refresh endpoint
- [ ] Implement POST /logout endpoint
- [ ] Add @Valid annotations on request bodies
- [ ] Add error handling

**UserController.java**
- [ ] Inject UserService, PreferencesService
- [ ] Implement GET /users/{userId}
- [ ] Implement PUT /users/{userId}
- [ ] Implement DELETE /users/{userId}
- [ ] Implement GET /users/{userId}/preferences
- [ ] Implement PUT /users/{userId}/preferences
- [ ] Add @PreAuthorize annotations
- [ ] Add caching with @Cacheable

**AuthService.java** (Interface)
- [ ] Create AuthServiceImpl implementation
- [ ] Implement register() method
- [ ] Implement login() method
- [ ] Implement refreshToken() method
- [ ] Implement logout() method
- [ ] Add BCrypt password hashing
- [ ] Add JWT token generation
- [ ] Publish RabbitMQ events

**UserRepository.java**
- [ ] Add custom methods:
  - [ ] findByEmail(String email)
  - [ ] findByUsername(String username)
  - [ ] findByStatusOrderByCreatedAtDesc()

#### Match Service

**SwipeController.java**
- [ ] Implement POST /swipes (record swipe)
- [ ] Implement GET /swipes/{userId} (swipe history)
- [ ] Add rate limiting
- [ ] Publish swipe:recorded event

**FeedController.java**
- [ ] Implement GET /feed/{userId} (get recommendations)
- [ ] Implement GET /matches (get all matches)
- [ ] Add caching
- [ ] Implement feed generation algorithm

#### Chat Service

**WebSocketHandler.java**
- [ ] Configure WebSocket endpoint
- [ ] Implement JWT authentication for WebSocket
- [ ] Handle SEND_MESSAGE events
- [ ] Handle MARK_AS_READ events
- [ ] Handle TYPING_START/STOP events
- [ ] Configure RabbitMQ STOMP relay
- [ ] Persist messages to database

#### Recommendation Service

**RecommendationController.java**
- [ ] Implement GET /recommendations (get recommendations)
- [ ] Implement GET /score (get compatibility score)
- [ ] Implement POST /feedback (record feedback)
- [ ] Add caching
- [ ] Implement scoring algorithm

#### API Gateway

**GatewayConfiguration.java**
- [ ] Configure route definitions
- [ ] Implement JWT authentication filter
- [ ] Implement CORS filter
- [ ] Implement rate limiting filter
- [ ] Implement logging filter
- [ ] Implement circuit breaker with Resilience4j
- [ ] Configure WebSocket routes

### Additional Backend Files to Create

Create these files in addition to the stubs:

**Common Library:**
- [ ] More entities: Match, Swipe, Message, Preference, MatchScore
- [ ] More DTOs: LoginRequest, RegisterRequest, SwipeRequest, MessageResponse
- [ ] Exception subclasses: All specific exceptions
- [ ] Utilities: JwtProvider, PasswordUtils, DateUtils
- [ ] Configuration: JpaConfig, CacheConfig

**Each Service:**
- [ ] ServiceImpl classes (implementations of interfaces)
- [ ] More repositories for other entities
- [ ] More controllers for all endpoints
- [ ] Configuration classes
- [ ] Event publishers/consumers for RabbitMQ
- [ ] Request/Response DTOs for all endpoints

**Cross-Service:**
- [ ] GlobalExceptionHandler
- [ ] Security configuration (Spring Security)
- [ ] Actuator endpoints for health checks
- [ ] Logging configuration

---

## Frontend Implementation Guide

### Structure Created

```
frontend/
└── src/
    ├── main.tsx (STUB - Entry point)
    ├── App.tsx (STUB - Root component)
    ├── types/
    │   └── auth.ts (STUB - Type definitions)
    ├── services/
    │   └── api.ts (STUB - API client)
    ├── store/
    │   └── authStore.ts (STUB - State management)
    ├── components/
    │   └── common/
    │       ├── Button.tsx (STUB)
    │       └── Input.tsx (STUB)
    └── pages/
        ├── LoginPage.tsx (STUB)
        ├── SwipePage.tsx (STUB)
        └── ChatPage.tsx (STUB)
```

### What Each File Needs

**main.tsx**
- [ ] Import React and ReactDOM
- [ ] Import App component
- [ ] Create root element
- [ ] Render App to DOM

**App.tsx**
- [ ] Setup BrowserRouter
- [ ] Define all routes
- [ ] Add authentication check
- [ ] Initialize Zustand stores
- [ ] Add error boundary

**types/auth.ts**
- [ ] Define User interface
- [ ] Define AuthResponse interface
- [ ] Define LoginRequest interface
- [ ] Define RegisterRequest interface
- [ ] Define AuthState interface

**services/api.ts**
- [ ] Create axios instance
- [ ] Configure base URL (from env)
- [ ] Add request interceptor (JWT)
- [ ] Add response interceptor (error handling)
- [ ] Export typed API methods

**store/authStore.ts**
- [ ] Define state interface
- [ ] Create Zustand store
- [ ] Implement setUser()
- [ ] Implement setToken()
- [ ] Implement login()
- [ ] Implement register()
- [ ] Implement logout()
- [ ] Add localStorage persistence
- [ ] Add token refresh logic

**components/common/Button.tsx**
- [ ] Define props interface
- [ ] Implement variants (primary, secondary, danger, outline)
- [ ] Implement sizes (sm, md, lg)
- [ ] Add loading state
- [ ] Add disabled state
- [ ] Style with Tailwind

**components/common/Input.tsx**
- [ ] Define props interface
- [ ] Add label
- [ ] Add error display
- [ ] Add focus states
- [ ] Add validation feedback
- [ ] Style with Tailwind

**pages/LoginPage.tsx**
- [ ] Create login form
- [ ] Add email and password inputs
- [ ] Implement validation
- [ ] Connect to authStore.login()
- [ ] Add loading state
- [ ] Handle errors
- [ ] Add link to register page

**pages/SwipePage.tsx**
- [ ] Load feed from API
- [ ] Display profile card
- [ ] Implement like button
- [ ] Implement pass button
- [ ] Handle swipes (POST API)
- [ ] Show match notifications
- [ ] Load next card
- [ ] Add infinite scroll

**pages/ChatPage.tsx**
- [ ] Connect to WebSocket
- [ ] Load message history
- [ ] Display messages
- [ ] Implement message input
- [ ] Send messages via WebSocket
- [ ] Show typing indicators
- [ ] Mark messages as read

### Additional Frontend Files to Create

**More Pages:**
- [ ] RegisterPage.tsx
- [ ] MatchesPage.tsx
- [ ] ProfilePage.tsx
- [ ] PreferencesPage.tsx
- [ ] NotFoundPage.tsx
- [ ] HomePage.tsx

**More Components:**
- [ ] SwipeCard.tsx (individual card)
- [ ] SwipeStack.tsx (card stack)
- [ ] MessageList.tsx (chat messages)
- [ ] MessageInput.tsx (chat input)
- [ ] Header.tsx (navigation)
- [ ] Footer.tsx
- [ ] Modal.tsx
- [ ] Toast.tsx (notifications)
- [ ] Avatar.tsx

**More Stores (Zustand):**
- [ ] userStore.ts
- [ ] matchStore.ts
- [ ] chatStore.ts
- [ ] uiStore.ts
- [ ] recommendationStore.ts

**More Services:**
- [ ] userService.ts (API calls)
- [ ] matchService.ts
- [ ] chatService.ts
- [ ] recommendationService.ts
- [ ] webSocketService.ts

**More Types:**
- [ ] match.ts
- [ ] chat.ts
- [ ] recommendation.ts
- [ ] common.ts

**Styles:**
- [ ] globals.css (Tailwind imports)
- [ ] index.css
- [ ] variables.css (custom colors/spacing)

---

## Implementation Priority

### Phase 1: Foundation (Week 1)
Start with these to get services running:

**Backend Priority:**
1. Common Library entities (User, Match, Message)
2. Common Library exceptions
3. User Service - AuthService implementation
4. User Service - UserRepository implementation
5. User Service - AuthController with endpoints
6. API Gateway - Basic routing configuration

**Frontend Priority:**
1. main.tsx and App.tsx setup
2. Auth types and API service
3. authStore implementation
4. LoginPage and RegisterPage
5. Basic Button and Input components

### Phase 2: Core Features (Weeks 2-3)

**Backend:**
1. Match Service - SwipeController and logic
2. Match Service - FeedController and algorithm
3. Chat Service - WebSocketHandler
4. Message entity and repository
5. RabbitMQ event publishers

**Frontend:**
1. SwipePage (matching interface)
2. SwipeCard and card stack
3. ChatPage with WebSocket
4. MatchesPage listing

### Phase 3: Enhancement (Weeks 4+)

**Backend:**
1. Recommendation Service - Algorithm
2. API Gateway - Security filters
3. Error handling and validation
4. Testing infrastructure

**Frontend:**
1. Profile pages
2. Preferences settings
3. UI polish and animations
4. Testing components

---

## Key Points

1. **Follow the TODOs** - Each stub file has detailed TODO comments
2. **Read the docstrings** - They explain what each component does
3. **Use the README files** - Each service README explains architecture
4. **Test incrementally** - Build and test each piece before moving on
5. **Keep it simple** - Start minimal, add complexity gradually
6. **Reference the docs** - API-SPECIFICATION.md and ARCHITECTURE.md
7. **Don't skip types** - Define all TypeScript types before implementing
8. **Use dependency injection** - Spring DI for backend, props for React

---

## Testing Your Implementation

### Backend
```bash
# Build individual service
cd backend/user-service
mvn clean package

# Run tests
mvn test

# Start service
mvn spring-boot:run

# Test endpoint
curl http://localhost:8081/actuator/health
```

### Frontend
```bash
# Install dependencies
cd frontend
npm install

# Development
npm run dev

# Build for production
npm run build

# Run tests
npm run test
```

### Full Stack
```bash
# Start all services
docker-compose up

# Check if everything running
curl http://localhost:8080/actuator/health
```

---

## Common Patterns

### Adding a New Endpoint

1. Create controller method in Controller class
2. Add route mapping (@GetMapping, @PostMapping, etc.)
3. Create request/response DTOs
4. Inject required services
5. Implement logic in service layer
6. Add error handling
7. Add validation annotations
8. Add authorization checks
9. Write tests

### Adding a New Page

1. Create page component
2. Define types in types/ directory
3. Add routes in App.tsx
4. Create necessary store actions
5. Create API service methods
6. Create smaller components if needed
7. Add tests

---

Good luck with the implementation! Start with Phase 1 and build incrementally.
