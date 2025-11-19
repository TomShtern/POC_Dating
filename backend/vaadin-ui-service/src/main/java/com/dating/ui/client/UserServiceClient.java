package com.dating.ui.client;

import com.dating.ui.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feign Client for User Service
 * Handles authentication, user profiles, and preferences
 */
@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserServiceClient {

    @PostMapping("/api/users/auth/login")
    AuthResponse login(@RequestBody LoginRequest request);

    @PostMapping("/api/users/auth/register")
    AuthResponse register(@RequestBody RegisterRequest request);

    @GetMapping("/api/users/{userId}")
    User getUser(@PathVariable String userId, @RequestHeader("Authorization") String token);

    @PutMapping("/api/users/{userId}")
    User updateUser(@PathVariable String userId, @RequestBody User user,
                    @RequestHeader("Authorization") String token);

    @GetMapping("/api/users/{userId}/preferences")
    User getPreferences(@PathVariable String userId, @RequestHeader("Authorization") String token);

    @PutMapping("/api/users/{userId}/preferences")
    User updatePreferences(@PathVariable String userId, @RequestBody User preferences,
                          @RequestHeader("Authorization") String token);

    // Password management
    @PostMapping("/api/users/{userId}/change-password")
    void changePassword(@PathVariable String userId, @RequestBody ChangePasswordRequest request,
                       @RequestHeader("Authorization") String token);

    @PostMapping("/api/users/auth/forgot-password")
    void forgotPassword(@RequestBody ForgotPasswordRequest request);

    @PostMapping("/api/users/auth/reset-password")
    void resetPassword(@RequestBody ResetPasswordRequest request);

    // Account management
    @DeleteMapping("/api/users/{userId}")
    void deleteAccount(@PathVariable String userId, @RequestHeader("Authorization") String token);

    // Block/Report
    @PostMapping("/api/users/{userId}/block")
    void blockUser(@PathVariable String userId, @RequestBody BlockRequest request,
                  @RequestHeader("Authorization") String token);

    @DeleteMapping("/api/users/{userId}/block/{blockedUserId}")
    void unblockUser(@PathVariable String userId, @PathVariable String blockedUserId,
                    @RequestHeader("Authorization") String token);

    @GetMapping("/api/users/{userId}/blocked")
    List<BlockedUser> getBlockedUsers(@PathVariable String userId,
                                      @RequestHeader("Authorization") String token);

    @PostMapping("/api/users/{userId}/report")
    void reportUser(@PathVariable String userId, @RequestBody ReportRequest request,
                   @RequestHeader("Authorization") String token);
}
