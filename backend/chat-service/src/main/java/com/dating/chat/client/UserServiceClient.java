package com.dating.chat.client;

import com.dating.chat.dto.UserSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign Client for User Service
 *
 * Communicates with user-service to fetch user profile information
 */
@FeignClient(
        name = "user-service",
        url = "${feign.user-service.url}",
        fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {

    /**
     * Get user profile by ID
     */
    @GetMapping("/api/users/{userId}")
    UserSummaryDTO getUserById(@PathVariable("userId") UUID userId);

    /**
     * Check if user exists
     */
    @GetMapping("/api/users/{userId}/exists")
    Boolean userExists(@PathVariable("userId") UUID userId);
}
