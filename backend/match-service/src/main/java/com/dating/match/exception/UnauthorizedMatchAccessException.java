package com.dating.match.exception;

/**
 * Exception thrown when a user attempts to access a match they are not part of.
 */
public class UnauthorizedMatchAccessException extends RuntimeException {

    public UnauthorizedMatchAccessException(String message) {
        super(message);
    }

    public UnauthorizedMatchAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
