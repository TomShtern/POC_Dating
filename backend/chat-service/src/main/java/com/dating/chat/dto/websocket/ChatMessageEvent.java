package com.dating.chat.dto.websocket;

import java.time.Instant;
import java.util.UUID;

/**
 * Chat message broadcast to conversation participants.
 * Server sends to: /user/{recipientId}/queue/messages
 */
public record ChatMessageEvent(
        UUID messageId,
        UUID matchId,
        UUID senderId,
        String senderName,
        String content,
        MessageType type,
        Instant timestamp
) {}
