package com.dating;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Match Service Application Entry Point
 *
 * PURPOSE: Bootstrap the Match Service microservice
 *
 * RESPONSIBILITIES:
 * - Initialize Spring Boot application context
 * - Enable dependency injection for match logic
 * - Configure auto-configuration
 * - Enable scheduled feed regeneration tasks
 *
 * PACKAGES SCANNED:
 * - com.dating.match.* (all service subpackages)
 * - Controllers (swipes, feed, matches endpoints)
 * - Services (match detection, feed generation, scoring)
 * - Repositories (JPA database access)
 * - Events (RabbitMQ publishers)
 *
 * SCHEDULED TASKS:
 * - Regenerate recommendation feeds (hourly)
 * - Archive old swipes (daily)
 * - Recalculate match scores (periodic)
 */
@SpringBootApplication
@EnableScheduling
public class MatchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchServiceApplication.class, args);
    }
}
