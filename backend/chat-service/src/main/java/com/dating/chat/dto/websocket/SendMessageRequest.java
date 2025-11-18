package com.dating.chat.dto.websocket;

import java.util.UUID;

/**
 * Message sent by client to send a chat message.
 * Client sends to: /app/chat.send
 */
public record SendMessageRequest(
        UUID matchId,
        String content,
        MessageType type
) {}
