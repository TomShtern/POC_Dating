package com.dating;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway Application Entry Point
 *
 * PURPOSE: Bootstrap the API Gateway microservice
 *
 * RESPONSIBILITIES:
 * - Initialize Spring Cloud Gateway
 * - Enable request routing to microservices
 * - Configure JWT authentication/authorization
 * - Implement rate limiting
 * - Setup circuit breaker patterns
 * - Configure CORS and security headers
 * - Enable service discovery
 *
 * ROUTING:
 * - /api/users/* → user-service:8081
 * - /api/matches/* → match-service:8082
 * - /api/chat/* → chat-service:8083
 * - /api/recommendations/* → recommendation-service:8084
 *
 * SECURITY:
 * - JWT Bearer token validation on all endpoints
 * - X-User-Id header injection for downstream services
 * - CORS configuration for frontend origin
 * - Rate limiting (100 req/min per user)
 *
 * PACKAGES SCANNED:
 * - com.dating.gateway.* (all gateway subpackages)
 * - Configuration (routes, filters, security)
 * - Filters (authentication, rate limiting, logging)
 * - Utils (JWT validation, error handling)
 *
 * PUBLIC ENTRY POINT:
 * - http://localhost:8080 (single URL for all API calls)
 * - Internal services not directly accessible
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
