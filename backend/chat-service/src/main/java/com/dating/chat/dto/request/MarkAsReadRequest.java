package com.dating.chat.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for marking messages as read.
 *
 * @param conversationId The match/conversation ID
 * @param lastMessageId Optional - mark all messages up to this ID as read
 */
public record MarkAsReadRequest(
    @NotNull(message = "Conversation ID is required")
    UUID conversationId,

    UUID lastMessageId
) {}
