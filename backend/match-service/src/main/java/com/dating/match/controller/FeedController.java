package com.dating.match.controller;

import com.dating.match.dto.response.FeedResponse;
import com.dating.match.service.FeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for swiping feed operations.
 */
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Slf4j
public class FeedController {

    private final FeedService feedService;

    /**
     * Get swiping feed for a user.
     *
     * @param userId User ID from path
     * @param requestUserId User ID from X-User-Id header (for authorization)
     * @param limit Number of users to return (default 10)
     * @param offset Pagination offset (default 0)
     * @return Feed response
     */
    @GetMapping("/feed/{userId}")
    public ResponseEntity<FeedResponse> getFeed(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") UUID requestUserId,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {
        log.debug("Feed request for user {} by {}", userId, requestUserId);

        // Verify the requesting user is the same as the path user
        // (or implement admin access if needed)
        if (!userId.equals(requestUserId)) {
            log.warn("User {} attempted to access feed of user {}", requestUserId, userId);
            return ResponseEntity.status(403).build();
        }

        FeedResponse response = feedService.getFeed(userId, limit, offset);
        return ResponseEntity.ok(response);
    }
}
