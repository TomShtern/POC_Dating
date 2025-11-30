package com.dating.chat.dto.websocket;

import java.util.UUID;

/**
 * WebSocket message DTO for typing indicators.
 */
public record TypingIndicator(
    UUID matchId,
    boolean isTyping
) {}
