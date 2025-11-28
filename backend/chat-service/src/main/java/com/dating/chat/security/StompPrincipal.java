package com.dating.chat.security;

import java.security.Principal;

/**
 * Simple Principal implementation for WebSocket.
 *
 * Holds user ID and username extracted from JWT token.
 */
public record StompPrincipal(String userId, String username) implements Principal {

    /**
     * Returns the user ID as the principal name.
     * This is used by Spring to route user-specific messages.
     */
    @Override
    public String getName() {
        return userId;
    }
}
