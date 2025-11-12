package com.dating.match.client;

import com.dating.match.dto.UserSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign client for User Service communication
 *
 * PURPOSE: Declarative HTTP client for fetching user profile data
 *
 * WHY FEIGN:
 * - Declarative: Interface-based (no implementation needed)
 * - Integration: Works seamlessly with Spring Cloud
 * - Load balancing: Supports service discovery (Eureka/Consul)
 * - Resilience: Easy to add circuit breakers (Resilience4j)
 * - Cleaner: Less boilerplate than RestTemplate
 *
 * CONFIGURATION:
 * - @FeignClient: Defines service name and configuration
 * - name: Logical name of target service
 * - url: Service URL (from application.yml)
 * - configuration: Optional custom config class
 *
 * ALTERNATIVES:
 * - RestTemplate: More verbose, deprecated in new projects
 * - WebClient: Reactive, overkill for simple calls
 * - HTTP Client: Too low-level
 *
 * RATIONALE:
 * - Feign is the standard for Spring microservice communication
 * - Reduces boilerplate code significantly
 * - Easy to test with mocks
 * - Natural fit for REST APIs
 *
 * ERROR HANDLING:
 * - Feign throws FeignException on HTTP errors
 * - Can configure custom error decoder
 * - Service layer handles fallbacks
 *
 * SECURITY:
 * - JWT token propagation via FeignClientInterceptor
 * - Add Authorization header in interceptor
 * - Configured in FeignConfig class
 */
@FeignClient(
    name = "user-service",
    url = "${spring.cloud.openfeign.client.config.user-service.url}"
)
public interface UserServiceClient {

    /**
     * Get user profile summary by ID
     *
     * @param userId User ID
     * @return User profile summary
     */
    @GetMapping("/users/{userId}/summary")
    UserSummaryDTO getUserSummary(@PathVariable("userId") Long userId);

    /**
     * Get multiple user summaries by IDs
     * Used for batch loading (avoid N+1 queries)
     *
     * @param userIds List of user IDs
     * @return List of user summaries
     */
    @GetMapping("/users/summaries")
    List<UserSummaryDTO> getUserSummaries(@RequestParam("ids") List<Long> userIds);

    /**
     * Get recommended users for matching
     * Filters based on preferences, location, etc.
     *
     * @param userId User requesting recommendations
     * @param limit Max number of users to return
     * @param excludeUserIds User IDs to exclude (already swiped)
     * @return List of recommended user summaries
     */
    @GetMapping("/users/recommendations")
    List<UserSummaryDTO> getRecommendations(
        @RequestParam("userId") Long userId,
        @RequestParam("limit") Integer limit,
        @RequestParam("excludeUserIds") List<Long> excludeUserIds
    );

    /**
     * Check if user exists and is active
     * Used for validation before creating swipe
     *
     * @param userId User ID to check
     * @return true if user exists and is active
     */
    @GetMapping("/users/{userId}/exists")
    Boolean userExists(@PathVariable("userId") Long userId);
}
