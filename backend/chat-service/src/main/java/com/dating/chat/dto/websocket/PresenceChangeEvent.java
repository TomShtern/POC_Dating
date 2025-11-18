package com.dating.chat.dto.websocket;

import java.time.Instant;

/**
 * Presence change event broadcast to all connected clients.
 * Sent to: /topic/presence
 */
public record PresenceChangeEvent(
        String userId,
        String username,
        boolean isOnline,
        Instant timestamp
) {}
