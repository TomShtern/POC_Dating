package com.dating.chat.listener;

import com.dating.chat.model.Conversation;
import com.dating.chat.repository.ConversationRepository;
import com.dating.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * RabbitMQ Event Listener for Match events.
 *
 * Listens for:
 * - match.created - Creates a new conversation
 * - match.ended - Archives the conversation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchEventListener {

    private final ConversationRepository conversationRepository;
    private final ChatMessageService chatMessageService;

    /**
     * Handle match created event.
     * Creates a conversation for the new match.
     */
    @RabbitListener(queues = "${app.rabbitmq.queues.match-created:chat.match.created.queue}")
    @Transactional
    public void handleMatchCreated(MatchCreatedEvent event) {
        log.info("Received match created event: matchId={}, user1={}, user2={}",
                event.matchId(), event.user1Id(), event.user2Id());

        // Check if conversation already exists
        if (conversationRepository.findByMatchId(event.matchId()).isPresent()) {
            log.warn("Conversation already exists for match: {}", event.matchId());
            return;
        }

        // Create conversation
        Conversation conversation = chatMessageService.getOrCreateConversation(
                event.matchId(),
                event.user1Id(),
                event.user2Id()
        );

        log.info("Created conversation: id={} for match: {}", conversation.getId(), event.matchId());
    }

    /**
     * Handle match ended event.
     * Archives the conversation.
     */
    @RabbitListener(queues = "${app.rabbitmq.queues.match-ended:chat.match.ended.queue}")
    @Transactional
    public void handleMatchEnded(MatchEndedEvent event) {
        log.info("Received match ended event: matchId={}", event.matchId());

        conversationRepository.findByMatchId(event.matchId())
                .ifPresentOrElse(
                        conversation -> {
                            conversation.setArchivedAt(Instant.now());
                            conversationRepository.save(conversation);
                            log.info("Archived conversation: id={} for match: {}",
                                    conversation.getId(), event.matchId());
                        },
                        () -> log.warn("Conversation not found for match: {}", event.matchId())
                );
    }

    /**
     * Event record for match created.
     */
    public record MatchCreatedEvent(
            UUID matchId,
            UUID user1Id,
            UUID user2Id,
            String matchedAt
    ) {}

    /**
     * Event record for match ended.
     */
    public record MatchEndedEvent(
            UUID matchId,
            String endedAt,
            String reason
    ) {}
}
