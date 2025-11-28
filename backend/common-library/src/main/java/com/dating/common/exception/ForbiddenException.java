package com.dating.common.exception;

/**
 * Exception thrown when a user is authenticated but not authorized
 * to access a resource or perform an action.
 *
 * <p>Maps to HTTP 403 Forbidden status code.</p>
 *
 * <p>Use this when the user is authenticated but lacks permission.
 * Use {@link UnauthorizedException} when authentication is missing.</p>
 */
public class ForbiddenException extends DatingException {

    private static final long serialVersionUID = 1L;

    /**
     * Default message for forbidden access.
     */
    private static final String DEFAULT_MESSAGE = "Access denied";

    /**
     * Constructs a new ForbiddenException with default message.
     */
    public ForbiddenException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new ForbiddenException with the specified message.
     *
     * @param message The detail message
     */
    public ForbiddenException(String message) {
        super(message);
    }

    /**
     * Create exception for accessing another user's resource.
     *
     * @param resourceType Type of resource
     * @return New ForbiddenException instance
     */
    public static ForbiddenException cannotAccessOtherUserResource(String resourceType) {
        return new ForbiddenException(
                String.format("You are not allowed to access this %s", resourceType));
    }

    /**
     * Create exception for suspended account.
     *
     * @return New ForbiddenException instance
     */
    public static ForbiddenException accountSuspended() {
        return new ForbiddenException("Your account has been suspended");
    }

    /**
     * Create exception for unverified email.
     *
     * @return New ForbiddenException instance
     */
    public static ForbiddenException emailNotVerified() {
        return new ForbiddenException("Please verify your email address to continue");
    }
}
