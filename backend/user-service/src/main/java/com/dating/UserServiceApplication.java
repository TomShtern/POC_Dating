package com.dating;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * User Service Application Entry Point
 *
 * PURPOSE: Bootstrap the User Service microservice
 *
 * RESPONSIBILITIES:
 * - Initialize Spring Boot application context
 * - Enable dependency injection
 * - Configure auto-configuration
 * - Enable service discovery (Eureka client - optional for POC)
 * - Enable scheduled tasks
 *
 * WHY THIS CLASS:
 * - Spring Boot requires a @SpringBootApplication main class
 * - Without this, service cannot start
 * - Serves as configuration entry point for Spring component scanning
 *
 * PACKAGES SCANNED:
 * - com.dating.* (all subpackages)
 * - Controllers, Services, Repositories, Configurations, etc.
 *
 * PROFILES:
 * - dev: Development environment (local debugging)
 * - test: Integration test environment (TestContainers)
 * - prod: Production environment (cloud deployment)
 *
 * USAGE:
 * java -jar user-service.jar --spring.profiles.active=dev
 */
@SpringBootApplication
@EnableScheduling
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
