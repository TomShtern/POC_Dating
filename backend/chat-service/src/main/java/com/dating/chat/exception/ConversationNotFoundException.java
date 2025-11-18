package com.dating.chat.exception;

import java.util.UUID;

/**
 * Exception thrown when a conversation is not found.
 */
public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(UUID matchId) {
        super("Conversation not found for match: " + matchId);
    }

    public ConversationNotFoundException(String message) {
        super(message);
    }
}
