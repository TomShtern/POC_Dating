package com.dating.common.exception;

/**
 * Exception thrown when a requested resource is not found.
 * This is a generic not-found exception that can be used for any resource type.
 *
 * <p>Maps to HTTP 404 Not Found status code.</p>
 */
public class ResourceNotFoundException extends DatingException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new ResourceNotFoundException with the specified message.
     *
     * @param message The detail message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new ResourceNotFoundException for a specific resource type and ID.
     *
     * @param resourceType Type of resource (e.g., "User", "Match")
     * @param id ID of the resource
     */
    public ResourceNotFoundException(String resourceType, Object id) {
        super(String.format("%s not found with id: %s", resourceType, id));
    }

    /**
     * Constructs a new ResourceNotFoundException with message and cause.
     *
     * @param message The detail message
     * @param cause The cause of the exception
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
