package com.dating.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway routing configuration.
 *
 * Routes requests to appropriate backend microservices based on path patterns.
 */
@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service Routes
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .uri("http://localhost:8081"))

                // Match Service Routes
                .route("match-service", r -> r
                        .path("/api/matches/**")
                        .uri("http://localhost:8082"))

                // Chat Service Routes
                .route("chat-service", r -> r
                        .path("/api/chat/**")
                        .uri("http://localhost:8083"))

                // Recommendation Service Routes
                .route("recommendation-service", r -> r
                        .path("/api/recommendations/**")
                        .uri("http://localhost:8084"))

                .build();
    }
}
