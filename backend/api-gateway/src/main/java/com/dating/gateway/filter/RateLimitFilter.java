package com.dating.gateway.filter;

import com.dating.gateway.config.RateLimitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Rate limiting filter using Redis for distributed rate limiting.
 *
 * Limits:
 * - Authenticated users: 100 requests/minute
 * - Anonymous users: 30 requests/minute
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RateLimitConfig rateLimitConfig;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userId = exchange.getRequest().getHeaders().getFirst(USER_ID_HEADER);
        String clientIp = getClientIp(exchange);

        // Determine the key and limit based on authentication status
        String key;
        int limit;

        if (userId != null && !userId.isEmpty()) {
            // Authenticated user - limit by user ID
            key = RATE_LIMIT_PREFIX + "user:" + userId;
            limit = rateLimitConfig.getAuthenticatedLimit();
        } else {
            // Anonymous user - limit by IP address
            key = RATE_LIMIT_PREFIX + "ip:" + clientIp;
            limit = rateLimitConfig.getAnonymousLimit();
        }

        return checkRateLimit(key, limit)
                .flatMap(allowed -> {
                    if (allowed) {
                        return chain.filter(exchange);
                    } else {
                        log.warn("Rate limit exceeded for key: {}", key);
                        return onRateLimitExceeded(exchange);
                    }
                });
    }

    /**
     * Check if the request is within rate limit using Redis.
     */
    private Mono<Boolean> checkRateLimit(String key, int limit) {
        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        // First request - set expiration
                        return redisTemplate.expire(key, Duration.ofSeconds(rateLimitConfig.getWindowSeconds()))
                                .thenReturn(true);
                    }
                    return Mono.just(count <= limit);
                })
                .onErrorResume(e -> {
                    // If Redis is unavailable, allow the request
                    log.error("Redis error in rate limiting: {}", e.getMessage());
                    return Mono.just(true);
                });
    }

    /**
     * Get client IP address from request.
     */
    private String getClientIp(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }

        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    /**
     * Send rate limit exceeded response.
     */
    private Mono<Void> onRateLimitExceeded(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(rateLimitConfig.getWindowSeconds()));

        String body = String.format(
                "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\",\"status\":429,\"retryAfter\":%d}",
                rateLimitConfig.getWindowSeconds()
        );

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes()))
        );
    }

    @Override
    public int getOrder() {
        // Run after JWT authentication filter
        return -90;
    }
}
