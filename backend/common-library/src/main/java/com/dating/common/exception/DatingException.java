package com.dating.common.exception;

/**
 * Base exception class for the POC Dating application.
 * All custom exceptions should extend this class to ensure
 * consistent exception handling across all microservices.
 *
 * <p>This exception extends RuntimeException to allow unchecked exception
 * handling, which is the standard approach for service layer exceptions.</p>
 */
public class DatingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new DatingException with the specified message.
     *
     * @param message The detail message
     */
    public DatingException(String message) {
        super(message);
    }

    /**
     * Constructs a new DatingException with the specified message and cause.
     *
     * @param message The detail message
     * @param cause The cause of the exception
     */
    public DatingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new DatingException with the specified cause.
     *
     * @param cause The cause of the exception
     */
    public DatingException(Throwable cause) {
        super(cause);
    }
}
