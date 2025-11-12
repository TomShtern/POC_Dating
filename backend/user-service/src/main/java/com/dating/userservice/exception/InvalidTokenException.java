package com.dating.userservice.exception;

/**
 * Invalid Token Exception
 *
 * Thrown when a refresh token is invalid or expired.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException() {
        super("Invalid or expired token");
    }
}
