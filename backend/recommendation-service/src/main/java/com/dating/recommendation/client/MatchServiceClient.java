package com.dating.recommendation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feign client for communicating with the Match Service.
 *
 * This client is used to fetch swipe history and matches
 * to exclude users that have already been swiped on.
 */
@FeignClient(
    name = "match-service",
    url = "${feign.match-service.url}",
    configuration = FeignClientConfig.class
)
public interface MatchServiceClient {

    /**
     * Get all user IDs that the given user has already swiped on (left or right).
     * This is used to exclude them from recommendations.
     *
     * @param userId the user ID
     * @return list of user IDs that have been swiped on
     */
    @GetMapping("/api/swipes/{userId}/swiped-users")
    List<Long> getSwipedUserIds(@PathVariable("userId") Long userId);

    /**
     * Get all matched user IDs for the given user.
     * These should also be excluded from new recommendations.
     *
     * @param userId the user ID
     * @return list of matched user IDs
     */
    @GetMapping("/api/matches/{userId}/matched-users")
    List<Long> getMatchedUserIds(@PathVariable("userId") Long userId);
}
