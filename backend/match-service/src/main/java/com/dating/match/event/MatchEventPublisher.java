package com.dating.match.event;

import com.dating.common.config.RabbitMQConstants;
import com.dating.common.event.MatchCreatedEvent;
import com.dating.common.event.MatchEndedEvent;
import com.dating.match.model.Match;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Event publisher for match-related events.
 * Publishes events to RabbitMQ for other services to consume.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish match created event.
     *
     * @param match Created match
     * @param user1Name First user's name
     * @param user2Name Second user's name
     */
    public void publishMatchCreated(Match match, String user1Name, String user2Name) {
        MatchCreatedEvent event = MatchCreatedEvent.builder()
                .matchId(match.getId())
                .user1Id(match.getUser1Id())
                .user2Id(match.getUser2Id())
                .user1FirstName(user1Name)
                .user2FirstName(user2Name)
                .matchedAt(match.getMatchedAt())
                .build();
        event.initializeEvent("match-service", "MATCH_CREATED");

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.MATCH_EXCHANGE,
                    RabbitMQConstants.MATCH_CREATED_KEY,
                    event);
            log.info("Published MATCH_CREATED event for match: {}", match.getId());
        } catch (Exception ex) {
            log.error("Failed to publish MATCH_CREATED event for match: {}", match.getId(), ex);
        }
    }

    /**
     * Publish match ended event.
     *
     * @param match Ended match
     * @param endedByUserId User who ended the match
     */
    public void publishMatchEnded(Match match, UUID endedByUserId) {
        UUID otherUserId = match.getOtherUserId(endedByUserId);

        MatchEndedEvent event = MatchEndedEvent.builder()
                .matchId(match.getId())
                .endedByUserId(endedByUserId)
                .otherUserId(otherUserId)
                .endedAt(match.getEndedAt())
                .build();
        event.initializeEvent("match-service", "MATCH_ENDED");

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.MATCH_EXCHANGE,
                    RabbitMQConstants.MATCH_ENDED_KEY,
                    event);
            log.info("Published MATCH_ENDED event for match: {}", match.getId());
        } catch (Exception ex) {
            log.error("Failed to publish MATCH_ENDED event for match: {}", match.getId(), ex);
        }
    }
}
