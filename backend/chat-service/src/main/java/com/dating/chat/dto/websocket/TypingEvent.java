package com.dating.chat.dto.websocket;

import java.util.UUID;

/**
 * Typing indicator broadcast.
 * Server sends to: /user/{recipientId}/queue/typing
 */
public record TypingEvent(
        UUID matchId,
        UUID userId,
        boolean isTyping
) {}
