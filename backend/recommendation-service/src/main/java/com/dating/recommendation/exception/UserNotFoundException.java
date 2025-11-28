package com.dating.recommendation.exception;

import java.util.UUID;

/**
 * ============================================================================
 * USER NOT FOUND EXCEPTION
 * ============================================================================
 *
 * PURPOSE:
 * Thrown when a requested user does not exist in the database.
 * Maps to HTTP 404 Not Found.
 *
 * USAGE:
 * throw new UserNotFoundException(userId);
 *
 * HANDLING:
 * Caught by GlobalExceptionHandler and converted to error response.
 *
 * ============================================================================
 */
public class UserNotFoundException extends RuntimeException {

    private final UUID userId;

    public UserNotFoundException(UUID userId) {
        super("User not found: " + userId);
        this.userId = userId;
    }

    public UserNotFoundException(String message) {
        super(message);
        this.userId = null;
    }

    public UUID getUserId() {
        return userId;
    }
}
