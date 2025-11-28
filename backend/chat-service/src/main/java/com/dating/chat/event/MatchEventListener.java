package com.dating.chat.event;

import com.dating.common.config.RabbitMQConstants;
import com.dating.common.event.MatchCreatedEvent;
import com.dating.common.event.MatchEndedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens for match-related events from Match Service.
 * Creates/ends conversations based on match events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchEventListener {

    /**
     * Handle match created event.
     * Creates a conversation entry for the new match.
     *
     * @param event MatchCreatedEvent
     */
    @RabbitListener(queues = RabbitMQConstants.CHAT_MATCH_CREATED_QUEUE)
    public void handleMatchCreated(MatchCreatedEvent event) {
        log.info("Received MatchCreatedEvent: matchId={}, user1={}, user2={}",
                event.getMatchId(), event.getUser1Id(), event.getUser2Id());

        // In this implementation, conversations are implicit (matchId = conversationId)
        // No explicit conversation entity needed - messages reference the matchId directly

        // Could optionally:
        // - Pre-create a welcome message
        // - Initialize read receipts
        // - Cache conversation metadata

        log.info("Conversation ready for match {}", event.getMatchId());
    }

    /**
     * Handle match ended event.
     * Archives or deletes the conversation.
     *
     * @param event MatchEndedEvent
     */
    @RabbitListener(queues = RabbitMQConstants.CHAT_MATCH_ENDED_QUEUE)
    public void handleMatchEnded(MatchEndedEvent event) {
        log.info("Received MatchEndedEvent: matchId={}", event.getMatchId());

        // Could optionally:
        // - Archive messages
        // - Delete messages
        // - Clear cache

        log.info("Conversation ended for match {}", event.getMatchId());
    }
}
