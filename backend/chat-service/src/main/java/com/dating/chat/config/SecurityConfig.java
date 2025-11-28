package com.dating.chat.config;

import com.dating.common.security.XUserIdValidationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the Chat Service.
 * Relies on API Gateway for JWT validation.
 * Trusts X-User-Id header set by gateway.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Configure security filter chain.
     * All requests come through API Gateway which validates JWT.
     * We trust the X-User-Id header for authentication.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Health check endpoints
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // WebSocket endpoint
                .requestMatchers("/ws/**").permitAll()
                // All other endpoints require X-User-Id header (validated by filter)
                .anyRequest().authenticated())
            .addFilterBefore(new XUserIdValidationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
