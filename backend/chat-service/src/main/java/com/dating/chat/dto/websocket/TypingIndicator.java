package com.dating.chat.dto.websocket;

import java.util.UUID;

/**
 * Typing indicator from client.
 * Client sends to: /app/chat.typing
 */
public record TypingIndicator(
        UUID matchId,
        boolean isTyping
) {}
