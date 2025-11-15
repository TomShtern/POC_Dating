# Code Patterns Reference - POC Dating Application

**Document Purpose:** Comprehensive reference guide for common code patterns used in the POC Dating codebase.

**Last Updated:** 2025-11-15
**Status:** Active Development - Vaadin UI Implementation Phase

---

## Table of Contents

1. [REST Controller Patterns](#rest-controller-patterns)
2. [Service Layer Patterns](#service-layer-patterns)
3. [Feign Client Patterns](#feign-client-patterns)
4. [Repository Patterns](#repository-patterns)
5. [Vaadin View Patterns](#vaadin-view-patterns)
6. [Entity Patterns](#entity-patterns)
7. [DTO Patterns](#dto-patterns)
8. [Exception Handling](#exception-handling)
9. [Security & Authentication](#security--authentication)
10. [Testing Patterns](#testing-patterns)

---

## REST Controller Patterns

### Pattern 1: Basic CRUD Controller (User Service)

This pattern represents the standard REST controller implementing CRUD operations with proper HTTP status codes and error handling.

```java
package com.dating.user.controller;

import com.dating.user.dto.UserRegistrationRequest;
import com.dating.user.dto.UserResponse;
import com.dating.user.dto.UserUpdateRequest;
import com.dating.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for user management operations
 * Handles registration, authentication, and profile management
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Register a new user
     *
     * @param request Registration details (email, password, name)
     * @return 201 Created with user profile and tokens
     * @throws UserAlreadyExistsException if email is already registered
     */
    @PostMapping("/auth/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        log.debug("Registering new user: {}", request.getEmail());
        UserResponse user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Login user with email and password
     *
     * @param request Email and password
     * @return 200 OK with JWT tokens
     * @throws UserNotFoundException if user doesn't exist
     * @throws InvalidPasswordException if password is incorrect
     */
    @PostMapping("/auth/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("Login attempt for email: {}", request.getEmail());
        UserResponse user = userService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(user);
    }

    /**
     * Get user by ID
     *
     * @param userId User UUID
     * @return 200 OK with user profile
     * @throws UserNotFoundException if user doesn't exist
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        log.debug("Getting user: {}", userId);
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Update user profile
     *
     * @param userId User UUID
     * @param request Updated user data
     * @return 200 OK with updated profile
     * @throws UserNotFoundException if user doesn't exist
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserUpdateRequest request) {
        log.debug("Updating user: {}", userId);
        UserResponse user = userService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }

    /**
     * Delete user account (soft delete)
     *
     * @param userId User UUID
     * @return 204 No Content
     * @throws UserNotFoundException if user doesn't exist
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        log.debug("Deleting user: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get user preferences
     *
     * @param userId User UUID
     * @return 200 OK with preferences
     */
    @GetMapping("/{userId}/preferences")
    public ResponseEntity<UserPreferencesResponse> getPreferences(@PathVariable UUID userId) {
        log.debug("Getting preferences for user: {}", userId);
        UserPreferencesResponse prefs = userService.getPreferences(userId);
        return ResponseEntity.ok(prefs);
    }

    /**
     * Update user preferences
     *
     * @param userId User UUID
     * @param request Updated preferences
     * @return 200 OK with updated preferences
     */
    @PutMapping("/{userId}/preferences")
    public ResponseEntity<UserPreferencesResponse> updatePreferences(
            @PathVariable UUID userId,
            @Valid @RequestBody UserPreferencesRequest request) {
        log.debug("Updating preferences for user: {}", userId);
        UserPreferencesResponse prefs = userService.updatePreferences(userId, request);
        return ResponseEntity.ok(prefs);
    }
}
```

**Key Points:**
- Use `@RequiredArgsConstructor` for dependency injection
- Add `@Slf4j` for logging
- Use meaningful path variables and request bodies
- Return appropriate HTTP status codes
- Include detailed JavaDoc for API documentation

---

### Pattern 2: Search/Filter Controller

Controller for complex queries with pagination and filtering.

```java
package com.dating.match.controller;

import com.dating.match.dto.UserProfileResponse;
import com.dating.match.dto.FeedFilterRequest;
import com.dating.match.service.FeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for feed and discovery operations
 */
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Slf4j
public class MatchController {

    private final FeedService feedService;

    /**
     * Get personalized feed of profiles to swipe on
     *
     * @param userId User requesting the feed
     * @param page Page number (0-indexed, default: 0)
     * @param size Page size (default: 20, max: 50)
     * @return 200 OK with paginated profiles
     */
    @GetMapping("/feed")
    public ResponseEntity<Page<UserProfileResponse>> getFeed(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Validate pagination parameters
        if (size > 50) size = 50; // Max 50 per page
        if (page < 0) page = 0;

        Pageable pageable = PageRequest.of(page, size);
        log.debug("Getting feed for user {} - page: {}, size: {}", userId, page, size);

        Page<UserProfileResponse> feed = feedService.generateFeed(userId, pageable);
        return ResponseEntity.ok(feed);
    }

    /**
     * Get filtered feed based on preferences
     *
     * @param userId User requesting the feed
     * @param filter Filter criteria (age range, distance, etc.)
     * @param page Page number
     * @param size Page size
     * @return 200 OK with filtered profiles
     */
    @PostMapping("/feed/filter")
    public ResponseEntity<Page<UserProfileResponse>> getFilteredFeed(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody FeedFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        log.debug("Getting filtered feed for user {} with filters: {}", userId, filter);

        Page<UserProfileResponse> feed = feedService.generateFilteredFeed(userId, filter, pageable);
        return ResponseEntity.ok(feed);
    }
}
```

**Key Points:**
- Use `@RequestParam` for pagination (page, size, sort)
- Use `@RequestBody` for complex filter objects
- Return `Page<T>` for paginated results
- Validate and constrain pagination parameters
- Log filter criteria for monitoring

---

## Service Layer Patterns

### Pattern 1: Basic Service with Dependency Injection

The service layer encapsulates business logic and coordinates between controllers and repositories.

```java
package com.dating.user.service;

import com.dating.user.dto.UserRegistrationRequest;
import com.dating.user.dto.UserResponse;
import com.dating.user.dto.UserUpdateRequest;
import com.dating.user.entity.User;
import com.dating.user.exception.UserAlreadyExistsException;
import com.dating.user.exception.UserNotFoundException;
import com.dating.user.mapper.UserMapper;
import com.dating.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service layer for user-related operations
 * Handles business logic for user management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtTokenProvider tokenProvider;

    /**
     * Register a new user
     *
     * @param request Registration request with email, password, name
     * @return User response with authentication tokens
     * @throws UserAlreadyExistsException if email already exists
     */
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new UserAlreadyExistsException("Email already registered");
        }

        // Create new user entity
        User user = User.builder()
            .id(UUID.randomUUID())
            .email(request.getEmail())
            .username(request.getUsername())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .status("ACTIVE")
            .build();

        // Save to database
        User savedUser = userRepository.save(user);
        log.debug("User registered successfully: {}", savedUser.getId());

        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(savedUser.getId());
        String refreshToken = tokenProvider.generateRefreshToken(savedUser.getId());

        // Map to response
        return userMapper.toResponse(savedUser, accessToken, refreshToken);
    }

    /**
     * Login user with email and password
     *
     * @param email User email
     * @param password Plain text password
     * @return User response with tokens
     * @throws UserNotFoundException if user doesn't exist
     * @throws InvalidPasswordException if password is incorrect
     */
    @Transactional(readOnly = true)
    public UserResponse login(String email, String password) {
        log.debug("Login attempt for email: {}", email);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + email));

        // Validate password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login failed - invalid password for: {}", email);
            throw new InvalidPasswordException("Invalid password");
        }

        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(user.getId());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());

        log.info("User logged in successfully: {}", user.getId());
        return userMapper.toResponse(user, accessToken, refreshToken);
    }

    /**
     * Get user by ID
     *
     * @param userId User UUID
     * @return User response
     * @throws UserNotFoundException if user doesn't exist
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        log.debug("Fetching user: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        return userMapper.toResponse(user);
    }

    /**
     * Update user profile
     *
     * @param userId User UUID
     * @param request Updated user data
     * @return Updated user response
     * @throws UserNotFoundException if user doesn't exist
     */
    @Transactional
    public UserResponse updateUser(UUID userId, UserUpdateRequest request) {
        log.debug("Updating user: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        // Update only provided fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        User updated = userRepository.save(user);
        log.info("User updated: {}", userId);

        return userMapper.toResponse(updated);
    }

    /**
     * Delete user account (soft delete)
     *
     * @param userId User UUID
     * @throws UserNotFoundException if user doesn't exist
     */
    @Transactional
    public void deleteUser(UUID userId) {
        log.debug("Deleting user: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        user.setStatus("DELETED");
        userRepository.save(user);

        log.info("User deleted (soft delete): {}", userId);
    }
}
```

**Key Points:**
- Use `@Transactional` for transaction management
- Use `@Transactional(readOnly = true)` for read-only operations
- Constructor injection via `@RequiredArgsConstructor`
- Throw specific exceptions rather than generic ones
- Log important business events at INFO level
- Log debug information at DEBUG level

---

### Pattern 2: Service with Caching

Service layer with Redis caching for expensive operations.

```java
package com.dating.match.service;

import com.dating.match.dto.UserProfileResponse;
import com.dating.match.entity.User;
import com.dating.match.repository.MatchRepository;
import com.dating.match.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for feed generation and user discovery
 * Implements caching for frequently accessed data
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final UserMapper userMapper;

    /**
     * Generate personalized feed for user
     * Results cached for 24 hours (TTL: 86400s)
     *
     * @param userId User requesting feed
     * @param pageable Pagination info
     * @return Paginated list of profile recommendations
     */
    @Cacheable(value = "user_feed", key = "#userId", unless = "#result.isEmpty()")
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> generateFeed(UUID userId, Pageable pageable) {
        log.debug("Generating feed for user: {}", userId);

        User currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Get candidates matching user preferences
        Page<User> candidates = userRepository.findCandidates(
            currentUser.getMinAge(),
            currentUser.getMaxAge(),
            currentUser.getMaxDistance(),
            userId,
            pageable
        );

        log.debug("Found {} candidates for user: {}", candidates.getTotalElements(), userId);

        // Score and sort candidates
        return candidates.map(candidate -> {
            double score = scoreProfile(candidate, currentUser);
            return userMapper.toProfileResponse(candidate, score);
        });
    }

    /**
     * Invalidate feed cache when user preferences change
     *
     * @param userId User whose preferences changed
     */
    @CacheEvict(value = "user_feed", key = "#userId")
    public void invalidateFeedCache(UUID userId) {
        log.debug("Invalidating feed cache for user: {}", userId);
    }

    /**
     * Score a profile based on user preferences
     *
     * @param candidate Profile to score
     * @param user User requesting the feed
     * @return Score between 0 and 100
     */
    private double scoreProfile(User candidate, User user) {
        double score = 50.0; // Base score

        // Age preference (±10 years bonus)
        int ageDiff = Math.abs(candidate.getAge() - user.getPreferredAge());
        if (ageDiff <= 5) score += 15;
        else if (ageDiff <= 10) score += 10;

        // Distance preference
        double distance = calculateDistance(candidate.getLocation(), user.getLocation());
        if (distance <= 10) score += 20;
        else if (distance <= 25) score += 10;

        // Shared interests (simplified)
        if (hasCommonInterests(candidate, user)) score += 15;

        return Math.min(score, 100.0);
    }

    private double calculateDistance(Location loc1, Location loc2) {
        // Haversine formula implementation
        return 0.0; // Simplified
    }

    private boolean hasCommonInterests(User candidate, User user) {
        // Check shared interests
        return true; // Simplified
    }
}
```

**Key Points:**
- Use `@Cacheable` for expensive operations
- Cache key includes relevant parameters
- Use `unless` to avoid caching empty results
- Use `@CacheEvict` to invalidate cache on updates
- Read-only transactions for queries
- Document cache TTL in comments

---

## Feign Client Patterns

### Pattern 1: HTTP Client with Feign

Feign clients abstract HTTP communication between microservices.

```java
package com.dating.ui.client;

import com.dating.ui.dto.AuthResponse;
import com.dating.ui.dto.LoginRequest;
import com.dating.ui.dto.RegisterRequest;
import com.dating.ui.dto.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign Client for User Service
 * Handles all HTTP communication with the User microservice
 */
@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserServiceClient {

    /**
     * Register a new user
     *
     * @param request Registration details
     * @return Auth response with tokens
     */
    @PostMapping("/api/users/auth/register")
    AuthResponse register(@RequestBody RegisterRequest request);

    /**
     * Login user
     *
     * @param request Login credentials
     * @return Auth response with tokens
     */
    @PostMapping("/api/users/auth/login")
    AuthResponse login(@RequestBody LoginRequest request);

    /**
     * Get user profile
     *
     * @param userId User UUID
     * @param authorization Bearer token
     * @return User profile
     */
    @GetMapping("/api/users/{userId}")
    User getUser(
        @PathVariable String userId,
        @RequestHeader("Authorization") String authorization);

    /**
     * Update user profile
     *
     * @param userId User UUID
     * @param user Updated user data
     * @param authorization Bearer token
     * @return Updated user
     */
    @PutMapping("/api/users/{userId}")
    User updateUser(
        @PathVariable String userId,
        @RequestBody User user,
        @RequestHeader("Authorization") String authorization);

    /**
     * Get user preferences
     *
     * @param userId User UUID
     * @param authorization Bearer token
     * @return User preferences
     */
    @GetMapping("/api/users/{userId}/preferences")
    User getPreferences(
        @PathVariable String userId,
        @RequestHeader("Authorization") String authorization);

    /**
     * Update user preferences
     *
     * @param userId User UUID
     * @param preferences Updated preferences
     * @param authorization Bearer token
     * @return Updated preferences
     */
    @PutMapping("/api/users/{userId}/preferences")
    User updatePreferences(
        @PathVariable String userId,
        @RequestBody User preferences,
        @RequestHeader("Authorization") String authorization);
}
```

**Key Points:**
- Use `@FeignClient` with service name and URL
- Methods mirror REST endpoints
- Use `@PathVariable` for path parameters
- Use `@RequestHeader` for authentication headers
- Return DTOs, not raw JSON
- Include detailed JavaDoc

---

### Pattern 2: Feign Client with Error Handling

Feign clients with custom error decoder for better exception handling.

```java
package com.dating.ui.client.config;

import com.dating.ui.exception.ApiException;
import com.dating.ui.exception.AuthenticationException;
import com.dating.ui.exception.BadRequestException;
import com.dating.ui.exception.ResourceNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom error decoder for Feign clients
 * Converts HTTP error responses to appropriate exceptions
 */
@Slf4j
public class CustomErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder delegate = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Feign client error [{}] - status: {}", methodKey, response.status());

        switch (response.status()) {
            case 400:
                return new BadRequestException("Invalid request: " + response.reason());
            case 401:
                return new AuthenticationException("Unauthorized: " + response.reason());
            case 403:
                return new AuthenticationException("Forbidden: " + response.reason());
            case 404:
                return new ResourceNotFoundException("Resource not found: " + response.reason());
            case 500:
                return new ApiException("Server error: " + response.reason());
            default:
                return delegate.decode(methodKey, response);
        }
    }
}

/**
 * Configuration for Feign clients
 */
@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // Add common headers, tracing, etc.
            template.header("X-Client-Version", "1.0");
            template.header("X-Request-ID", UUID.randomUUID().toString());
        };
    }
}
```

**Key Points:**
- Implement `ErrorDecoder` for HTTP error handling
- Map HTTP status codes to specific exceptions
- Add request interceptors for common headers
- Log all errors for debugging
- Throw meaningful exceptions to service layer

---

## Repository Patterns

### Pattern 1: JPA Repository with Custom Queries

```java
package com.dating.user.repository;

import com.dating.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

/**
 * Repository for User entity
 * Handles all database operations for users
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email
     *
     * @param email User email
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email already exists
     *
     * @param email Email to check
     * @return true if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find users by status
     *
     * @param status User status (ACTIVE, INACTIVE, BANNED)
     * @param pageable Pagination info
     * @return Paginated list of users
     */
    Page<User> findByStatus(String status, Pageable pageable);

    /**
     * Find users created after specific date
     * Custom JPQL query for complex filtering
     *
     * @param status User status
     * @param date Date threshold
     * @return List of users matching criteria
     */
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.createdAt > :date")
    List<User> findActiveUsersSince(
        @Param("status") String status,
        @Param("date") Instant date);

    /**
     * Find candidate profiles matching user preferences
     * Used for feed generation
     *
     * @param minAge Minimum age
     * @param maxAge Maximum age
     * @param maxDistance Maximum distance in km
     * @param userId Current user (to exclude)
     * @param pageable Pagination info
     * @return Paginated list of candidates
     */
    @Query(value = """
        SELECT u FROM User u
        WHERE u.status = 'ACTIVE'
          AND u.id != :userId
          AND u.age BETWEEN :minAge AND :maxAge
          AND u.gender = :preferredGender
          AND (6371 * acos(cos(radians(:latitude)) * cos(radians(u.latitude)) *
               cos(radians(u.longitude) - radians(:longitude)) +
               sin(radians(:latitude)) * sin(radians(u.latitude)))) <= :maxDistance
          AND u.id NOT IN (
              SELECT CASE
                WHEN s.userId = :userId THEN s.targetUserId
                ELSE s.userId
              END
              FROM Swipe s
              WHERE (s.userId = :userId OR s.targetUserId = :userId)
          )
        ORDER BY u.updatedAt DESC
        """)
    Page<User> findCandidates(
        @Param("minAge") Integer minAge,
        @Param("maxAge") Integer maxAge,
        @Param("maxDistance") Integer maxDistance,
        @Param("userId") UUID userId,
        @Param("preferredGender") String preferredGender,
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        Pageable pageable);

    /**
     * Find users with unread messages
     *
     * @return List of users with unread messages
     */
    @Query("""
        SELECT DISTINCT u FROM User u
        WHERE EXISTS (
            SELECT 1 FROM Message m
            WHERE m.receiverId = u.id AND m.status = 'UNREAD'
        )
        """)
    List<User> findUsersWithUnreadMessages();

    /**
     * Count users created in last N days
     *
     * @param days Number of days
     * @return Count of new users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= CURRENT_TIMESTAMP - :days * INTERVAL '1 day'")
    long countNewUsersLastDays(@Param("days") Integer days);
}
```

**Key Points:**
- Extend `JpaRepository<T, ID>` for CRUD operations
- Use derived query methods for simple queries (findByEmail)
- Use `@Query` for complex queries (JPQL or native SQL)
- Use `@Param` for named parameters
- Include pagination with `Pageable`
- Write descriptive JavaDoc with parameter documentation
- Use Optional for queries returning single results

---

### Pattern 2: Specification-based Repository (Advanced Filtering)

```java
package com.dating.match.repository;

import com.dating.match.entity.User;
import com.dating.match.entity.User_;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications for complex User queries
 * Used for dynamic filtering in feed generation
 */
public class UserSpecifications {

    /**
     * User has minimum age
     */
    public static Specification<User> minAge(Integer age) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(User_.age), age);
    }

    /**
     * User has maximum age
     */
    public static Specification<User> maxAge(Integer age) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(User_.age), age);
    }

    /**
     * User has specific gender
     */
    public static Specification<User> hasGender(String gender) {
        return (root, query, cb) -> cb.equal(root.get(User_.gender), gender);
    }

    /**
     * User is within distance
     */
    public static Specification<User> withinDistance(Double lat, Double lon, Double distance) {
        return (root, query, cb) -> {
            // Haversine formula in SQL
            // Simplified for example
            return cb.lessThanOrEqualTo(
                root.get("distance"),
                distance
            );
        };
    }

    /**
     * User is not already swiped on
     */
    public static Specification<User> notSwiped(java.util.UUID userId) {
        return (root, query, cb) -> {
            // Exclude users already swiped by current user
            return cb.not(cb.in(root.get(User_.id))
                .value(/* subquery for swiped users */));
        };
    }

    /**
     * Combine specifications with AND logic
     */
    public static Specification<User> buildFeedCriteria(
            Integer minAge, Integer maxAge, String gender,
            Double latitude, Double longitude, Double maxDistance,
            java.util.UUID userId) {

        return Specification
            .where(minAge(minAge))
            .and(maxAge(maxAge))
            .and(hasGender(gender))
            .and(withinDistance(latitude, longitude, maxDistance))
            .and(notSwiped(userId));
    }
}

/**
 * Repository using Specification interface for dynamic queries
 */
@Repository
public interface UserSpecificationRepository extends JpaRepository<User, UUID>,
        JpaSpecificationExecutor<User> {

    // Inherited methods from JpaSpecificationExecutor:
    // Page<User> findAll(Specification<User> spec, Pageable pageable);
}

/**
 * Usage in service
 */
@Service
public class FeedService {

    @Autowired
    private UserSpecificationRepository userRepository;

    public Page<User> getFilteredFeed(FeedFilter filter, UUID userId, Pageable pageable) {
        Specification<User> spec = UserSpecifications.buildFeedCriteria(
            filter.getMinAge(),
            filter.getMaxAge(),
            filter.getPreferredGender(),
            filter.getLatitude(),
            filter.getLongitude(),
            filter.getMaxDistance(),
            userId
        );

        return userRepository.findAll(spec, pageable);
    }
}
```

**Key Points:**
- Use `Specification` for dynamic, composable queries
- Implement `JpaSpecificationExecutor` for Specification support
- Build complex queries by chaining `.and()` and `.or()`
- Leverage type-safe metamodel (@Entity_) for column references
- Useful when filtering criteria is dynamic

---

## Vaadin View Patterns

### Pattern 1: Basic View with Form (Login/Register)

```java
package com.dating.ui.views;

import com.dating.ui.dto.AuthResponse;
import com.dating.ui.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.extern.slf4j.Slf4j;

/**
 * Login view - Entry point for the application
 * Users can enter email and password to authenticate
 */
@Route("login")
@PageTitle("Login | POC Dating")
@AnonymousAllowed
@Slf4j
public class LoginView extends VerticalLayout {

    private final UserService userService;

    private EmailField emailField;
    private PasswordField passwordField;
    private Button loginButton;
    private Button registerButton;

    public LoginView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)");

        createUI();
    }

    /**
     * Build the login form UI
     */
    private void createUI() {
        // Title
        H1 title = new H1("❤️ POC Dating");
        title.getStyle()
            .set("color", "white")
            .set("margin-bottom", "2rem");

        Span subtitle = new Span("Find your perfect match");
        subtitle.getStyle()
            .set("color", "rgba(255,255,255,0.8)")
            .set("font-size", "1.2rem");

        // Form container
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setWidth("400px");
        formLayout.setPadding(true);
        formLayout.getStyle()
            .set("background", "white")
            .set("border-radius", "12px")
            .set("box-shadow", "0 8px 32px rgba(0,0,0,0.1)");

        // Email field
        emailField = new EmailField("Email");
        emailField.setPlaceholder("you@example.com");
        emailField.setRequiredIndicatorVisible(true);
        emailField.setWidthFull();

        // Password field
        passwordField = new PasswordField("Password");
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setWidthFull();

        // Login button
        loginButton = new Button("Login", e -> handleLogin());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();

        // Register button
        registerButton = new Button("Create Account", e -> handleRegister());
        registerButton.setWidthFull();

        formLayout.add(emailField, passwordField, loginButton, registerButton);

        add(title, subtitle, formLayout);
    }

    /**
     * Handle login button click
     * Validates input and calls user service
     */
    private void handleLogin() {
        String email = emailField.getValue();
        String password = passwordField.getValue();

        // Client-side validation
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        if (!emailField.isInvalid() && !email.contains("@")) {
            emailField.setInvalid(true);
            emailField.setErrorMessage("Please enter a valid email");
            return;
        }

        try {
            // Disable button to prevent double-click
            loginButton.setEnabled(false);

            // Call user service to login
            AuthResponse response = userService.login(email, password);

            log.info("Login successful for user: {}", response.getUser().getId());

            // Show success message
            Notification.show("Welcome back, " + response.getUser().getFirstName() + "!",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Navigate to main app
            UI.getCurrent().navigate(SwipeView.class);

        } catch (Exception ex) {
            log.error("Login failed", ex);
            showError("Invalid email or password");
        } finally {
            loginButton.setEnabled(true);
        }
    }

    /**
     * Handle register button click
     * Navigate to registration view
     */
    private void handleRegister() {
        UI.getCurrent().navigate(RegisterView.class);
    }

    /**
     * Show error notification
     */
    private void showError(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
```

**Key Points:**
- Use `@Route` for navigation mapping
- Use `@AnonymousAllowed` for public views
- Extend `VerticalLayout`, `HorizontalLayout`, or custom layout
- Use constructor injection for services
- Create components in `createUI()` method
- Add styles using `getStyle().set()`
- Show notifications for user feedback
- Disable buttons during async operations
- Log important events
- Use sensible spacing and sizing

---

### Pattern 2: Master-Detail View (Grid with Selection)

```java
package com.dating.ui.views;

import com.dating.ui.dto.Conversation;
import com.dating.ui.service.ChatService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.UI;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Messages view - Master-detail layout showing conversations
 * Users can view all conversations and click to open chat
 */
@Route(value = "messages", layout = MainLayout.class)
@PageTitle("Messages | POC Dating")
@PermitAll
@Slf4j
public class MessagesView extends VerticalLayout {

    private final ChatService chatService;
    private Grid<Conversation> conversationGrid;

    public MessagesView(ChatService chatService) {
        this.chatService = chatService;

        setSizeFull();
        setPadding(true);

        createUI();
        loadConversations();
    }

    /**
     * Build the messages view UI
     */
    private void createUI() {
        H2 title = new H2("Messages");

        // Create grid with Conversation objects
        conversationGrid = new Grid<>(Conversation.class, false);
        conversationGrid.addColumn(conv -> conv.getOtherUser().getFirstName())
            .setHeader("Name")
            .setWidth("150px");

        conversationGrid.addColumn(conv ->
            conv.getLastMessage() != null ?
            conv.getLastMessage().getText() :
            "No messages yet"
        )
            .setHeader("Last Message")
            .setFlexGrow(1);

        conversationGrid.addColumn(Conversation::getUnreadCount)
            .setHeader("Unread")
            .setWidth("80px");

        // Add click listener to open chat
        conversationGrid.addItemClickListener(event -> {
            Conversation conv = event.getItem();
            log.debug("Opening conversation: {}", conv.getId());
            // Navigate to ChatView with conversation ID
            UI.getCurrent().navigate("chat/" + conv.getId());
        });

        conversationGrid.setSizeFull();

        add(title, conversationGrid);
    }

    /**
     * Load conversations from service and populate grid
     */
    private void loadConversations() {
        try {
            List<Conversation> conversations = chatService.getConversations();
            conversationGrid.setItems(conversations);

            if (conversations.isEmpty()) {
                add(new Paragraph("No conversations yet. Start matching!"));
            }

        } catch (Exception ex) {
            log.error("Failed to load conversations", ex);
            add(new Paragraph("Failed to load conversations: " + ex.getMessage()));
        }
    }
}
```

**Key Points:**
- Use `Grid<T>` for displaying tabular data
- Use `addColumn()` to define columns
- Add click listeners for master-detail navigation
- Set appropriate widths and flex-grow
- Load data asynchronously (in this case synchronously, but show how to handle it)
- Show empty state when no data
- Handle exceptions gracefully

---

### Pattern 3: View with Real-time Updates (@Push)

```java
package com.dating.ui.views;

import com.dating.ui.dto.Message;
import com.dating.ui.service.ChatService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.push.Push;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Chat view - Real-time messaging with WebSocket (@Push)
 * Messages are displayed in real-time as they arrive
 */
@Route(value = "chat/:conversationId", layout = MainLayout.class)
@PageTitle("Chat | POC Dating")
@Push // Enable Vaadin push for real-time updates
@PermitAll
@Slf4j
public class ChatView extends VerticalLayout {

    private final ChatService chatService;
    private String conversationId;

    private Div messageContainer;
    private TextArea messageInput;
    private Button sendButton;

    public ChatView(ChatService chatService) {
        this.chatService = chatService;

        setSizeFull();
        setPadding(true);

        createUI();
    }

    /**
     * Build chat UI
     */
    private void createUI() {
        H2 title = new H2("Chat");

        // Message display area
        messageContainer = new Div();
        messageContainer.setWidth("100%");
        messageContainer.setHeight("500px");
        messageContainer.getStyle().set("overflow-y", "auto");
        messageContainer.getStyle().set("border", "1px solid #ccc");

        // Message input
        messageInput = new TextArea("Message");
        messageInput.setWidth("100%");
        messageInput.setHeight("80px");

        // Send button
        sendButton = new Button("Send", e -> handleSendMessage());

        add(title, messageContainer, messageInput, sendButton);
    }

    /**
     * Called when user sends a message
     */
    private void handleSendMessage() {
        String text = messageInput.getValue().trim();
        if (text.isEmpty()) return;

        try {
            // Send message to service
            Message message = chatService.sendMessage(conversationId, text);

            // Clear input
            messageInput.clear();

            // Display message immediately (optimistic update)
            displayMessage(message);

            // Start polling or WebSocket listener for updates
            listenForMessages();

        } catch (Exception ex) {
            log.error("Failed to send message", ex);
        }
    }

    /**
     * Display message in message container
     */
    private void displayMessage(Message message) {
        Div messageDiv = new Div();
        messageDiv.getStyle().set("padding", "10px");
        messageDiv.getStyle().set("margin", "5px 0");
        messageDiv.getStyle().set("background", "#f0f0f0");
        messageDiv.setText(message.getSenderName() + ": " + message.getText());

        messageContainer.add(messageDiv);

        // Scroll to bottom
        messageContainer.getElement().executeJs("this.scrollTop = this.scrollHeight");
    }

    /**
     * Listen for incoming messages (WebSocket or polling)
     * Uses ui.access() to safely update UI from background thread
     */
    private void listenForMessages() {
        // Start async listener
        new Thread(() -> {
            try {
                List<Message> messages = chatService.pollMessages(conversationId);

                UI ui = UI.getCurrent();
                if (ui != null) {
                    ui.access(() -> {
                        messages.forEach(this::displayMessage);
                    });
                }

            } catch (Exception ex) {
                log.error("Failed to listen for messages", ex);
            }
        }).start();
    }
}
```

**Key Points:**
- Use `@Push` annotation for real-time updates via WebSocket/SSE
- Use `UI.getCurrent().access()` to safely update UI from background threads
- Display messages optimistically before server confirmation
- Implement scrolling to bottom for chat UX
- Handle disconnections gracefully
- Log all errors for debugging

---

## Entity Patterns

### Pattern 1: Basic JPA Entity with Timestamps

```java
package com.dating.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.UUID;

/**
 * User entity - represents a user profile
 * Includes authentication, profile, and preference data
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * Unique identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User email (unique)
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * Username (unique)
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Bcrypt-hashed password
     */
    @Column(nullable = false, length = 255)
    private String passwordHash;

    /**
     * First name
     */
    @Column(length = 50)
    private String firstName;

    /**
     * Last name
     */
    @Column(length = 50)
    private String lastName;

    /**
     * User bio/about section
     */
    @Column(columnDefinition = "TEXT")
    private String bio;

    /**
     * Date of birth
     */
    @Column
    private java.time.LocalDate dateOfBirth;

    /**
     * Age (derived from DOB, but stored for query performance)
     */
    @Column
    private Integer age;

    /**
     * Gender
     */
    @Column(length = 20)
    private String gender;

    /**
     * Profile photo URL
     */
    @Column
    private String photoUrl;

    /**
     * City for location
     */
    @Column(length = 100)
    private String city;

    /**
     * Country for location
     */
    @Column(length = 100)
    private String country;

    /**
     * GPS latitude
     */
    @Column
    private Double latitude;

    /**
     * GPS longitude
     */
    @Column
    private Double longitude;

    /**
     * User status (ACTIVE, INACTIVE, BANNED, DELETED)
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    /**
     * Minimum preferred age
     */
    @Column
    private Integer minAge;

    /**
     * Maximum preferred age
     */
    @Column
    private Integer maxAge;

    /**
     * Preferred gender
     */
    @Column(length = 20)
    private String interestedInGender;

    /**
     * Maximum distance for matches (in km)
     */
    @Column
    private Integer maxDistance;

    /**
     * Created timestamp
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Last updated timestamp
     */
    @LastModifiedDate
    @Column
    private Instant updatedAt;

    /**
     * Calculate age from date of birth
     */
    @PrePersist
    @PreUpdate
    public void calculateAge() {
        if (dateOfBirth != null) {
            this.age = java.time.LocalDate.now()
                .getYear() - dateOfBirth.getYear();
        }
    }
}
```

**Key Points:**
- Use `@Entity` and `@Table` annotations
- Define indexes on frequently queried columns
- Use `@Column` with appropriate constraints (nullable, unique, length)
- Use `@CreatedDate` and `@LastModifiedDate` for audit columns
- Use `@GeneratedValue(strategy = GenerationType.UUID)` for IDs
- Use `@PrePersist` and `@PreUpdate` for computed columns
- Use Lombok annotations for boilerplate (`@Data`, `@Builder`)
- Write detailed JavaDoc for each field

---

### Pattern 2: Entity with Relationships

```java
package com.dating.match.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.util.UUID;

/**
 * Match entity - represents a mutual like between two users
 * Created when both users swipe "like" on each other
 */
@Entity
@Table(name = "matches", indexes = {
    @Index(name = "idx_user1_user2", columnList = "user1_id, user2_id"),
    @Index(name = "idx_user1_status", columnList = "user1_id, status"),
    @Index(name = "idx_user2_status", columnList = "user2_id, status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    /**
     * Unique identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * First user in the match
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    /**
     * Second user in the match
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    /**
     * Match status (ACTIVE, ENDED)
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    /**
     * When the match was created
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant matchedAt;

    /**
     * When the match was ended (if applicable)
     */
    @Column
    private Instant endedAt;

    /**
     * Composite unique constraint to prevent duplicate matches
     */
    @PrePersist
    public void ensureConsistentOrder() {
        // Ensure user1 ID is always less than user2 ID (for uniqueness)
        if (user1.getId().compareTo(user2.getId()) > 0) {
            User temp = user1;
            user1 = user2;
            user2 = temp;
        }
    }
}

/**
 * Message entity - messages in a match conversation
 */
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_match_created", columnList = "match_id, created_at"),
    @Index(name = "idx_receiver_status", columnList = "receiver_id, status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    /**
     * Unique identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The match this message belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    /**
     * Message sender
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * Message receiver
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    /**
     * Message content
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Message status (SENT, DELIVERED, READ)
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "SENT";

    /**
     * When message was created
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * When message was read (if applicable)
     */
    @Column
    private Instant readAt;
}
```

**Key Points:**
- Use `@ManyToOne` for relationships
- Use `FetchType.LAZY` to avoid N+1 queries (query child on demand)
- Use `@JoinColumn` to specify foreign key column names
- Define composite indexes for common queries
- Use `@PrePersist` for data validation/transformation
- Document relationships in JavaDoc
- Use separate entity classes for separate tables

---

## DTO Patterns

### Pattern 1: Request/Response DTOs

```java
package com.dating.user.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * User registration request DTO
 * Includes validation constraints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must contain uppercase, lowercase, digit, and special character"
    )
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @PastOrPresent(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "MALE|FEMALE|OTHER")
    private String gender;
}

/**
 * User response DTO
 * Contains user data and authentication tokens
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private String id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private Integer age;
    private String gender;
    private String bio;
    private String photoUrl;
    private String city;
    private String country;

    // Authentication data
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;

    // Preferences
    private Integer minAge;
    private Integer maxAge;
    private String interestedInGender;
    private Integer maxDistance;
}

/**
 * User update request DTO
 * All fields optional for partial updates
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Size(max = 5000)
    private String bio;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String country;

    private Double latitude;
    private Double longitude;

    @Size(max = 500)
    private String photoUrl;
}

/**
 * Login request DTO
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
```

**Key Points:**
- Use validation annotations from `jakarta.validation.constraints`
- Include meaningful error messages
- Make fields optional when appropriate (using null)
- Separate request and response DTOs
- Never expose internal details in DTOs
- Use Lombok for boilerplate
- Document validation rules in comments

---

## Exception Handling

### Pattern 1: Custom Exceptions

```java
package com.dating.common.exception;

/**
 * Base exception for dating application
 * All custom exceptions should extend this
 */
public class DatingException extends RuntimeException {

    public DatingException(String message) {
        super(message);
    }

    public DatingException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Exception thrown when user is not found
 */
public class UserNotFoundException extends DatingException {

    public UserNotFoundException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when user already exists
 */
public class UserAlreadyExistsException extends DatingException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when password is invalid
 */
public class InvalidPasswordException extends DatingException {

    public InvalidPasswordException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when authentication fails
 */
public class AuthenticationException extends DatingException {

    public AuthenticationException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when resource not found
 */
public class ResourceNotFoundException extends DatingException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when input validation fails
 */
public class ValidationException extends DatingException {

    public ValidationException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when operation is forbidden
 */
public class ForbiddenException extends DatingException {

    public ForbiddenException(String message) {
        super(message);
    }
}
```

**Key Points:**
- Create a base exception class
- Extend base exception for specific error types
- Use meaningful exception names
- Include relevant error context in message
- Throw early and specific (not generic Exception)

---

### Pattern 2: Global Exception Handler

```java
package com.dating.common.config;

import com.dating.common.dto.ErrorResponse;
import com.dating.common.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers
 * Converts all exceptions to standardized error responses
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle user not found exception
     */
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("NOT_FOUND")
            .message(ex.getMessage())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle user already exists exception
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("User already exists: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.CONFLICT.value())
            .error("CONFLICT")
            .message(ex.getMessage())
            .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle invalid password exception
     */
    @ExceptionHandler(InvalidPasswordException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleInvalidPassword(InvalidPasswordException ex) {
        log.warn("Invalid password attempt");

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("UNAUTHORIZED")
            .message("Invalid credentials")
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle authentication exception
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("UNAUTHORIZED")
            .message(ex.getMessage())
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle forbidden exception
     */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        log.warn("Access forbidden: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("FORBIDDEN")
            .message(ex.getMessage())
            .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle validation exceptions (from @Valid annotation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("VALIDATION_ERROR")
            .message("Validation failed")
            .validationErrors(errors)
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("INTERNAL_ERROR")
            .message("An unexpected error occurred")
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

/**
 * Standard error response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private Map<String, String> validationErrors;
}
```

**Key Points:**
- Use `@ControllerAdvice` for global exception handling
- Map exceptions to appropriate HTTP status codes
- Return standardized error response format
- Log errors appropriately (warn for expected, error for unexpected)
- Include validation errors in response
- Never expose internal details in error messages
- Include timestamp for debugging

---

## Security & Authentication

### Pattern 1: JWT Token Provider

```java
package com.dating.user.security;

import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

/**
 * JWT token provider for token generation and validation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret:your-secret-key-here}")
    private String jwtSecret;

    @Value("${jwt.expiration:900000}") // 15 minutes
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800000}") // 7 days
    private long refreshTokenExpiration;

    /**
     * Generate JWT access token
     *
     * @param userId User ID to embed in token
     * @return JWT token string
     */
    public String generateAccessToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }

    /**
     * Generate refresh token
     *
     * @param userId User ID to embed in token
     * @return Refresh token string
     */
    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("type", "refresh")
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }

    /**
     * Get user ID from token
     *
     * @param token JWT token
     * @return User ID
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();

        return UUID.fromString(claims.getSubject());
    }

    /**
     * Validate JWT token
     *
     * @param token JWT token
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }

    /**
     * Get expiration time from token
     *
     * @param token JWT token
     * @return Expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();

        return claims.getExpiration();
    }
}
```

**Key Points:**
- Use JJWT library for JWT handling
- Store secret in configuration (never hardcode)
- Use appropriate expiration times (15 min for access, 7 days for refresh)
- Validate token signature
- Log all security events
- Return meaningful error messages

---

## Testing Patterns

### Pattern 1: Unit Test with Mockito

```java
package com.dating.user.service;

import com.dating.user.dto.UserRegistrationRequest;
import com.dating.user.dto.UserResponse;
import com.dating.user.entity.User;
import com.dating.user.exception.UserAlreadyExistsException;
import com.dating.user.exception.InvalidPasswordException;
import com.dating.user.mapper.UserMapper;
import com.dating.user.repository.UserRepository;
import com.dating.user.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService
 * Tests business logic in isolation using mocks
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private UserService userService;

    private UserRegistrationRequest registrationRequest;
    private User testUser;
    private UserResponse expectedResponse;

    @BeforeEach
    void setUp() {
        registrationRequest = UserRegistrationRequest.builder()
            .email("test@example.com")
            .username("testuser")
            .password("Password123!")
            .firstName("John")
            .lastName("Doe")
            .build();

        testUser = User.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .username("testuser")
            .passwordHash("$2a$12$hashed_password")
            .firstName("John")
            .lastName("Doe")
            .status("ACTIVE")
            .build();

        expectedResponse = UserResponse.builder()
            .id(testUser.getId().toString())
            .email("test@example.com")
            .username("testuser")
            .firstName("John")
            .lastName("Doe")
            .accessToken("jwt_token")
            .refreshToken("refresh_token")
            .build();
    }

    /**
     * Test successful user registration
     */
    @Test
    void testRegister_Success() {
        // Arrange
        when(userRepository.existsByEmail(registrationRequest.getEmail()))
            .thenReturn(false);
        when(passwordEncoder.encode(registrationRequest.getPassword()))
            .thenReturn("$2a$12$hashed_password");
        when(userRepository.save(any(User.class)))
            .thenReturn(testUser);
        when(tokenProvider.generateAccessToken(testUser.getId()))
            .thenReturn("jwt_token");
        when(tokenProvider.generateRefreshToken(testUser.getId()))
            .thenReturn("refresh_token");
        when(userMapper.toResponse(eq(testUser), anyString(), anyString()))
            .thenReturn(expectedResponse);

        // Act
        UserResponse response = userService.register(registrationRequest);

        // Assert
        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertEquals("testuser", response.getUsername());
        assertEquals("jwt_token", response.getAccessToken());

        // Verify interactions
        verify(userRepository, times(1)).existsByEmail(registrationRequest.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
        verify(tokenProvider, times(1)).generateAccessToken(testUser.getId());
    }

    /**
     * Test registration fails when email already exists
     */
    @Test
    void testRegister_EmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(registrationRequest.getEmail()))
            .thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.register(registrationRequest);
        });

        // Verify repository was checked but not saved
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Test successful login
     */
    @Test
    void testLogin_Success() {
        // Arrange
        String email = "test@example.com";
        String password = "Password123!";

        when(userRepository.findByEmail(email))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPasswordHash()))
            .thenReturn(true);
        when(tokenProvider.generateAccessToken(testUser.getId()))
            .thenReturn("jwt_token");
        when(tokenProvider.generateRefreshToken(testUser.getId()))
            .thenReturn("refresh_token");
        when(userMapper.toResponse(eq(testUser), anyString(), anyString()))
            .thenReturn(expectedResponse);

        // Act
        UserResponse response = userService.login(email, password);

        // Assert
        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
    }

    /**
     * Test login fails with wrong password
     */
    @Test
    void testLogin_InvalidPassword() {
        // Arrange
        String email = "test@example.com";
        String wrongPassword = "WrongPassword123!";

        when(userRepository.findByEmail(email))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(wrongPassword, testUser.getPasswordHash()))
            .thenReturn(false);

        // Act & Assert
        assertThrows(InvalidPasswordException.class, () -> {
            userService.login(email, wrongPassword);
        });
    }
}
```

**Key Points:**
- Use `@ExtendWith(MockitoExtension.class)` for Mockito support
- Use `@Mock` for dependencies, `@InjectMocks` for service under test
- Use `@BeforeEach` to set up test data
- Use descriptive test method names
- Follow Arrange-Act-Assert pattern
- Verify mock interactions
- Test both success and failure cases

---

### Pattern 2: Integration Test with TestContainers

```java
package com.dating.user.service;

import com.dating.user.dto.UserRegistrationRequest;
import com.dating.user.dto.UserResponse;
import com.dating.user.entity.User;
import com.dating.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UserService
 * Uses TestContainers to test with real PostgreSQL database
 */
@SpringBootTest
@Testcontainers
class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("dating_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Test end-to-end user registration and retrieval
     */
    @Test
    void testRegisterAndFindUser() {
        // Arrange
        UserRegistrationRequest request = UserRegistrationRequest.builder()
            .email("integration@test.com")
            .username("integrationtest")
            .password("Password123!")
            .firstName("Integration")
            .lastName("Test")
            .build();

        // Act
        UserResponse registered = userService.register(request);

        // Assert - user was saved to database
        assertNotNull(registered.getId());
        assertEquals("integration@test.com", registered.getEmail());

        Optional<User> found = userRepository.findByEmail("integration@test.com");
        assertTrue(found.isPresent());
        assertEquals(registered.getId(), found.get().getId().toString());
    }

    /**
     * Test login after registration
     */
    @Test
    void testRegisterAndLogin() {
        // Arrange
        UserRegistrationRequest registerRequest = UserRegistrationRequest.builder()
            .email("login@test.com")
            .username("logintest")
            .password("Password123!")
            .firstName("Login")
            .lastName("Test")
            .build();

        userService.register(registerRequest);

        // Act
        UserResponse loginResponse = userService.login("login@test.com", "Password123!");

        // Assert
        assertNotNull(loginResponse);
        assertEquals("login@test.com", loginResponse.getEmail());
    }
}
```

**Key Points:**
- Use `@SpringBootTest` for full context testing
- Use `@Testcontainers` and `@Container` for database
- Use `@DynamicPropertySource` to configure test database
- Test with real database for integration tests
- Use `@Autowired` to inject actual beans
- Test complete workflows, not just isolated logic

---

## Summary

This code patterns reference provides:

1. **REST Controllers** - CRUD operations, search, filtering
2. **Services** - Business logic, transactions, caching
3. **Feign Clients** - HTTP communication between services
4. **Repositories** - JPA queries, custom queries, specifications
5. **Vaadin Views** - Forms, grids, real-time updates
6. **Entities** - JPA mappings, relationships, indexes
7. **DTOs** - Request/response objects with validation
8. **Exception Handling** - Custom exceptions, global handler
9. **Security** - JWT tokens, authentication
10. **Testing** - Unit tests with Mockito, integration tests with TestContainers

All patterns follow Spring Boot and Java best practices and are ready to use in new implementations.

---

**Document Control:**
- Last Updated: 2025-11-15
- Author: AI Assistant
- Status: Active
- Version: 1.0
