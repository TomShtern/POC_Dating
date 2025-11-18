package com.dating.match.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for swiping feed.
 *
 * @param feed List of users to show in feed
 * @param total Total number of available users
 * @param hasMore Whether more users are available
 */
public record FeedResponse(
    List<FeedUserInfo> feed,
    long total,
    boolean hasMore
) {
    /**
     * User information for feed display.
     *
     * @param id User ID
     * @param name User's display name
     * @param age User's age
     * @param profilePictureUrl User's profile picture
     * @param bio User's bio
     * @param compatibilityScore Compatibility score with current user
     */
    public record FeedUserInfo(
        UUID id,
        String name,
        int age,
        String profilePictureUrl,
        String bio,
        int compatibilityScore
    ) {}
}
