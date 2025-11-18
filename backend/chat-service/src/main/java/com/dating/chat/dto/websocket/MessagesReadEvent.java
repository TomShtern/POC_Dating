package com.dating.chat.dto.websocket;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification that messages were read.
 * Server sends to: /user/{senderId}/queue/read
 */
public record MessagesReadEvent(
        UUID matchId,
        UUID readByUserId,
        UUID lastReadMessageId,
        Instant readAt
) {}
