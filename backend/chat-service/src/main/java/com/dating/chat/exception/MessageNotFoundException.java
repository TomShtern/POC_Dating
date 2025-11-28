package com.dating.chat.exception;

import java.util.UUID;

/**
 * Exception thrown when a message is not found.
 */
public class MessageNotFoundException extends RuntimeException {

    public MessageNotFoundException(UUID messageId) {
        super("Message not found: " + messageId);
    }

    public MessageNotFoundException(String message) {
        super(message);
    }
}
