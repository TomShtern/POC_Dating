package com.dating.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for sending a message.
 *
 * @param conversationId The match/conversation ID
 * @param content Message content (max 1000 characters)
 */
public record SendMessageRequest(
    @NotNull(message = "Conversation ID is required")
    UUID conversationId,

    @NotBlank(message = "Message content is required")
    @Size(min = 1, max = 1000, message = "Message must be between 1 and 1000 characters")
    String content
) {}
