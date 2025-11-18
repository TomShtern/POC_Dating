package com.dating.chat.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * WebSocket message DTO for typing indicators.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypingIndicator {

    /**
     * Typing indicator type.
     */
    public enum Type {
        TYPING_START,
        TYPING_STOP
    }

    /**
     * Type of indicator.
     */
    private Type type;

    /**
     * Conversation/match ID.
     */
    private UUID conversationId;

    /**
     * User who is typing.
     */
    private UUID userId;

    /**
     * Timestamp.
     */
    private Instant timestamp;

    /**
     * Create a typing start indicator.
     */
    public static TypingIndicator start(UUID conversationId, UUID userId) {
        return TypingIndicator.builder()
                .type(Type.TYPING_START)
                .conversationId(conversationId)
                .userId(userId)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a typing stop indicator.
     */
    public static TypingIndicator stop(UUID conversationId, UUID userId) {
        return TypingIndicator.builder()
                .type(Type.TYPING_STOP)
                .conversationId(conversationId)
                .userId(userId)
                .timestamp(Instant.now())
                .build();
    }
}
