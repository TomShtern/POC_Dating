package com.dating;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Recommendation Service Application Entry Point
 *
 * PURPOSE: Bootstrap the Recommendation Service microservice
 *
 * RESPONSIBILITIES:
 * - Initialize Spring Boot application context
 * - Enable dependency injection
 * - Configure auto-configuration
 * - Enable scheduled recommendation generation tasks
 * - Enable metrics collection for algorithm performance
 *
 * PACKAGES SCANNED:
 * - com.dating.recommendation.* (all service subpackages)
 * - Controllers (recommendation endpoints)
 * - Services (scoring algorithms, ML models)
 * - Repositories (recommendations, interaction history, algorithm performance)
 * - Events (recommendation feedback consumers)
 *
 * SCHEDULED TASKS:
 * - Batch generate recommendations (hourly per user segment)
 * - Cache invalidation (daily)
 * - Algorithm performance analysis (daily)
 * - ML model retraining (weekly)
 *
 * ALGORITHMS:
 * - v1: Rule-based (preferences + demographics)
 * - v2: Collaborative filtering (future)
 * - v3: Deep learning (future)
 */
@SpringBootApplication
@EnableScheduling
public class RecommendationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendationServiceApplication.class, args);
    }
}
