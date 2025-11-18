package com.dating.chat.dto.websocket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Message sent by client to send a chat message.
 * Client sends to: /app/chat.send
 */
public record SendMessageRequest(
        @NotNull(message = "Match ID is required")
        UUID matchId,

        @NotBlank(message = "Message content cannot be empty")
        @Size(max = 10000, message = "Message content cannot exceed 10000 characters")
        String content,

        @NotNull(message = "Message type is required")
        MessageType type
) {}
