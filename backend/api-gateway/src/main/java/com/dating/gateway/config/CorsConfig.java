package com.dating.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for the API Gateway.
 *
 * Allows requests from the Vaadin UI (localhost:8090) and other configured origins.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Allowed origins - Vaadin UI
        corsConfig.setAllowedOrigins(List.of(
                "http://localhost:8090",
                "http://localhost:3000"
        ));

        // Allowed HTTP methods
        corsConfig.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Allowed headers
        corsConfig.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Cache-Control"
        ));

        // Exposed headers (accessible by client)
        corsConfig.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-User-Id"
        ));

        // Allow credentials (cookies, authorization headers)
        corsConfig.setAllowCredentials(true);

        // Preflight cache duration (1 hour)
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
