package com.dating.match;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main application class for Match Service
 *
 * PURPOSE: Bootstrap Spring Boot microservice for match and swipe management
 *
 * ANNOTATIONS:
 * - @SpringBootApplication: Auto-configuration and component scanning
 * - @EnableFeignClients: Enable Feign declarative HTTP clients for inter-service communication
 * - @EnableJpaAuditing: Enable automatic audit fields (createdAt, updatedAt)
 *
 * RESPONSIBILITIES:
 * - Record user swipes (like/pass/super-like)
 * - Detect mutual matches between users
 * - Generate personalized match recommendations
 * - Manage active matches (unmatch)
 * - Communicate with user-service via Feign
 *
 * ARCHITECTURE DECISIONS:
 * - Uses Feign client instead of RestTemplate for cleaner inter-service calls
 * - JWT authentication for security (validates tokens from user-service)
 * - PostgreSQL for persistent storage of swipes and matches
 * - Redis for caching recommendation feeds and match data
 * - RabbitMQ for publishing match events
 *
 * PORT: 8082 (configured in application.yml)
 * DATABASE: dating_matches (PostgreSQL)
 */
@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing
public class MatchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchServiceApplication.class, args);
    }
}
