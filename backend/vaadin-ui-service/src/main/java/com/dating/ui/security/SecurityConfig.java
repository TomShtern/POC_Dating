package com.dating.ui.security;

import com.dating.ui.views.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Bean;

/**
 * Security configuration for Vaadin UI
 * - Protects all views except login/register
 * - Session-based authentication (stored in Redis)
 * - JWT tokens stored in session for backend API calls
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Allow public access to login and register pages
        http.authorizeHttpRequests(auth ->
            auth.requestMatchers("/images/**").permitAll()
        );

        super.configure(http);

        // Set login view
        setLoginView(http, LoginView.class);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
