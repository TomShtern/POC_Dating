package com.dating.userservice.controller;

import com.dating.userservice.dto.UpdateProfileRequest;
import com.dating.userservice.dto.UserProfileDTO;
import com.dating.userservice.dto.UserSummaryDTO;
import com.dating.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * User Controller
 *
 * REST endpoints for user profile management.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Get current user profile
     *
     * @param authentication Current authenticated user
     * @return User profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        log.info("GET /api/users/me - Fetching current user profile: {}", userId);
        UserProfileDTO profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Get user profile by ID
     *
     * @param userId User ID
     * @return User profile
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable UUID userId) {
        log.info("GET /api/users/{} - Fetching user profile", userId);
        UserProfileDTO profile = userService.getActiveUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Get user summary by ID (lightweight)
     *
     * @param userId User ID
     * @return User summary
     */
    @GetMapping("/{userId}/summary")
    public ResponseEntity<UserSummaryDTO> getUserSummary(@PathVariable UUID userId) {
        log.info("GET /api/users/{}/summary - Fetching user summary", userId);
        UserSummaryDTO summary = userService.getUserSummary(userId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Update user profile
     *
     * @param authentication Current authenticated user
     * @param request Profile update data
     * @return Updated user profile
     */
    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        log.info("PUT /api/users/me - Updating user profile: {}", userId);
        UserProfileDTO profile = userService.updateProfile(userId, request);
        return ResponseEntity.ok(profile);
    }

    /**
     * Update user profile by ID (admin use case)
     *
     * @param userId User ID
     * @param request Profile update data
     * @return Updated user profile
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileDTO> updateUserProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        log.info("PUT /api/users/{} - Updating user profile", userId);
        UserProfileDTO profile = userService.updateProfile(userId, request);
        return ResponseEntity.ok(profile);
    }

    /**
     * Delete current user account
     *
     * @param authentication Current authenticated user
     * @return Success message
     */
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteAccount(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        log.info("DELETE /api/users/me - Deleting user account: {}", userId);
        userService.deleteAccount(userId);
        return ResponseEntity.ok("Account deleted successfully");
    }

    /**
     * Delete user account by ID (admin use case)
     *
     * @param userId User ID
     * @return Success message
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUserAccount(@PathVariable UUID userId) {
        log.info("DELETE /api/users/{} - Deleting user account", userId);
        userService.deleteAccount(userId);
        return ResponseEntity.ok("Account deleted successfully");
    }
}
