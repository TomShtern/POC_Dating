package com.dating.ui.service;

import com.dating.ui.client.UserServiceClient;
import com.dating.ui.dto.AuthResponse;
import com.dating.ui.dto.LoginRequest;
import com.dating.ui.dto.RegisterRequest;
import com.dating.ui.dto.User;
import com.dating.ui.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service layer for user-related operations
 * Wraps UserServiceClient and handles authentication
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserServiceClient userClient;

    /**
     * Login user
     */
    public AuthResponse login(String email, String password) {
        log.debug("Attempting login for email: {}", email);
        LoginRequest request = new LoginRequest(email, password);
        AuthResponse response = userClient.login(request);

        // Store auth info in session
        SecurityUtils.setAuthenticationInfo(
            response.getUser().getId(),
            response.getAccessToken(),
            response.getUser().getFirstName()
        );

        log.info("User logged in successfully: {}", email);
        return response;
    }

    /**
     * Register new user
     */
    public AuthResponse register(RegisterRequest request) {
        log.debug("Attempting registration for email: {}", request.getEmail());
        AuthResponse response = userClient.register(request);

        // Store auth info in session
        SecurityUtils.setAuthenticationInfo(
            response.getUser().getId(),
            response.getAccessToken(),
            response.getUser().getFirstName()
        );

        log.info("User registered successfully: {}", request.getEmail());
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

        return userClient.getUser(userId, "Bearer " + token);
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

        return userClient.updateUser(userId, user, "Bearer " + token);
    }

    /**
     * Logout current user
     */
    public void logout() {
        log.info("User logged out: {}", SecurityUtils.getCurrentUserId());
        SecurityUtils.clearAuthentication();
    }

    /**
     * Get user preferences
     */
    public User getPreferences() {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return userClient.getPreferences(userId, "Bearer " + token);
    }

    /**
     * Update user preferences
     */
    public User updatePreferences(User preferences) {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return userClient.updatePreferences(userId, preferences, "Bearer " + token);
    }
}
