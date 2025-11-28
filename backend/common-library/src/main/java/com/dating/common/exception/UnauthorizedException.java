package com.dating.common.exception;

/**
 * Exception thrown when authentication is required but not provided or invalid.
 *
 * <p>Maps to HTTP 401 Unauthorized status code.</p>
 *
 * <p>Use this for missing or invalid authentication tokens.
 * Use {@link InvalidCredentialsException} for login failures.</p>
 */
public class UnauthorizedException extends DatingException {

    private static final long serialVersionUID = 1L;

    /**
     * Default message for unauthorized access.
     */
    private static final String DEFAULT_MESSAGE = "Authentication required";

    /**
     * Constructs a new UnauthorizedException with default message.
     */
    public UnauthorizedException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new UnauthorizedException with the specified message.
     *
     * @param message The detail message
     */
    public UnauthorizedException(String message) {
        super(message);
    }

    /**
     * Create exception for missing authorization header.
     *
     * @return New UnauthorizedException instance
     */
    public static UnauthorizedException missingToken() {
        return new UnauthorizedException("Authorization token is missing");
    }

    /**
     * Create exception for invalid token format.
     *
     * @return New UnauthorizedException instance
     */
    public static UnauthorizedException invalidToken() {
        return new UnauthorizedException("Invalid authorization token");
    }

    /**
     * Create exception for blacklisted token.
     *
     * @return New UnauthorizedException instance
     */
    public static UnauthorizedException blacklistedToken() {
        return new UnauthorizedException("Token has been revoked");
    }
}
