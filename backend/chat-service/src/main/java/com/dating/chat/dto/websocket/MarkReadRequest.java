package com.dating.chat.dto.websocket;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request to mark messages as read.
 * Client sends to: /app/chat.read
 */
public record MarkReadRequest(
        @NotNull(message = "Match ID is required")
        UUID matchId,

        @NotNull(message = "Last read message ID is required")
        UUID lastReadMessageId
) {}
