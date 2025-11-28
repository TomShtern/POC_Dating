package com.dating.common.exception;

import java.util.UUID;

/**
 * Exception thrown when a match is not found in the system.
 *
 * <p>Maps to HTTP 404 Not Found status code.</p>
 */
public class MatchNotFoundException extends ResourceNotFoundException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new MatchNotFoundException with the specified message.
     *
     * @param message The detail message
     */
    public MatchNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new MatchNotFoundException for a specific match ID.
     *
     * @param matchId ID of the match
     */
    public MatchNotFoundException(UUID matchId) {
        super("Match", matchId);
    }

    /**
     * Create exception for match not found between two users.
     *
     * @param user1Id First user ID
     * @param user2Id Second user ID
     * @return New MatchNotFoundException instance
     */
    public static MatchNotFoundException betweenUsers(UUID user1Id, UUID user2Id) {
        return new MatchNotFoundException(
                String.format("Match not found between users: %s and %s", user1Id, user2Id));
    }
}
