package com.dating.ui.client;

import com.dating.ui.dto.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign Client for Recommendation Service
 * Handles personalized recommendations
 */
@FeignClient(name = "recommendation-service", url = "${services.recommendation-service.url}")
public interface RecommendationServiceClient {

    @GetMapping("/api/recommendations")
    List<User> getRecommendations(@RequestHeader("Authorization") String token,
                                   @RequestParam(defaultValue = "20") int limit);

    @GetMapping("/api/recommendations/refresh")
    List<User> refreshRecommendations(@RequestHeader("Authorization") String token);
}
