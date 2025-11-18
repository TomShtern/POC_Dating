package com.dating.chat.dto.response;

import com.dating.common.constant.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a message.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    /**
     * Message ID.
     */
    private UUID id;

    /**
     * Match/conversation ID.
     */
    private UUID conversationId;

    /**
     * Sender user ID.
     */
    private UUID senderId;

    /**
     * Receiver user ID.
     */
    private UUID receiverId;

    /**
     * Message content.
     */
    private String content;

    /**
     * Message delivery status.
     */
    private MessageStatus status;

    /**
     * When the message was sent.
     */
    private Instant sentAt;

    /**
     * When the message was delivered.
     */
    private Instant deliveredAt;

    /**
     * When the message was read.
     */
    private Instant readAt;
}
