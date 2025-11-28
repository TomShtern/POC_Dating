package com.dating.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a list of messages with pagination metadata.
 * Wraps the messages array with conversation context and pagination info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageListResponse {

    /**
     * Conversation/match ID.
     */
    private UUID conversationId;

    /**
     * List of messages.
     */
    private List<MessageResponse> messages;

    /**
     * Total number of messages in the conversation.
     */
    private int total;

    /**
     * Page size.
     */
    private int limit;

    /**
     * Current offset.
     */
    private int offset;

    /**
     * Whether there are more messages to load.
     */
    private boolean hasMore;
}
