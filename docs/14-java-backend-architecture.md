# Java Backend Architecture (Spring Boot)

## Table of Contents
- [Project Structure](#project-structure)
- [Spring Boot Configuration](#spring-boot-configuration)
- [Domain Layer](#domain-layer)
- [Repository Layer](#repository-layer)
- [Service Layer](#service-layer)
- [Controller Layer](#controller-layer)
- [Security & Authentication](#security--authentication)
- [Caching with Redis](#caching-with-redis)
- [Exception Handling](#exception-handling)
- [Testing](#testing)
- [Performance Optimization](#performance-optimization)

---

## Project Structure

### Multi-Module Maven/Gradle Project

```
dating-app/
├── dating-app-common/              # Shared utilities, DTOs, constants
│   ├── src/main/java/
│   │   └── com/datingapp/common/
│   │       ├── dto/                # Data Transfer Objects
│   │       ├── exception/          # Custom exceptions
│   │       ├── util/               # Utility classes
│   │       └── constant/           # Constants
│   └── pom.xml
│
├── dating-app-domain/              # Domain models and business logic
│   ├── src/main/java/
│   │   └── com/datingapp/domain/
│   │       ├── model/              # Domain entities
│   │       ├── repository/         # Repository interfaces
│   │       └── service/            # Domain services
│   └── pom.xml
│
├── dating-app-auth-service/        # Authentication microservice
│   ├── src/main/java/
│   │   └── com/datingapp/auth/
│   │       ├── config/             # Security configuration
│   │       ├── controller/         # REST controllers
│   │       ├── service/            # Business logic
│   │       ├── repository/         # Data access
│   │       └── AuthServiceApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── application-prod.yml
│   └── pom.xml
│
├── dating-app-user-service/        # User management microservice
│   └── (similar structure)
│
├── dating-app-match-service/       # Matching algorithm microservice
│   └── (similar structure)
│
├── dating-app-api-gateway/         # Spring Cloud Gateway
│   └── (similar structure)
│
└── pom.xml                         # Parent POM
```

### Parent POM Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>com.datingapp</groupId>
    <artifactId>dating-app-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>dating-app-common</module>
        <module>dating-app-domain</module>
        <module>dating-app-auth-service</module>
        <module>dating-app-user-service</module>
        <module>dating-app-match-service</module>
        <module>dating-app-api-gateway</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
        <jwt.version>0.12.3</jwt.version>
        <lombok.version>1.18.30</lombok.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Cloud -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- JWT -->
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
                <version>${jwt.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-impl</artifactId>
                <version>${jwt.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-jackson</artifactId>
                <version>${jwt.version}</version>
            </dependency>

            <!-- MapStruct -->
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### Service-Specific Dependencies

```xml
<!-- dating-app-auth-service/pom.xml -->
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
    </dependency>

    <!-- Password Hashing (Argon2) -->
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcprov-jdk18on</artifactId>
        <version>1.77</version>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- MapStruct -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## Spring Boot Configuration

### Application Configuration (application.yml)

```yaml
# dating-app-auth-service/src/main/resources/application.yml
spring:
  application:
    name: auth-service

  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:dating_app}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  jpa:
    hibernate:
      ddl-auto: validate  # Use Flyway/Liquibase for migrations
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
    show-sql: false

  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 2

  jackson:
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: false

# Server Configuration
server:
  port: 8081
  servlet:
    context-path: /api
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain

# JWT Configuration
jwt:
  secret: ${JWT_SECRET:your-secret-key-change-in-production}
  access-token-expiration: 900000      # 15 minutes
  refresh-token-expiration: 604800000  # 7 days

# Logging
logging:
  level:
    root: INFO
    com.datingapp: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/auth-service.log
    max-size: 10MB
    max-history: 30

# Actuator for monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

# Custom Application Properties
app:
  name: Dating App Auth Service
  version: 1.0.0
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:3000}
```

---

## Domain Layer

### Domain Entity (JPA)

```java
// dating-app-domain/src/main/java/com/datingapp/domain/model/User.java
package com.datingapp.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_last_active", columnList = "last_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Column(length = 500)
    private String bio;

    @Column(length = 20)
    private String phone;

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "phone_verified")
    @Builder.Default
    private Boolean phoneVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    // Geolocation (PostGIS will be stored as Point type)
    @Column(columnDefinition = "geography(Point, 4326)")
    private String location; // WKT format: "POINT(longitude latitude)"

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "last_active")
    private LocalDateTime lastActive;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Photo> photos = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserPreferences preferences;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Business methods
    public int getAge() {
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE && deletedAt == null;
    }

    public void updateLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.location = String.format("POINT(%f %f)", longitude, latitude);
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.status = UserStatus.DELETED;
    }

    // Enums
    public enum Gender {
        MALE, FEMALE, NON_BINARY, OTHER
    }

    public enum UserRole {
        USER, MODERATOR, ADMIN
    }

    public enum UserStatus {
        ACTIVE, SUSPENDED, BANNED, DELETED
    }
}
```

### Value Object Example

```java
// dating-app-domain/src/main/java/com/datingapp/domain/model/Photo.java
package com.datingapp.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "photos", indexes = {
    @Index(name = "idx_photos_user_order", columnList = "user_id, display_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String url;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PhotoStatus status = PhotoStatus.PENDING;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    public enum PhotoStatus {
        PENDING, APPROVED, REJECTED
    }
}
```

### User Preferences Entity

```java
// dating-app-domain/src/main/java/com/datingapp/domain/model/UserPreferences.java
package com.datingapp.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "show_me")
    @Enumerated(EnumType.STRING)
    private User.Gender showMe;

    @Column(name = "min_age")
    @Builder.Default
    private Integer minAge = 18;

    @Column(name = "max_age")
    @Builder.Default
    private Integer maxAge = 99;

    @Column(name = "max_distance_km")
    @Builder.Default
    private Integer maxDistanceKm = 50;

    @Column(name = "show_distance")
    @Builder.Default
    private Boolean showDistance = true;

    @Column(name = "only_show_with_photos")
    @Builder.Default
    private Boolean onlyShowWithPhotos = true;
}
```

---

## Repository Layer

### JPA Repository Interface

```java
// dating-app-domain/src/main/java/com/datingapp/domain/repository/UserRepository.java
package com.datingapp.domain.repository;

import com.datingapp.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND u.deletedAt IS NULL " +
           "AND u.lastActive > :sinceTime")
    List<User> findActiveUsersSince(@Param("sinceTime") LocalDateTime sinceTime);

    // PostGIS geospatial query
    @Query(value = "SELECT u.* FROM users u " +
                   "WHERE ST_DWithin(" +
                   "  u.location::geography, " +
                   "  ST_MakePoint(:longitude, :latitude)::geography, " +
                   "  :radiusMeters" +
                   ") " +
                   "AND u.id != :excludeUserId " +
                   "AND u.deleted_at IS NULL " +
                   "AND u.status = 'ACTIVE' " +
                   "ORDER BY ST_Distance(u.location::geography, ST_MakePoint(:longitude, :latitude)::geography) " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<User> findNearbyUsers(
        @Param("latitude") double latitude,
        @Param("longitude") double longitude,
        @Param("radiusMeters") int radiusMeters,
        @Param("excludeUserId") UUID excludeUserId,
        @Param("limit") int limit
    );

    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.photos p " +
           "WHERE u.id = :userId AND u.deletedAt IS NULL")
    Optional<User> findByIdWithPhotos(@Param("userId") UUID userId);

    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.preferences " +
           "WHERE u.id = :userId AND u.deletedAt IS NULL")
    Optional<User> findByIdWithPreferences(@Param("userId") UUID userId);
}
```

### Custom Repository Implementation

```java
// dating-app-domain/src/main/java/com/datingapp/domain/repository/CustomUserRepository.java
package com.datingapp.domain.repository;

import com.datingapp.domain.model.User;
import java.util.List;
import java.util.UUID;

public interface CustomUserRepository {
    List<User> findMatchCandidates(UUID userId, int limit);
}

// dating-app-domain/src/main/java/com/datingapp/domain/repository/impl/CustomUserRepositoryImpl.java
package com.datingapp.domain.repository.impl;

import com.datingapp.domain.model.User;
import com.datingapp.domain.model.UserPreferences;
import com.datingapp.domain.repository.CustomUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@Slf4j
public class CustomUserRepositoryImpl implements CustomUserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<User> findMatchCandidates(UUID userId, int limit) {
        // Complex query with multiple joins and filters
        String sql = """
            SELECT DISTINCT u.*
            FROM users u
            INNER JOIN user_preferences up ON u.id = up.user_id
            LEFT JOIN swipes s ON s.user_id = :userId AND s.target_user_id = u.id
            WHERE u.id != :userId
              AND u.deleted_at IS NULL
              AND u.status = 'ACTIVE'
              AND s.id IS NULL  -- Exclude already swiped users
              AND EXISTS (
                SELECT 1 FROM user_preferences my_prefs
                WHERE my_prefs.user_id = :userId
                  AND u.gender = my_prefs.show_me
                  AND EXTRACT(YEAR FROM AGE(u.date_of_birth)) BETWEEN my_prefs.min_age AND my_prefs.max_age
              )
            ORDER BY u.last_active DESC
            LIMIT :limit
            """;

        Query query = entityManager.createNativeQuery(sql, User.class);
        query.setParameter("userId", userId);
        query.setParameter("limit", limit);

        return query.getResultList();
    }
}
```

---

## Service Layer

### Service Interface

```java
// dating-app-auth-service/src/main/java/com/datingapp/auth/service/UserService.java
package com.datingapp.auth.service;

import com.datingapp.auth.dto.RegisterRequest;
import com.datingapp.auth.dto.UserDto;
import com.datingapp.auth.dto.UpdateProfileRequest;

import java.util.UUID;

public interface UserService {
    UserDto registerUser(RegisterRequest request);
    UserDto getUserById(UUID userId);
    UserDto updateProfile(UUID userId, UpdateProfileRequest request);
    void updateLocation(UUID userId, double latitude, double longitude);
    void deleteUser(UUID userId);
}
```

### Service Implementation

```java
// dating-app-auth-service/src/main/java/com/datingapp/auth/service/impl/UserServiceImpl.java
package com.datingapp.auth.service.impl;

import com.datingapp.auth.dto.RegisterRequest;
import com.datingapp.auth.dto.UpdateProfileRequest;
import com.datingapp.auth.dto.UserDto;
import com.datingapp.auth.exception.ResourceNotFoundException;
import com.datingapp.auth.exception.UserAlreadyExistsException;
import com.datingapp.auth.mapper.UserMapper;
import com.datingapp.auth.service.PasswordService;
import com.datingapp.auth.service.UserService;
import com.datingapp.domain.model.User;
import com.datingapp.domain.model.UserPreferences;
import com.datingapp.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final UserMapper userMapper;

    @Override
    public UserDto registerUser(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }

        // Hash password
        String passwordHash = passwordService.hashPassword(request.getPassword());

        // Create user
        User user = User.builder()
            .email(request.getEmail())
            .passwordHash(passwordHash)
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .dateOfBirth(request.getDateOfBirth())
            .gender(request.getGender())
            .role(User.UserRole.USER)
            .status(User.UserStatus.ACTIVE)
            .emailVerified(false)
            .lastActive(LocalDateTime.now())
            .build();

        // Create default preferences
        UserPreferences preferences = UserPreferences.builder()
            .user(user)
            .showMe(request.getInterestedIn())
            .minAge(18)
            .maxAge(99)
            .maxDistanceKm(50)
            .build();

        user.setPreferences(preferences);

        // Save user
        User savedUser = userRepository.save(user);

        log.info("User registered successfully with ID: {}", savedUser.getId());

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#userId")
    public UserDto getUserById(UUID userId) {
        log.debug("Fetching user with ID: {}", userId);

        User user = userRepository.findByIdWithPreferences(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return userMapper.toDto(user);
    }

    @Override
    @CacheEvict(value = "users", key = "#userId")
    public UserDto updateProfile(UUID userId, UpdateProfileRequest request) {
        log.info("Updating profile for user ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Update fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        User updatedUser = userRepository.save(user);

        log.info("Profile updated successfully for user ID: {}", userId);

        return userMapper.toDto(updatedUser);
    }

    @Override
    @CacheEvict(value = "users", key = "#userId")
    public void updateLocation(UUID userId, double latitude, double longitude) {
        log.debug("Updating location for user ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.updateLocation(latitude, longitude);
        user.setLastActive(LocalDateTime.now());

        userRepository.save(user);
    }

    @Override
    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(UUID userId) {
        log.info("Deleting user with ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.softDelete();
        userRepository.save(user);

        log.info("User deleted successfully with ID: {}", userId);
    }
}
```

### Password Service (Argon2)

```java
// dating-app-auth-service/src/main/java/com/datingapp/auth/service/PasswordService.java
package com.datingapp.auth.service;

public interface PasswordService {
    String hashPassword(String rawPassword);
    boolean verifyPassword(String rawPassword, String hashedPassword);
    boolean needsRehash(String hashedPassword);
}

// dating-app-auth-service/src/main/java/com/datingapp/auth/service/impl/PasswordServiceImpl.java
package com.datingapp.auth.service.impl;

import com.datingapp.auth.service.PasswordService;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@Slf4j
public class PasswordServiceImpl implements PasswordService {

    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;
    private static final int ITERATIONS = 3;
    private static final int MEMORY_KB = 65536; // 64 MB
    private static final int PARALLELISM = 4;

    @Override
    public String hashPassword(String rawPassword) {
        // Generate random salt
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        // Configure Argon2
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(ITERATIONS)
            .withMemoryAsKB(MEMORY_KB)
            .withParallelism(PARALLELISM)
            .withSalt(salt);

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(builder.build());

        // Generate hash
        byte[] hash = new byte[HASH_LENGTH];
        generator.generateBytes(rawPassword.toCharArray(), hash);

        // Encode as: $argon2id$v=19$m=65536,t=3,p=4$<salt>$<hash>
        String encodedSalt = Base64.getEncoder().withoutPadding().encodeToString(salt);
        String encodedHash = Base64.getEncoder().withoutPadding().encodeToString(hash);

        return String.format("$argon2id$v=19$m=%d,t=%d,p=%d$%s$%s",
            MEMORY_KB, ITERATIONS, PARALLELISM, encodedSalt, encodedHash);
    }

    @Override
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        try {
            // Parse encoded hash
            String[] parts = hashedPassword.split("\\$");
            if (parts.length != 6 || !parts[1].equals("argon2id")) {
                return false;
            }

            // Parse parameters
            String[] paramParts = parts[3].split(",");
            int memory = Integer.parseInt(paramParts[0].split("=")[1]);
            int iterations = Integer.parseInt(paramParts[1].split("=")[1]);
            int parallelism = Integer.parseInt(paramParts[2].split("=")[1]);

            // Decode salt and hash
            byte[] salt = Base64.getDecoder().decode(parts[4]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[5]);

            // Configure Argon2
            Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(iterations)
                .withMemoryAsKB(memory)
                .withParallelism(parallelism)
                .withSalt(salt);

            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(builder.build());

            // Generate hash with provided password
            byte[] hash = new byte[expectedHash.length];
            generator.generateBytes(rawPassword.toCharArray(), hash);

            // Constant-time comparison
            return MessageDigest.isEqual(hash, expectedHash);

        } catch (Exception e) {
            log.error("Error verifying password", e);
            return false;
        }
    }

    @Override
    public boolean needsRehash(String hashedPassword) {
        try {
            String[] parts = hashedPassword.split("\\$");
            String[] paramParts = parts[3].split(",");
            int memory = Integer.parseInt(paramParts[0].split("=")[1]);
            int iterations = Integer.parseInt(paramParts[1].split("=")[1]);
            int parallelism = Integer.parseInt(paramParts[2].split("=")[1]);

            return memory != MEMORY_KB || iterations != ITERATIONS || parallelism != PARALLELISM;
        } catch (Exception e) {
            return true;
        }
    }
}
```

---

## Controller Layer

### REST Controller

```java
// dating-app-auth-service/src/main/java/com/datingapp/auth/controller/AuthController.java
package com.datingapp.auth.controller;

import com.datingapp.auth.dto.*;
import com.datingapp.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Token refresh request received");
        TokenResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        log.info("Logout request received");
        String jwt = token.substring(7); // Remove "Bearer " prefix
        authService.logout(jwt);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@RequestHeader("Authorization") String token) {
        log.info("Logout all devices request received");
        String jwt = token.substring(7);
        authService.logoutAllDevices(jwt);
        return ResponseEntity.noContent().build();
    }
}
```

### User Controller

```java
// dating-app-user-service/src/main/java/com/datingapp/user/controller/UserController.java
package com.datingapp.user.controller;

import com.datingapp.auth.dto.UpdateProfileRequest;
import com.datingapp.auth.dto.UserDto;
import com.datingapp.auth.service.UserService;
import com.datingapp.common.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getCurrentUser(@CurrentUser UUID userId) {
        log.debug("Get current user request for ID: {}", userId);
        UserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID userId) {
        log.debug("Get user request for ID: {}", userId);
        UserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> updateProfile(
        @CurrentUser UUID userId,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        log.info("Update profile request for user ID: {}", userId);
        UserDto updatedUser = userService.updateProfile(userId, request);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/me/location")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateLocation(
        @CurrentUser UUID userId,
        @Valid @RequestBody LocationUpdateRequest request
    ) {
        log.debug("Update location request for user ID: {}", userId);
        userService.updateLocation(userId, request.getLatitude(), request.getLongitude());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAccount(@CurrentUser UUID userId) {
        log.info("Delete account request for user ID: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
```

---

## Security & Authentication

### JWT Service

```java
// dating-app-auth-service/src/main/java/com/datingapp/auth/service/JwtService.java
package com.datingapp.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(UUID userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("email", email);
        return createToken(claims, userId.toString(), accessTokenExpiration);
    }

    public String generateRefreshToken(UUID userId, String tokenId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("tokenId", tokenId);
        return createToken(claims, userId.toString(), refreshTokenExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public UUID extractUserId(String token) {
        String userIdStr = extractClaim(token, Claims::getSubject);
        return UUID.fromString(userIdStr);
    }

    public String extractTokenId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("tokenId", String.class);
    }

    public String extractTokenType(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("type", String.class);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean validateToken(String token, UUID userId) {
        final UUID tokenUserId = extractUserId(token);
        return (tokenUserId.equals(userId) && !isTokenExpired(token));
    }
}
```

### Security Configuration

```java
// dating-app-auth-service/src/main/java/com/datingapp/auth/config/SecurityConfig.java
package com.datingapp.auth.config;

import com.datingapp.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/register", "/auth/login", "/auth/refresh").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "https://yourdatingapp.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

### JWT Authentication Filter

```java
// dating-app-auth-service/src/main/java/com/datingapp/auth/security/JwtAuthenticationFilter.java
package com.datingapp.auth.security;

import com.datingapp.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final UUID userId = jwtService.extractUserId(jwt);
            final String tokenType = jwtService.extractTokenType(jwt);

            if (!tokenType.equals("access")) {
                log.warn("Invalid token type: {}", tokenType);
                filterChain.doFilter(request, response);
                return;
            }

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId.toString());

                if (jwtService.validateToken(jwt, userId)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication", e);
        }

        filterChain.doFilter(request, response);
    }
}
```

---

## Caching with Redis

### Redis Configuration

```java
// dating-app-common/src/main/java/com/datingapp/common/config/RedisConfig.java
package com.datingapp.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
            )
            .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("users", config.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration("matches", config.entryTtl(Duration.ofMinutes(5)))
            .build();
    }
}
```

### Cache Service

```java
// dating-app-common/src/main/java/com/datingapp/common/service/CacheService.java
package com.datingapp.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
```

---

## Exception Handling

### Custom Exceptions

```java
// dating-app-common/src/main/java/com/datingapp/common/exception/ResourceNotFoundException.java
package com.datingapp.common.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// dating-app-common/src/main/java/com/datingapp/common/exception/UserAlreadyExistsException.java
package com.datingapp.common.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}

// dating-app-common/src/main/java/com/datingapp/common/exception/UnauthorizedException.java
package com.datingapp.common.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
```

### Global Exception Handler

```java
// dating-app-common/src/main/java/com/datingapp/common/exception/GlobalExceptionHandler.java
package com.datingapp.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
        ResourceNotFoundException ex,
        WebRequest request
    ) {
        log.error("Resource not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(
        UserAlreadyExistsException ex,
        WebRequest request
    ) {
        log.error("User already exists: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.CONFLICT.value())
            .error("Conflict")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
        UnauthorizedException ex,
        WebRequest request
    ) {
        log.error("Unauthorized: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("Unauthorized")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex
    ) {
        log.error("Validation error: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ValidationErrorResponse errorResponse = ValidationErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Invalid input")
            .errors(errors)
            .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Internal server error", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

// Error Response DTOs
@Data
@Builder
class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}

@Data
@Builder
class ValidationErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private Map<String, String> errors;
}
```

---

## Testing

### Unit Tests (JUnit 5 + Mockito)

```java
// dating-app-auth-service/src/test/java/com/datingapp/auth/service/UserServiceImplTest.java
package com.datingapp.auth.service;

import com.datingapp.auth.dto.RegisterRequest;
import com.datingapp.auth.dto.UserDto;
import com.datingapp.auth.exception.UserAlreadyExistsException;
import com.datingapp.auth.mapper.UserMapper;
import com.datingapp.auth.service.impl.UserServiceImpl;
import com.datingapp.domain.model.User;
import com.datingapp.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordService passwordService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequest registerRequest;
    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
            .email("test@example.com")
            .password("Test123!@#")
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .gender(User.Gender.MALE)
            .interestedIn(User.Gender.FEMALE)
            .build();

        user = User.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .firstName("John")
            .lastName("Doe")
            .build();

        userDto = UserDto.builder()
            .id(user.getId())
            .email("test@example.com")
            .firstName("John")
            .build();
    }

    @Test
    @DisplayName("Should register user successfully")
    void registerUser_Success() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordService.hashPassword(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(userDto);

        // When
        UserDto result = userService.registerUser(registerRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(userRepository).save(any(User.class));
        verify(passwordService).hashPassword("Test123!@#");
    }

    @Test
    @DisplayName("Should throw exception when user already exists")
    void registerUser_UserExists_ThrowsException() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(registerRequest))
            .isInstanceOf(UserAlreadyExistsException.class)
            .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }
}
```

### Integration Tests

```java
// dating-app-auth-service/src/test/java/com/datingapp/auth/controller/AuthControllerIntegrationTest.java
package com.datingapp.auth.controller;

import com.datingapp.auth.dto.RegisterRequest;
import com.datingapp.domain.model.User;
import com.datingapp.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        registerRequest = RegisterRequest.builder()
            .email("integration-test@example.com")
            .password("Test123!@#")
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .gender(User.Gender.MALE)
            .interestedIn(User.Gender.FEMALE)
            .build();
    }

    @Test
    @DisplayName("Should register user successfully via API")
    void register_Success() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.email", is("integration-test@example.com")))
            .andExpect(jsonPath("$.user.firstName", is("John")))
            .andExpect(jsonPath("$.accessToken", notNullValue()))
            .andExpect(jsonPath("$.refreshToken", notNullValue()));
    }

    @Test
    @DisplayName("Should return 409 when email already exists")
    void register_EmailExists_Returns409() throws Exception {
        // First registration
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        // Duplicate registration
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("Should return 400 for invalid input")
    void register_InvalidInput_Returns400() throws Exception {
        registerRequest.setEmail("invalid-email");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.email", notNullValue()));
    }
}
```

---

## Performance Optimization

### Database Connection Pooling (HikariCP)

```yaml
# Already configured in application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20      # Max connections
      minimum-idle: 5            # Min idle connections
      connection-timeout: 30000  # 30 seconds
      idle-timeout: 600000       # 10 minutes
      max-lifetime: 1800000      # 30 minutes
```

### Query Optimization Tips

```java
// 1. Use @EntityGraph to avoid N+1 queries
@EntityGraph(attributePaths = {"photos", "preferences"})
Optional<User> findWithPhotosAndPreferencesById(UUID id);

// 2. Use projections for read-only queries
public interface UserSummary {
    UUID getId();
    String getFirstName();
    String getEmail();
}

List<UserSummary> findAllProjectedBy();

// 3. Batch operations
@Modifying
@Query("UPDATE User u SET u.lastActive = :time WHERE u.id IN :ids")
void batchUpdateLastActive(@Param("ids") List<UUID> ids, @Param("time") LocalDateTime time);
```

### Async Processing

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}

// Usage
@Service
public class NotificationService {

    @Async
    public CompletableFuture<Void> sendPushNotification(UUID userId, String message) {
        // Send notification asynchronously
        return CompletableFuture.completedFuture(null);
    }
}
```

---

## Summary

This Java/Spring Boot architecture provides:

✅ **Clean structure** with multi-module Maven project
✅ **Domain-driven design** with JPA entities
✅ **Spring Security** with JWT authentication
✅ **Redis caching** for performance
✅ **Comprehensive exception handling**
✅ **Production-ready testing** (unit + integration)
✅ **Connection pooling** with HikariCP
✅ **Async processing** capabilities
✅ **Geospatial queries** with PostGIS

**Next:** See `15-hybrid-architecture-java-nodejs.md` for integrating Node.js real-time services.
