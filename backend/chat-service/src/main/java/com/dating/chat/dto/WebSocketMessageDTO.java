package com.dating.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for WebSocket message transmission
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketMessageDTO {

    private String type; // MESSAGE_RECEIVED, MESSAGE_DELIVERED, MESSAGE_READ, TYPING_START, TYPING_STOP, USER_ONLINE, USER_OFFLINE
    private UUID messageId;
    private UUID conversationId;
    private UUID senderId;
    private UUID recipientId;
    private String content;
    private LocalDateTime timestamp;
    private Object payload; // Generic payload for different message types
}
