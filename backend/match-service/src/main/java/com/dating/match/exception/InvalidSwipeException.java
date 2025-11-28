package com.dating.match.exception;

/**
 * Exception thrown when a swipe is invalid.
 */
public class InvalidSwipeException extends RuntimeException {

    public InvalidSwipeException(String message) {
        super(message);
    }

    public InvalidSwipeException(String message, Throwable cause) {
        super(message, cause);
    }
}
