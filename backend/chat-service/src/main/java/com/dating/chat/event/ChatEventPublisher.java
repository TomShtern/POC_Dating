package com.dating.chat.event;

import com.dating.chat.model.Message;
import com.dating.common.config.RabbitMQConstants;
import com.dating.common.event.MessageReadEvent;
import com.dating.common.event.MessageSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Publishes chat-related events to RabbitMQ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish event when a message is sent.
     * Note: The receiverId is not stored in Message entity.
     * TODO: For proper notifications, look up match participants to get receiverId.
     *
     * @param message The sent message
     */
    public void publishMessageSent(Message message) {
        // Note: receiverId is null because Message entity doesn't store it
        // The receiver is implicitly the other participant in the match
        // Notification service would need to look up match participants
        MessageSentEvent event = MessageSentEvent.create(
                message.getId(),
                message.getMatchId(),
                message.getSenderId(),
                null, // receiverId - not stored in Message, derive from match
                message.getContent()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.CHAT_EXCHANGE,
                RabbitMQConstants.MESSAGE_SENT_KEY,
                event
        );

        log.info("Published MessageSentEvent for message {} in conversation {}",
                message.getId(), message.getMatchId());
    }

    /**
     * Publish event when messages are marked as read.
     *
     * @param conversationId Conversation ID
     * @param readerId User who read the messages
     * @param count Number of messages marked as read
     */
    public void publishMessagesRead(UUID conversationId, UUID readerId, int count) {
        // Note: In production, we would collect the actual message IDs
        // For this POC, we create an event with empty message list
        MessageReadEvent event = MessageReadEvent.builder()
                .matchId(conversationId)
                .readByUserId(readerId)
                .messageIds(List.of())
                .build();
        event.initializeEvent("chat-service", "MESSAGE_READ");

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.CHAT_EXCHANGE,
                RabbitMQConstants.MESSAGE_READ_KEY,
                event
        );

        log.info("Published MessageReadEvent for {} messages in conversation {} by user {}",
                count, conversationId, readerId);
    }
}
