package com.dating.chat.dto.websocket;

import com.dating.common.constant.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * WebSocket message DTO for real-time chat.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    /**
     * Message type for WebSocket handling.
     */
    public enum MessageType {
        SEND_MESSAGE,
        MESSAGE_RECEIVED,
        MESSAGE_DELIVERED,
        MESSAGE_READ,
        MARK_AS_READ,
        ERROR
    }

    /**
     * Type of message.
     */
    private MessageType type;

    /**
     * Message ID (for existing messages).
     */
    private UUID messageId;

    /**
     * Conversation/match ID.
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
     * Message status.
     */
    private MessageStatus status;

    /**
     * Timestamp.
     */
    private Instant timestamp;

    /**
     * Error message (for ERROR type).
     */
    private String error;

    /**
     * Create a new outgoing message.
     */
    public static ChatMessage newMessage(UUID conversationId, UUID senderId,
                                         UUID receiverId, String content) {
        return ChatMessage.builder()
                .type(MessageType.SEND_MESSAGE)
                .conversationId(conversationId)
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a message received notification.
     */
    public static ChatMessage messageReceived(UUID messageId, UUID conversationId,
                                              UUID senderId, UUID receiverId,
                                              String content, MessageStatus status) {
        return ChatMessage.builder()
                .type(MessageType.MESSAGE_RECEIVED)
                .messageId(messageId)
                .conversationId(conversationId)
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .status(status)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a message status update.
     */
    public static ChatMessage statusUpdate(MessageType type, UUID messageId,
                                           UUID conversationId, MessageStatus status) {
        return ChatMessage.builder()
                .type(type)
                .messageId(messageId)
                .conversationId(conversationId)
                .status(status)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an error message.
     */
    public static ChatMessage error(String errorMessage) {
        return ChatMessage.builder()
                .type(MessageType.ERROR)
                .error(errorMessage)
                .timestamp(Instant.now())
                .build();
    }
}
