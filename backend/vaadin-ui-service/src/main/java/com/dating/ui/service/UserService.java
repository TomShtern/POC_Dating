package com.dating.ui.service;

import com.dating.ui.client.UserServiceClient;
import com.dating.ui.dto.*;
import com.dating.ui.security.SecurityUtils;

import java.util.List;
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

    /**
     * Change password
     */
    public void changePassword(String currentPassword, String newPassword) {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword);
        userClient.changePassword(userId, request, "Bearer " + token);
        log.info("Password changed for user: {}", userId);
    }

    /**
     * Request password reset
     */
    public void forgotPassword(String email) {
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        userClient.forgotPassword(request);
        log.info("Password reset requested for email: {}", email);
    }

    /**
     * Reset password with token
     */
    public void resetPassword(String resetToken, String newPassword) {
        ResetPasswordRequest request = new ResetPasswordRequest(resetToken, newPassword);
        userClient.resetPassword(request);
        log.info("Password reset completed");
    }

    /**
     * Delete account
     */
    public void deleteAccount() {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        userClient.deleteAccount(userId, "Bearer " + token);
        log.info("Account deleted: {}", userId);
        SecurityUtils.clearAuthentication();
    }

    /**
     * Block a user
     */
    public void blockUser(String blockedUserId) {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        BlockRequest request = new BlockRequest(blockedUserId);
        userClient.blockUser(userId, request, "Bearer " + token);
        log.info("User {} blocked by {}", blockedUserId, userId);
    }

    /**
     * Unblock a user
     */
    public void unblockUser(String blockedUserId) {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        userClient.unblockUser(userId, blockedUserId, "Bearer " + token);
        log.info("User {} unblocked by {}", blockedUserId, userId);
    }

    /**
     * Get blocked users
     */
    public List<BlockedUser> getBlockedUsers() {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return userClient.getBlockedUsers(userId, "Bearer " + token);
    }

    /**
     * Report a user
     */
    public void reportUser(String reportedUserId, String reason, String description) {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        ReportRequest request = ReportRequest.builder()
            .reportedUserId(reportedUserId)
            .reason(reason)
            .description(description)
            .build();

        userClient.reportUser(userId, request, "Bearer " + token);
        log.info("User {} reported by {}: {}", reportedUserId, userId, reason);
    }
}
