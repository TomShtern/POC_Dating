package com.dating.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a message is sent in a conversation.
 * Consumed by Notification Service to send push notifications
 * and update unread message counts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MessageSentEvent extends BaseEvent {

    private static final long serialVersionUID = 1L;

    /**
     * ID of the message.
     */
    private UUID messageId;

    /**
     * ID of the match/conversation.
     */
    private UUID matchId;

    /**
     * ID of the sender.
     */
    private UUID senderId;

    /**
     * ID of the receiver.
     */
    private UUID receiverId;

    /**
     * Sender's first name (for notifications).
     */
    private String senderFirstName;

    /**
     * Sender's photo URL (for notifications).
     */
    private String senderPhotoUrl;

    /**
     * Message content (may be truncated for notifications).
     */
    private String content;

    /**
     * When the message was sent.
     */
    private Instant sentAt;

    /**
     * Create a new MessageSentEvent with default event metadata.
     *
     * @param messageId ID of the message
     * @param matchId ID of the match/conversation
     * @param senderId ID of the sender
     * @param receiverId ID of the receiver
     * @param content Message content
     * @return New MessageSentEvent instance
     */
    public static MessageSentEvent create(
            UUID messageId,
            UUID matchId,
            UUID senderId,
            UUID receiverId,
            String content) {
        MessageSentEvent event = MessageSentEvent.builder()
                .messageId(messageId)
                .matchId(matchId)
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .sentAt(Instant.now())
                .build();
        event.initializeEvent("chat-service", "MESSAGE_SENT");
        return event;
    }
}
