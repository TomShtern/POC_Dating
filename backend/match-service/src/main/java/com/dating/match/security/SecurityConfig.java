package com.dating.match.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security Configuration for Match Service
 *
 * PURPOSE: Configure JWT-based authentication for REST APIs
 *
 * SECURITY MODEL:
 * - Stateless (no sessions)
 * - JWT token authentication
 * - All endpoints require authentication (except actuator)
 * - CSRF disabled (not needed for stateless APIs)
 * - CORS configured separately
 *
 * CONFIGURATION:
 * - @EnableWebSecurity: Enable Spring Security
 * - SecurityFilterChain: Define security rules
 * - JwtAuthenticationFilter: Validate JWT tokens
 *
 * PUBLIC ENDPOINTS:
 * - /actuator/**: Health checks, metrics (for monitoring)
 * - Other endpoints require valid JWT token
 *
 * PROTECTED ENDPOINTS:
 * - /api/matches/**: All match-related operations
 *
 * AUTHENTICATION FLOW:
 * 1. Client sends request with Authorization: Bearer <token>
 * 2. JwtAuthenticationFilter validates token
 * 3. If valid, user ID stored in SecurityContext
 * 4. Controller accesses user ID via @AuthenticationPrincipal
 * 5. If invalid, returns 401 Unauthorized
 *
 * WHY STATELESS:
 * - Scalability: No server-side session storage
 * - Microservices: Works across service boundaries
 * - Load balancing: Any instance can handle any request
 * - Mobile-friendly: Easier than cookies
 *
 * ALTERNATIVES:
 * - Session-based: Doesn't scale across services
 * - OAuth2: More complex, overkill for internal services
 * - Basic auth: Less secure, no expiration
 *
 * RATIONALE:
 * - JWT is standard for microservices
 * - Stateless architecture scales better
 * - Consistent with user-service auth model
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (not needed for stateless REST APIs)
            .csrf(AbstractHttpConfigurer::disable)

            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Allow public access to actuator endpoints
                .requestMatchers("/actuator/**").permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )

            // Stateless session management (no sessions)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Add JWT authentication filter
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
