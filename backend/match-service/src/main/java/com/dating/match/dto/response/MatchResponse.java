package com.dating.match.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for match information in list view.
 *
 * @param id Match ID
 * @param matchedUser Information about the matched user
 * @param matchedAt When the match was created
 */
public record MatchResponse(
    UUID id,
    MatchedUserInfo matchedUser,
    Instant matchedAt
) {
    /**
     * Information about a matched user.
     *
     * @param id User ID
     * @param name User's display name
     * @param profilePictureUrl User's profile picture
     * @param lastMessage Last message in the conversation
     * @param lastMessageTime When the last message was sent
     * @param unreadCount Number of unread messages
     */
    public record MatchedUserInfo(
        UUID id,
        String name,
        String profilePictureUrl,
        String lastMessage,
        Instant lastMessageTime,
        int unreadCount
    ) {}
}
