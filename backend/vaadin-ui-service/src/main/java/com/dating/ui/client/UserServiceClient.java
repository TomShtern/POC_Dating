package com.dating.ui.client;

import com.dating.ui.dto.AuthResponse;
import com.dating.ui.dto.LoginRequest;
import com.dating.ui.dto.RegisterRequest;
import com.dating.ui.dto.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

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
}
