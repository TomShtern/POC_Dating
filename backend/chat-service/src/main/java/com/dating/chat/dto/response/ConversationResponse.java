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
     * The other participant's user ID.
     */
    private UUID participantId;

    /**
     * The other participant's first name.
     */
    private String participantFirstName;

    /**
     * The other participant's profile picture URL.
     */
    private String participantPhotoUrl;

    /**
     * Last message in the conversation.
     */
    private MessageResponse lastMessage;

    /**
     * Number of unread messages for the current user.
     */
    private int unreadCount;

    /**
     * When the conversation was created (match time).
     */
    private Instant createdAt;

    /**
     * When the last message was sent.
     */
    private Instant lastMessageAt;
}
