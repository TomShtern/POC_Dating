package com.dating.common.exception;

/**
 * Exception thrown when a JWT token has expired.
 *
 * <p>Maps to HTTP 401 Unauthorized status code.</p>
 *
 * <p>Clients receiving this error should attempt to refresh
 * their access token using the refresh token.</p>
 */
public class TokenExpiredException extends DatingException {

    private static final long serialVersionUID = 1L;

    /**
     * Default message for expired token.
     */
    private static final String DEFAULT_MESSAGE = "Token has expired";

    /**
     * Constructs a new TokenExpiredException with default message.
     */
    public TokenExpiredException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new TokenExpiredException with the specified message.
     *
     * @param message The detail message
     */
    public TokenExpiredException(String message) {
        super(message);
    }

    /**
     * Create exception for expired access token.
     *
     * @return New TokenExpiredException instance
     */
    public static TokenExpiredException accessToken() {
        return new TokenExpiredException("Access token has expired. Please refresh your token.");
    }

    /**
     * Create exception for expired refresh token.
     *
     * @return New TokenExpiredException instance
     */
    public static TokenExpiredException refreshToken() {
        return new TokenExpiredException("Refresh token has expired. Please login again.");
    }
}
