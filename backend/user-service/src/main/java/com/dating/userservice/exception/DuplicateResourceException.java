package com.dating.userservice.exception;

/**
 * Duplicate Resource Exception
 *
 * Thrown when attempting to create a resource that already exists (e.g., email already registered).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
