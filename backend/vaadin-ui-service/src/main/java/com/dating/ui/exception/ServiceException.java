package com.dating.ui.exception;

/**
 * Custom exception for service layer errors.
 * Wraps Feign exceptions and provides user-friendly error messages.
 */
public class ServiceException extends RuntimeException {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
