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
        Instant now = Instant.now();

        Message message = Message.builder()
                .id(messageId)
                .matchId(conversationId)
                .senderId(senderId)
                .content("Hello!")
                .status(MessageStatus.SENT)
                .createdAt(now)
                .readAt(null)
                .build();

        // Act
        MessageResponse response = mapper.toMessageResponse(message);

        // Assert
        assertNotNull(response);
        assertEquals(messageId, response.getId());
        assertEquals(conversationId, response.getConversationId());
        assertEquals(senderId, response.getSenderId());
        assertEquals("Hello!", response.getContent());
        assertEquals(MessageStatus.SENT, response.getStatus());
        assertEquals(now, response.getCreatedAt());
        assertNull(response.getReadAt());
    }

    @Test
    void testToMessageResponse_WithRead() {
        // Arrange
        Instant createdAt = Instant.now().minusSeconds(3600);
        Instant readAt = Instant.now();

        Message message = Message.builder()
                .id(UUID.randomUUID())
                .matchId(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .content("Test")
                .status(MessageStatus.READ)
                .createdAt(createdAt)
                .readAt(readAt)
                .build();

        // Act
        MessageResponse response = mapper.toMessageResponse(message);

        // Assert
        assertEquals(MessageStatus.READ, response.getStatus());
        assertEquals(createdAt, response.getCreatedAt());
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
