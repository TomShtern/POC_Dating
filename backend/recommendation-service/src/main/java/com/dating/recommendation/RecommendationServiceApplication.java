package com.dating.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main application class for the Recommendation Service.
 *
 * This service is responsible for generating personalized recommendations
 * for users based on their preferences, behavior, and demographics.
 *
 * Key Features:
 * - User preference management
 * - Recommendation generation based on multiple factors:
 *   * Age range filtering
 *   * Gender preferences
 *   * Location proximity
 *   * Common interests
 * - Exclusion of already swiped/matched users
 * - Simple but extensible scoring algorithm
 *
 * The service uses Feign clients to communicate with:
 * - User Service: To fetch user profiles and demographics
 * - Match Service: To get swipe history and exclude already swiped users
 *
 * @author POC Dating Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.dating.recommendation.client")
@EnableJpaAuditing
public class RecommendationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendationServiceApplication.class, args);
    }
}
