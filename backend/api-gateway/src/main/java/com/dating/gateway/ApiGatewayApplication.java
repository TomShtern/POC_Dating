package com.dating.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway Application - Single entry point for all microservices.
 *
 * Responsibilities:
 * - Route requests to appropriate microservices
 * - JWT authentication and validation
 * - Rate limiting
 * - CORS handling
 * - Request/Response logging
 *
 * Port: 8080
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
