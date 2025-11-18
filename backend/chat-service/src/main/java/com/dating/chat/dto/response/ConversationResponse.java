package com.dating.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a conversation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {

    /**
     * Conversation ID (same as match ID).
     */
    private UUID id;

    /**
     * The matched user information.
     */
    private MatchedUser matchedUser;

    /**
     * Last message content.
     */
    private String lastMessage;

    /**
     * When the last message was sent.
     */
    private Instant lastMessageTime;

    /**
     * Number of unread messages for the current user.
     */
    private int unreadCount;

    /**
     * When the conversation was created (match time).
     */
    private Instant createdAt;

    /**
     * Nested class for matched user info.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatchedUser {
        private UUID id;
        private String name;
        private String profilePictureUrl;
    }
}
