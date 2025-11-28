package com.dating.common.exception;

/**
 * Exception thrown when login credentials are invalid.
 * This includes wrong password or non-existent email.
 *
 * <p>Maps to HTTP 401 Unauthorized status code.</p>
 *
 * <p>Note: For security reasons, error messages should not distinguish
 * between "user not found" and "wrong password".</p>
 */
public class InvalidCredentialsException extends DatingException {

    private static final long serialVersionUID = 1L;

    /**
     * Default message for invalid credentials.
     */
    private static final String DEFAULT_MESSAGE = "Invalid email or password";

    /**
     * Constructs a new InvalidCredentialsException with default message.
     */
    public InvalidCredentialsException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new InvalidCredentialsException with the specified message.
     *
     * @param message The detail message
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
