package com.dating.chat.dto.websocket;

import java.util.UUID;

/**
 * Request to mark messages as read.
 * Client sends to: /app/chat.read
 */
public record MarkReadRequest(
        UUID matchId,
        UUID lastReadMessageId
) {}
