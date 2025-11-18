package com.dating.ui.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that adds request tracking context to all incoming requests.
 * Enables distributed tracing by adding requestId and userId to MDC.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Skip actuator endpoints and Vaadin internal requests
        String uri = request.getRequestURI();
        if (uri.startsWith("/actuator") || uri.startsWith("/VAADIN") || uri.startsWith("/PUSH")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get or generate request ID
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put("requestId", requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        // Extract user from session if available
        if (request.getUserPrincipal() != null) {
            MDC.put("userId", request.getUserPrincipal().getName());
        }

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Only log page requests, not assets
            if (!uri.contains(".") || uri.endsWith(".html")) {
                log.info("Request completed: method={}, path={}, status={}, duration={}ms",
                    request.getMethod(),
                    uri,
                    response.getStatus(),
                    duration);
            }

            MDC.clear();
        }
    }
}
