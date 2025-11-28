package com.dating.match.exception;

/**
 * Exception thrown when a user attempts to swipe on someone they already swiped on.
 */
public class DuplicateSwipeException extends RuntimeException {

    public DuplicateSwipeException(String message) {
        super(message);
    }

    public DuplicateSwipeException(String message, Throwable cause) {
        super(message, cause);
    }
}
