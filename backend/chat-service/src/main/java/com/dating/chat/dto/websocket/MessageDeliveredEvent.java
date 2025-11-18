package com.dating.chat.dto.websocket;

import java.time.Instant;
import java.util.UUID;

/**
 * Delivery confirmation.
 * Server sends to: /user/{senderId}/queue/delivered
 */
public record MessageDeliveredEvent(
        UUID messageId,
        UUID matchId,
        Instant deliveredAt
) {}
