package com.dating.chat.dto.websocket;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Typing indicator from client.
 * Client sends to: /app/chat.typing
 */
public record TypingIndicator(
        @NotNull(message = "Match ID is required")
        UUID matchId,

        boolean isTyping
) {}
