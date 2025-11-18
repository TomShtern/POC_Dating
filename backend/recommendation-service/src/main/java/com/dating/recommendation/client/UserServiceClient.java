package com.dating.recommendation.client;

import com.dating.recommendation.config.FeignClientConfig;
import com.dating.recommendation.dto.UserProfileDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

/**
 * Feign client for User Service.
 * Retrieves user profiles and preferences for scoring calculations.
 */
@FeignClient(
        name = "user-service",
        url = "${services.user-service.url}",
        configuration = FeignClientConfig.class
)
public interface UserServiceClient {

    /**
     * Get user profile by ID.
     *
     * @param userId User ID
     * @return User profile data
     */
    @GetMapping("/api/users/{userId}")
    UserProfileDto getUserById(@PathVariable("userId") UUID userId);

    /**
     * Get user preferences by ID.
     *
     * @param userId User ID
     * @return User preferences
     */
    @GetMapping("/api/users/{userId}/preferences")
    UserProfileDto getUserPreferences(@PathVariable("userId") UUID userId);

    /**
     * Get eligible candidates for a user based on preferences.
     *
     * @param userId User ID
     * @param limit Maximum number of candidates
     * @return List of candidate profiles
     */
    @GetMapping("/api/users/{userId}/candidates")
    List<UserProfileDto> getCandidates(
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "limit", defaultValue = "100") int limit);

    /**
     * Get multiple user profiles by IDs.
     *
     * @param userIds List of user IDs
     * @return List of user profiles
     */
    @PostMapping("/api/users/batch")
    List<UserProfileDto> getUsersByIds(@RequestBody List<UUID> userIds);
}
