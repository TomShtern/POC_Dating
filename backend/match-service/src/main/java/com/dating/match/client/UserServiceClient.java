package com.dating.match.client;

import com.dating.match.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client for User Service communication.
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
     * @return User profile response
     */
    @GetMapping("/api/users/{userId}")
    UserProfileResponse getUserById(@PathVariable("userId") UUID userId);

    /**
     * Get user preferences by ID.
     *
     * @param userId User ID
     * @return User preferences response
     */
    @GetMapping("/api/users/{userId}/preferences")
    UserPreferencesResponse getPreferences(@PathVariable("userId") UUID userId);

    /**
     * User profile response from user-service.
     */
    record UserProfileResponse(
        UUID id,
        String email,
        String username,
        String firstName,
        String lastName,
        int age,
        String gender,
        String bio,
        String profilePictureUrl,
        String status
    ) {}

    /**
     * User preferences response from user-service.
     */
    record UserPreferencesResponse(
        UUID id,
        UUID userId,
        int minAge,
        int maxAge,
        int maxDistanceKm,
        String interestedIn,
        java.util.List<String> interests
    ) {}
}
