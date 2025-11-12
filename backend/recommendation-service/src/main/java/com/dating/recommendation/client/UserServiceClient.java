package com.dating.recommendation.client;

import com.dating.recommendation.dto.UserSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign client for communicating with the User Service.
 *
 * This client is used to fetch user profiles and information
 * needed for generating recommendations.
 */
@FeignClient(
    name = "user-service",
    url = "${feign.user-service.url}",
    configuration = FeignClientConfig.class
)
public interface UserServiceClient {

    /**
     * Get a user profile by ID.
     *
     * @param userId the user ID
     * @return user summary DTO
     */
    @GetMapping("/api/users/{userId}")
    UserSummaryDTO getUserById(@PathVariable("userId") Long userId);

    /**
     * Get multiple user profiles by IDs.
     *
     * @param userIds comma-separated list of user IDs
     * @return list of user summary DTOs
     */
    @GetMapping("/api/users/batch")
    List<UserSummaryDTO> getUsersByIds(@RequestParam("ids") String userIds);

    /**
     * Get all active users (for generating recommendations).
     * This endpoint should ideally support pagination and filtering.
     *
     * @param excludeUserId user ID to exclude from results (the requesting user)
     * @param gender optional gender filter
     * @param minAge optional minimum age filter
     * @param maxAge optional maximum age filter
     * @return list of active users
     */
    @GetMapping("/api/users/active")
    List<UserSummaryDTO> getActiveUsers(
            @RequestParam(value = "excludeUserId", required = false) Long excludeUserId,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "minAge", required = false) Integer minAge,
            @RequestParam(value = "maxAge", required = false) Integer maxAge
    );
}
