package com.dating.common.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 *
 * <p>Maps to HTTP 409 Conflict status code.</p>
 */
public class DuplicateResourceException extends DatingException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new DuplicateResourceException with the specified message.
     *
     * @param message The detail message
     */
    public DuplicateResourceException(String message) {
        super(message);
    }

    /**
     * Constructs a new DuplicateResourceException for a specific resource type and field.
     *
     * @param resourceType Type of resource (e.g., "User", "Match")
     * @param field Field that has duplicate value (e.g., "email")
     * @param value The duplicate value
     */
    public DuplicateResourceException(String resourceType, String field, Object value) {
        super(String.format("%s already exists with %s: %s", resourceType, field, value));
    }

    /**
     * Create exception for duplicate email.
     *
     * @param email The duplicate email
     * @return New DuplicateResourceException instance
     */
    public static DuplicateResourceException email(String email) {
        return new DuplicateResourceException("User", "email", email);
    }

    /**
     * Create exception for duplicate username.
     *
     * @param username The duplicate username
     * @return New DuplicateResourceException instance
     */
    public static DuplicateResourceException username(String username) {
        return new DuplicateResourceException("User", "username", username);
    }
}
