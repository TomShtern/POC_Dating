package com.dating.common.exception;

import java.util.UUID;

/**
 * Exception thrown when a message is not found in the system.
 *
 * <p>Maps to HTTP 404 Not Found status code.</p>
 */
public class MessageNotFoundException extends ResourceNotFoundException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new MessageNotFoundException with the specified message.
     *
     * @param message The detail message
     */
    public MessageNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new MessageNotFoundException for a specific message ID.
     *
     * @param messageId ID of the message
     */
    public MessageNotFoundException(UUID messageId) {
        super("Message", messageId);
    }
}
