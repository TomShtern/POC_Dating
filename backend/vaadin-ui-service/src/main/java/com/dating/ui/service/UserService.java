package com.dating.ui.service;

import com.dating.ui.client.UserServiceClient;
import com.dating.ui.dto.AuthResponse;
import com.dating.ui.dto.LoginRequest;
import com.dating.ui.dto.RegisterRequest;
import com.dating.ui.dto.User;
import com.dating.ui.security.SecurityUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service layer for user-related operations
 * Wraps UserServiceClient and handles authentication
 */
@Service
@Slf4j
public class UserService {

    private final UserServiceClient userClient;
    private final Counter loginAttemptsCounter;
    private final Counter loginCounter;
    private final Counter loginFailureCounter;
    private final Counter registrationCounter;
    private final Timer apiCallTimer;

    public UserService(UserServiceClient userClient, MeterRegistry meterRegistry) {
        this.userClient = userClient;
        this.loginAttemptsCounter = Counter.builder("ui.login.attempts.total")
            .description("Total number of login attempts")
            .register(meterRegistry);
        this.loginCounter = Counter.builder("ui.login.success.total")
            .description("Total number of successful logins")
            .register(meterRegistry);
        this.loginFailureCounter = Counter.builder("ui.login.failure.total")
            .description("Total number of failed login attempts")
            .register(meterRegistry);
        this.registrationCounter = Counter.builder("ui.registrations.total")
            .description("Total number of successful registrations")
            .register(meterRegistry);
        this.apiCallTimer = Timer.builder("ui.api.call.time")
            .description("Time spent calling backend services")
            .tag("service", "user-service")
            .register(meterRegistry);
    }

    /**
     * Increment the login failure counter.
     * Call this when login fails for security monitoring.
     */
    public void recordLoginFailure() {
        loginFailureCounter.increment();
    }

    /**
     * Login user
     */
    public AuthResponse login(String email, String password) {
        log.debug("Attempting login for email: {}", email);
        loginAttemptsCounter.increment();
        LoginRequest request = new LoginRequest(email, password);

        AuthResponse response = apiCallTimer.record(() -> userClient.login(request));

        // Store auth info in session
        SecurityUtils.setAuthenticationInfo(
            response.getUser().getId(),
            response.getAccessToken(),
            response.getUser().getFirstName()
        );

        loginCounter.increment();
        log.debug("User logged in successfully");
        return response;
    }

    /**
     * Register new user
     */
    public AuthResponse register(RegisterRequest request) {
        log.debug("Attempting registration for email: {}", request.getEmail());

        AuthResponse response = apiCallTimer.record(() -> userClient.register(request));

        // Store auth info in session
        SecurityUtils.setAuthenticationInfo(
            response.getUser().getId(),
            response.getAccessToken(),
            response.getUser().getFirstName()
        );

        registrationCounter.increment();
        log.debug("User registered successfully");
        return response;
    }

    /**
     * Get current user profile
     */
    public User getCurrentUser() {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return apiCallTimer.record(() -> userClient.getUser(userId, "Bearer " + token));
    }

    /**
     * Update user profile
     */
    public User updateProfile(User user) {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return apiCallTimer.record(() -> userClient.updateUser(userId, user, "Bearer " + token));
    }

    /**
     * Logout current user
     */
    public void logout() {
        String userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            log.debug("User session ended");
        }
        SecurityUtils.clearAuthentication();
    }
}
