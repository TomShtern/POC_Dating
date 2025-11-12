package com.dating.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * User Service Application
 *
 * Main entry point for the User Service microservice.
 * Handles user authentication, profile management, and preferences.
 *
 * Features:
 * - JWT-based authentication
 * - User profile CRUD operations
 * - Password hashing with BCrypt
 * - Redis caching for performance
 * - Event publishing via RabbitMQ
 *
 * Port: 8081 (accessed through API Gateway on 8080)
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
