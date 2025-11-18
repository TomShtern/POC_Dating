package com.dating.match.dto.response;

import com.dating.common.constant.SwipeType;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for swipe action result.
 *
 * @param id Swipe ID
 * @param userId User who swiped
 * @param targetUserId Target user
 * @param action Swipe action
 * @param isMatch Whether this swipe resulted in a match
 * @param matchId Match ID if a match was created
 * @param matchedAt When the match was created (if applicable)
 * @param createdAt When the swipe was recorded
 */
public record SwipeResponse(
    UUID id,
    UUID userId,
    UUID targetUserId,
    SwipeType action,
    boolean isMatch,
    UUID matchId,
    Instant matchedAt,
    Instant createdAt
) {
    /**
     * Create a SwipeResponse without a match.
     */
    public static SwipeResponse noMatch(UUID id, UUID userId, UUID targetUserId,
                                         SwipeType action, Instant createdAt) {
        return new SwipeResponse(id, userId, targetUserId, action, false, null, null, createdAt);
    }

    /**
     * Create a SwipeResponse with a match.
     */
    public static SwipeResponse withMatch(UUID id, UUID userId, UUID targetUserId,
                                           SwipeType action, UUID matchId,
                                           Instant matchedAt, Instant createdAt) {
        return new SwipeResponse(id, userId, targetUserId, action, true, matchId, matchedAt, createdAt);
    }
}
