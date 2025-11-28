package com.dating.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event published when messages are marked as read.
 * Consumed to update read receipts and message status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MessageReadEvent extends BaseEvent {

    private static final long serialVersionUID = 1L;

    /**
     * ID of the match/conversation.
     */
    private UUID matchId;

    /**
     * ID of the user who read the messages.
     */
    private UUID readByUserId;

    /**
     * List of message IDs that were read.
     */
    private List<UUID> messageIds;

    /**
     * When the messages were read.
     */
    private Instant readAt;

    /**
     * Create a new MessageReadEvent with default event metadata.
     *
     * @param matchId ID of the match/conversation
     * @param readByUserId ID of user who read messages
     * @param messageIds List of read message IDs
     * @return New MessageReadEvent instance
     */
    public static MessageReadEvent create(UUID matchId, UUID readByUserId, List<UUID> messageIds) {
        MessageReadEvent event = MessageReadEvent.builder()
                .matchId(matchId)
                .readByUserId(readByUserId)
                .messageIds(messageIds)
                .readAt(Instant.now())
                .build();
        event.initializeEvent("chat-service", "MESSAGE_READ");
        return event;
    }
}
