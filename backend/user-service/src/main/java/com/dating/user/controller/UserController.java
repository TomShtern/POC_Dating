package com.dating.user.controller;

import com.dating.user.dto.request.UpdateUserRequest;
import com.dating.user.dto.response.UserResponse;
import com.dating.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for user profile management.
 * Handles user CRUD operations.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Get user profile by ID.
     *
     * @param userId User UUID
     * @return 200 OK with user response
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        log.debug("Get user request for: {}", userId);
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update user profile.
     *
     * @param userId User UUID
     * @param request Updated user data
     * @return 200 OK with updated user response
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        log.debug("Update user request for: {}", userId);
        UserResponse response = userService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete user account (soft delete).
     *
     * @param userId User UUID
     * @return 204 No Content
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        log.debug("Delete user request for: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get candidate users for matching.
     * Used by Match Service and Recommendation Service.
     *
     * @param userId User UUID requesting candidates
     * @param minAge Minimum age filter (default 18)
     * @param maxAge Maximum age filter (default 100)
     * @param maxDistance Maximum distance in km (default 100)
     * @param excludeIds List of user IDs to exclude
     * @return 200 OK with list of candidate users
     */
    @GetMapping("/{userId}/candidates")
    public ResponseEntity<List<UserResponse>> getCandidates(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "18") int minAge,
            @RequestParam(defaultValue = "100") int maxAge,
            @RequestParam(defaultValue = "100") int maxDistance,
            @RequestParam(required = false) List<UUID> excludeIds) {
        log.debug("Get candidates request for user: {}, minAge: {}, maxAge: {}, maxDistance: {}",
                userId, minAge, maxAge, maxDistance);
        List<UserResponse> candidates = userService.getCandidates(userId, minAge, maxAge,
                maxDistance, excludeIds);
        return ResponseEntity.ok(candidates);
    }

    /**
     * Get multiple users by their IDs.
     * Used for batch fetching user profiles.
     *
     * @param userIds List of user UUIDs
     * @return 200 OK with list of user responses
     */
    @PostMapping("/batch")
    public ResponseEntity<List<UserResponse>> getUsersByIds(
            @RequestBody List<UUID> userIds) {
        log.debug("Batch get users request, count: {}", userIds != null ? userIds.size() : 0);
        List<UserResponse> users = userService.getUsersByIds(userIds);
        return ResponseEntity.ok(users);
    }
}
