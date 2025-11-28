package com.dating.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a match is created between two users.
 * A match occurs when both users have liked each other.
 * Consumed by Chat Service to create conversation and
 * Notification Service to send match notifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MatchCreatedEvent extends BaseEvent {

    private static final long serialVersionUID = 1L;

    /**
     * ID of the created match.
     */
    private UUID matchId;

    /**
     * ID of the first user in the match.
     */
    private UUID user1Id;

    /**
     * ID of the second user in the match.
     */
    private UUID user2Id;

    /**
     * First name of user1 (for notifications).
     */
    private String user1FirstName;

    /**
     * First name of user2 (for notifications).
     */
    private String user2FirstName;

    /**
     * Photo URL of user1 (for notifications).
     */
    private String user1PhotoUrl;

    /**
     * Photo URL of user2 (for notifications).
     */
    private String user2PhotoUrl;

    /**
     * When the match was created.
     */
    private Instant matchedAt;

    /**
     * Create a new MatchCreatedEvent with default event metadata.
     *
     * @param matchId ID of the match
     * @param user1Id ID of first user
     * @param user2Id ID of second user
     * @return New MatchCreatedEvent instance
     */
    public static MatchCreatedEvent create(UUID matchId, UUID user1Id, UUID user2Id) {
        MatchCreatedEvent event = MatchCreatedEvent.builder()
                .matchId(matchId)
                .user1Id(user1Id)
                .user2Id(user2Id)
                .matchedAt(Instant.now())
                .build();
        event.initializeEvent("match-service", "MATCH_CREATED");
        return event;
    }
}
