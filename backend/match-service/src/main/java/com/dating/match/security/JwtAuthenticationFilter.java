package com.dating.match.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT Authentication Filter
 *
 * PURPOSE: Intercept HTTP requests and validate JWT tokens
 *
 * RESPONSIBILITIES:
 * - Extract JWT token from Authorization header
 * - Validate token signature and expiration
 * - Extract user ID from token
 * - Set authentication in Spring Security context
 *
 * FLOW:
 * 1. Extract "Authorization: Bearer <token>" header
 * 2. Validate token using JwtTokenProvider
 * 3. If valid, extract user ID
 * 4. Create Authentication object
 * 5. Store in SecurityContextHolder
 * 6. Controller can access user ID via @AuthenticationPrincipal
 *
 * WHY OncePerRequestFilter:
 * - Guarantees single execution per request
 * - Handles async requests correctly
 * - Standard for security filters
 *
 * SECURITY:
 * - Validates every request (except public endpoints)
 * - Rejects invalid/expired tokens (401 Unauthorized)
 * - No session creation (stateless)
 *
 * PUBLIC ENDPOINTS:
 * - /actuator/health: Health checks
 * - /actuator/metrics: Monitoring
 * - Other actuator endpoints
 *
 * ALTERNATIVES:
 * - GenericFilterBean: Less convenient
 * - Custom authentication provider: More complex
 * - OAuth2 resource server: Overkill for internal services
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);

                // Create authentication object with user ID as principal
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.emptyList()
                    );

                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Set authentication for user ID: {}", userId);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     *
     * @param request HTTP request
     * @return JWT token string or null
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return jwtTokenProvider.extractTokenFromHeader(bearerToken);
    }
}
