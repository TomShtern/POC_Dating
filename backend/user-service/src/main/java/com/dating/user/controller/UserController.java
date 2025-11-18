package com.dating.user.controller;

import com.dating.user.dto.request.UpdateUserRequest;
import com.dating.user.dto.response.UserResponse;
import com.dating.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
