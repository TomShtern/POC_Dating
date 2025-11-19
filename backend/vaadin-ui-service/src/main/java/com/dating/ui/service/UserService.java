package com.dating.ui.service;

import com.dating.ui.client.UserServiceClient;
import com.dating.ui.dto.*;
import com.dating.ui.exception.ServiceException;
import com.dating.ui.security.SecurityUtils;

import feign.FeignException;
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

        try {
            LoginRequest request = new LoginRequest(email, password);
            AuthResponse response = userClient.login(request);

            if (response == null || response.getUser() == null || response.getAccessToken() == null) {
                throw new ServiceException("Invalid response from authentication service");
            }

            // Store auth info in session
            SecurityUtils.setAuthenticationInfo(
                response.getUser().getId(),
                response.getAccessToken(),
                response.getUser().getFirstName()
            );

            log.info("User logged in successfully: {}", email);
            return response;
        } catch (FeignException e) {
            log.error("Failed to login user: {}", email, e);
            if (e.status() == 401 || e.status() == 403) {
                throw new ServiceException("Invalid email or password", e);
            }
            throw new ServiceException("Unable to login. Please try again later.", e);
        }
    }

    /**
     * Register new user
     */
    public AuthResponse register(RegisterRequest request) {
        log.debug("Attempting registration for email: {}", request.getEmail());

        try {
            AuthResponse response = userClient.register(request);

            if (response == null || response.getUser() == null || response.getAccessToken() == null) {
                throw new ServiceException("Invalid response from registration service");
            }

            // Store auth info in session
            SecurityUtils.setAuthenticationInfo(
                response.getUser().getId(),
                response.getAccessToken(),
                response.getUser().getFirstName()
            );

            log.info("User registered successfully: {}", request.getEmail());
            return response;
        } catch (FeignException e) {
            log.error("Failed to register user: {}", request.getEmail(), e);
            if (e.status() == 409) {
                throw new ServiceException("Email already registered. Please use a different email.", e);
            }
            if (e.status() == 400) {
                throw new ServiceException("Invalid registration data. Please check your inputs.", e);
            }
            throw new ServiceException("Unable to register. Please try again later.", e);
        }
    }

    /**
     * Get current user profile
     */
    public User getCurrentUser() {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new ServiceException("User not authenticated");
        }

        try {
            User user = userClient.getUser(userId, "Bearer " + token);
            if (user == null) {
                throw new ServiceException("Failed to retrieve user data");
            }
            return user;
        } catch (FeignException e) {
            log.error("Failed to get user: {}", userId, e);
            throw new ServiceException("Unable to load user profile. Please try again.", e);
        }
    }

    /**
     * Update user profile
     */
    public User updateProfile(User user) {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new ServiceException("User not authenticated");
        }

        try {
            User updatedUser = userClient.updateUser(userId, user, "Bearer " + token);
            if (updatedUser == null) {
                throw new ServiceException("Failed to update user profile");
            }
            return updatedUser;
        } catch (FeignException e) {
            log.error("Failed to update user profile: {}", userId, e);
            throw new ServiceException("Unable to update profile. Please try again.", e);
        }
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
            throw new ServiceException("User not authenticated");
        }

        try {
            User preferences = userClient.getPreferences(userId, "Bearer " + token);
            if (preferences == null) {
                throw new ServiceException("Failed to retrieve preferences");
            }
            return preferences;
        } catch (FeignException e) {
            log.error("Failed to get preferences for user: {}", userId, e);
            throw new ServiceException("Unable to load preferences. Please try again.", e);
        }
    }

    /**
     * Update user preferences
     */
    public User updatePreferences(User preferences) {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new ServiceException("User not authenticated");
        }

        try {
            User updatedPreferences = userClient.updatePreferences(userId, preferences, "Bearer " + token);
            if (updatedPreferences == null) {
                throw new ServiceException("Failed to update preferences");
            }
            return updatedPreferences;
        } catch (FeignException e) {
            log.error("Failed to update preferences for user: {}", userId, e);
            throw new ServiceException("Unable to update preferences. Please try again.", e);
        }
    }

    /**
     * Change password
     */
    public void changePassword(String currentPassword, String newPassword) {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new ServiceException("User not authenticated");
        }

        try {
            ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword);
            userClient.changePassword(userId, request, "Bearer " + token);
            log.info("Password changed for user: {}", userId);
        } catch (FeignException e) {
            log.error("Failed to change password for user: {}", userId, e);
            if (e.status() == 400 || e.status() == 401) {
                throw new ServiceException("Current password is incorrect", e);
            }
            throw new ServiceException("Unable to change password. Please try again.", e);
        }
    }

    /**
     * Request password reset
     */
    public void forgotPassword(String email) {
        try {
            ForgotPasswordRequest request = new ForgotPasswordRequest(email);
            userClient.forgotPassword(request);
            log.info("Password reset requested for email: {}", email);
        } catch (FeignException e) {
            log.error("Failed to request password reset for email: {}", email, e);
            // Don't reveal whether email exists for security
            throw new ServiceException("If this email is registered, you will receive a reset link.", e);
        }
    }

    /**
     * Reset password with token
     */
    public void resetPassword(String resetToken, String newPassword) {
        try {
            ResetPasswordRequest request = new ResetPasswordRequest(resetToken, newPassword);
            userClient.resetPassword(request);
            log.info("Password reset completed");
        } catch (FeignException e) {
            log.error("Failed to reset password", e);
            if (e.status() == 400 || e.status() == 404) {
                throw new ServiceException("Invalid or expired reset token. Please request a new one.", e);
            }
            throw new ServiceException("Unable to reset password. Please try again.", e);
        }
    }

    /**
     * Delete account
     */
    public void deleteAccount() {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new ServiceException("User not authenticated");
        }

        try {
            userClient.deleteAccount(userId, "Bearer " + token);
            log.info("Account deleted: {}", userId);
            SecurityUtils.clearAuthentication();
        } catch (FeignException e) {
            log.error("Failed to delete account: {}", userId, e);
            throw new ServiceException("Unable to delete account. Please try again.", e);
        }
    }

    /**
     * Block a user
     */
    public void blockUser(String blockedUserId) {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new ServiceException("User not authenticated");
        }

        try {
            BlockRequest request = new BlockRequest(blockedUserId);
            userClient.blockUser(userId, request, "Bearer " + token);
            log.info("User {} blocked by {}", blockedUserId, userId);
        } catch (FeignException e) {
            log.error("Failed to block user {} by {}", blockedUserId, userId, e);
            throw new ServiceException("Unable to block user. Please try again.", e);
        }
    }

    /**
     * Unblock a user
     */
    public void unblockUser(String blockedUserId) {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new ServiceException("User not authenticated");
        }

        try {
            userClient.unblockUser(userId, blockedUserId, "Bearer " + token);
            log.info("User {} unblocked by {}", blockedUserId, userId);
        } catch (FeignException e) {
            log.error("Failed to unblock user {} by {}", blockedUserId, userId, e);
            throw new ServiceException("Unable to unblock user. Please try again.", e);
        }
    }

    /**
     * Get blocked users
     */
    public List<BlockedUser> getBlockedUsers() {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new ServiceException("User not authenticated");
        }

        try {
            List<BlockedUser> blockedUsers = userClient.getBlockedUsers(userId, "Bearer " + token);
            if (blockedUsers == null) {
                throw new ServiceException("Failed to retrieve blocked users");
            }
            return blockedUsers;
        } catch (FeignException e) {
            log.error("Failed to get blocked users for user: {}", userId, e);
            throw new ServiceException("Unable to load blocked users. Please try again.", e);
        }
    }

    /**
     * Report a user
     */
    public void reportUser(String reportedUserId, String reason, String description) {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();

        if (userId == null || token == null) {
            throw new ServiceException("User not authenticated");
        }

        try {
            ReportRequest request = ReportRequest.builder()
                .reportedUserId(reportedUserId)
                .reason(reason)
                .description(description)
                .build();

            userClient.reportUser(userId, request, "Bearer " + token);
            log.info("User {} reported by {}: {}", reportedUserId, userId, reason);
        } catch (FeignException e) {
            log.error("Failed to report user {} by {}", reportedUserId, userId, e);
            throw new ServiceException("Unable to submit report. Please try again.", e);
        }
    }
}
