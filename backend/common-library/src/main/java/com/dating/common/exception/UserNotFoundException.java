package com.dating.common.exception;

import java.util.UUID;

/**
 * Exception thrown when a user is not found in the system.
 *
 * <p>Maps to HTTP 404 Not Found status code.</p>
 */
public class UserNotFoundException extends ResourceNotFoundException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new UserNotFoundException with the specified message.
     *
     * @param message The detail message
     */
    public UserNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new UserNotFoundException for a specific user ID.
     *
     * @param userId ID of the user
     */
    public UserNotFoundException(UUID userId) {
        super("User", userId);
    }

    /**
     * Create exception for user not found by email.
     *
     * @param email User's email
     * @return New UserNotFoundException instance
     */
    public static UserNotFoundException byEmail(String email) {
        return new UserNotFoundException("User not found with email: " + email);
    }

    /**
     * Create exception for user not found by username.
     *
     * @param username User's username
     * @return New UserNotFoundException instance
     */
    public static UserNotFoundException byUsername(String username) {
        return new UserNotFoundException("User not found with username: " + username);
    }
}
