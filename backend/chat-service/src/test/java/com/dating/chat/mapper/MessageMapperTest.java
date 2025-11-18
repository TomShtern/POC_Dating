package com.dating.chat.mapper;

import com.dating.chat.dto.response.MessageResponse;
import com.dating.chat.model.Message;
import com.dating.common.constant.MessageStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MessageMapper.
 */
class MessageMapperTest {

    private final MessageMapper mapper = new MessageMapper();

    @Test
    void testToMessageResponse_Success() {
        // Arrange
        UUID messageId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        Instant now = Instant.now();

        Message message = Message.builder()
                .id(messageId)
                .matchId(conversationId)
                .senderId(senderId)
                .receiverId(receiverId)
                .content("Hello!")
                .status(MessageStatus.SENT)
                .createdAt(now)
                .deliveredAt(null)
                .readAt(null)
                .build();

        // Act
        MessageResponse response = mapper.toMessageResponse(message);

        // Assert
        assertNotNull(response);
        assertEquals(messageId, response.getId());
        assertEquals(conversationId, response.getConversationId());
        assertEquals(senderId, response.getSenderId());
        assertEquals(receiverId, response.getReceiverId());
        assertEquals("Hello!", response.getContent());
        assertEquals(MessageStatus.SENT, response.getStatus());
        assertEquals(now, response.getSentAt());
        assertNull(response.getDeliveredAt());
        assertNull(response.getReadAt());
    }

    @Test
    void testToMessageResponse_WithDeliveredAndRead() {
        // Arrange
        Instant sentAt = Instant.now().minusSeconds(3600);
        Instant deliveredAt = Instant.now().minusSeconds(1800);
        Instant readAt = Instant.now();

        Message message = Message.builder()
                .id(UUID.randomUUID())
                .matchId(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .receiverId(UUID.randomUUID())
                .content("Test")
                .status(MessageStatus.READ)
                .createdAt(sentAt)
                .deliveredAt(deliveredAt)
                .readAt(readAt)
                .build();

        // Act
        MessageResponse response = mapper.toMessageResponse(message);

        // Assert
        assertEquals(MessageStatus.READ, response.getStatus());
        assertEquals(sentAt, response.getSentAt());
        assertEquals(deliveredAt, response.getDeliveredAt());
        assertEquals(readAt, response.getReadAt());
    }

    @Test
    void testToMessageResponse_NullInput() {
        // Act
        MessageResponse response = mapper.toMessageResponse(null);

        // Assert
        assertNull(response);
    }
}
