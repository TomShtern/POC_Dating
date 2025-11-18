package com.dating.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ============================================================================
 * RECOMMENDATION SERVICE - APPLICATION ENTRY POINT
 * ============================================================================
 *
 * PURPOSE:
 * Main entry point for the Recommendation Service microservice.
 * This service generates personalized match recommendations for users
 * using a pluggable, modular scoring algorithm.
 *
 * PORT: 8084 (configured in application.yml)
 *
 * KEY FEATURES:
 * - Pluggable scoring algorithm (add/remove scorers easily)
 * - Redis caching for performance (24h TTL)
 * - RabbitMQ event consumption for real-time updates
 * - Fully configurable weights in application.yml
 *
 * HOW TO RUN:
 * - Development: mvn spring-boot:run
 * - Production: java -jar recommendation-service.jar
 * - Docker: docker-compose up recommendation-service
 *
 * DEPENDENCIES:
 * - PostgreSQL: User data storage
 * - Redis: Caching recommendations
 * - RabbitMQ: Event-driven updates
 *
 * ============================================================================
 */
@SpringBootApplication
@EnableCaching      // Enables @Cacheable annotations for Redis caching
@EnableScheduling   // Enables @Scheduled for periodic recommendation refresh
public class RecommendationApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendationApplication.class, args);
    }
}
