package com.dating.chat.listener;

import com.dating.chat.model.Conversation;
import com.dating.chat.repository.ConversationRepository;
import com.dating.chat.service.ChatMessageService;
import com.dating.common.config.RabbitMQConstants;
import com.dating.common.event.MatchCreatedEvent;
import com.dating.common.event.MatchEndedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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
    @RabbitListener(queues = RabbitMQConstants.CHAT_MATCH_CREATED_QUEUE)
    @Transactional
    public void handleMatchCreated(MatchCreatedEvent event) {
        log.info("Received match created event: matchId={}, user1={}, user2={}",
                event.getMatchId(), event.getUser1Id(), event.getUser2Id());

        // Check if conversation already exists
        if (conversationRepository.findByMatchId(event.getMatchId()).isPresent()) {
            log.warn("Conversation already exists for match: {}", event.getMatchId());
            return;
        }

        // Create conversation
        Conversation conversation = chatMessageService.getOrCreateConversation(
                event.getMatchId(),
                event.getUser1Id(),
                event.getUser2Id()
        );

        log.info("Created conversation: id={} for match: {}", conversation.getId(), event.getMatchId());
    }

    /**
     * Handle match ended event.
     * Archives the conversation.
     */
    @RabbitListener(queues = RabbitMQConstants.CHAT_MATCH_ENDED_QUEUE)
    @Transactional
    public void handleMatchEnded(MatchEndedEvent event) {
        log.info("Received match ended event: matchId={}", event.getMatchId());

        conversationRepository.findByMatchId(event.getMatchId())
                .ifPresentOrElse(
                        conversation -> {
                            conversation.setArchivedAt(Instant.now());
                            conversationRepository.save(conversation);
                            log.info("Archived conversation: id={} for match: {}",
                                    conversation.getId(), event.getMatchId());
                        },
                        () -> log.warn("Conversation not found for match: {}", event.getMatchId())
                );
    }
}
