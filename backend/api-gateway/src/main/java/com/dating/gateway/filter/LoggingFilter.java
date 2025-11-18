package com.dating.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Logging filter for request/response logging.
 *
 * Logs:
 * - Request method and path
 * - Response status code
 * - Request duration
 */
@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final String START_TIME_ATTRIBUTE = "startTime";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        exchange.getAttributes().put(START_TIME_ATTRIBUTE, startTime);

        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getPath().value();
        String userId = request.getHeaders().getFirst(USER_ID_HEADER);

        log.info("Request: {} {} [User: {}]", method, path, userId != null ? userId : "anonymous");

        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    Long start = exchange.getAttribute(START_TIME_ATTRIBUTE);
                    long duration = start != null ? System.currentTimeMillis() - start : 0;

                    ServerHttpResponse response = exchange.getResponse();
                    int statusCode = response.getStatusCode() != null
                            ? response.getStatusCode().value()
                            : 0;

                    log.info("Response: {} {} - Status: {} - Duration: {}ms",
                            method, path, statusCode, duration);

                    // Log warning for slow requests (>500ms)
                    if (duration > 500) {
                        log.warn("Slow request detected: {} {} took {}ms", method, path, duration);
                    }
                }));
    }

    @Override
    public int getOrder() {
        // Run first to capture accurate timing
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
