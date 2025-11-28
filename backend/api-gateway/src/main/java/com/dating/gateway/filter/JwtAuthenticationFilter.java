package com.dating.gateway.filter;

import com.dating.gateway.security.JwtValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Global JWT authentication filter for the API Gateway.
 *
 * Validates JWT tokens and extracts user ID for downstream services.
 * Skips validation for public endpoints (auth, actuator).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtValidator jwtValidator;

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";

    /**
     * Public endpoints that don't require authentication.
     */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/users/auth/",
            "/actuator/"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Skip authentication for public endpoints
        if (isPublicPath(path)) {
            log.debug("Public path, skipping authentication: {}", path);
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        // Extract token
        String token = authHeader.substring(BEARER_PREFIX.length());

        // Validate token
        if (!jwtValidator.validateToken(token)) {
            log.warn("Invalid JWT token for path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
        }

        // Ensure it's an access token, not a refresh token
        if (!jwtValidator.isAccessToken(token)) {
            log.warn("Refresh token used for authentication on path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid token type");
        }

        // Extract user ID and add to request headers for downstream services
        UUID userId = jwtValidator.getUserIdFromToken(token);
        log.debug("Authenticated request for user {} to path: {}", userId, path);

        // Add X-User-Id header for downstream services
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(USER_ID_HEADER, userId.toString())
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    /**
     * Check if the path is a public endpoint that doesn't require authentication.
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Send error response.
     */
    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        String body = String.format("{\"error\":\"%s\",\"message\":\"%s\",\"status\":%d}",
                status.getReasonPhrase(), message, status.value());

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes()))
        );
    }

    @Override
    public int getOrder() {
        // Run early in the filter chain
        return -100;
    }
}
