package com.dating.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a match is ended (unmatched).
 * Consumed by Chat Service to archive conversation and
 * prevent further messaging between users.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MatchEndedEvent extends BaseEvent {

    private static final long serialVersionUID = 1L;

    /**
     * ID of the ended match.
     */
    private UUID matchId;

    /**
     * ID of the user who ended the match.
     */
    private UUID endedByUserId;

    /**
     * ID of the other user in the match.
     */
    private UUID otherUserId;

    /**
     * Reason for ending the match (optional).
     */
    private String reason;

    /**
     * When the match was ended.
     */
    private Instant endedAt;

    /**
     * Create a new MatchEndedEvent with default event metadata.
     *
     * @param matchId ID of the match
     * @param endedByUserId ID of user who ended the match
     * @param otherUserId ID of the other user
     * @return New MatchEndedEvent instance
     */
    public static MatchEndedEvent create(UUID matchId, UUID endedByUserId, UUID otherUserId) {
        MatchEndedEvent event = MatchEndedEvent.builder()
                .matchId(matchId)
                .endedByUserId(endedByUserId)
                .otherUserId(otherUserId)
                .endedAt(Instant.now())
                .build();
        event.initializeEvent("match-service", "MATCH_ENDED");
        return event;
    }
}
