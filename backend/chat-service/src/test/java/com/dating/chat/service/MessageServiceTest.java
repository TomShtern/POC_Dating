package com.dating.chat.service;

import com.dating.chat.dto.request.SendMessageRequest;
import com.dating.chat.dto.response.MessageListResponse;
import com.dating.chat.dto.response.MessageResponse;
import com.dating.chat.event.ChatEventPublisher;
import com.dating.chat.mapper.MessageMapper;
import com.dating.chat.model.Message;
import com.dating.chat.repository.MessageRepository;
import com.dating.common.constant.MessageStatus;
import com.dating.common.exception.MessageNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MessageService.
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private ChatEventPublisher eventPublisher;

    @InjectMocks
    private MessageService messageService;

    private UUID senderId;
    private UUID otherUserId;
    private UUID conversationId;
    private UUID messageId;
    private Message testMessage;
    private MessageResponse testMessageResponse;

    @BeforeEach
    void setUp() {
        senderId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        messageId = UUID.randomUUID();

        testMessage = Message.builder()
                .id(messageId)
                .matchId(conversationId)
                .senderId(senderId)
                .content("Hello!")
                .status(MessageStatus.SENT)
                .createdAt(Instant.now())
                .build();

        testMessageResponse = MessageResponse.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(senderId)
                .content("Hello!")
                .status(MessageStatus.SENT)
                .sentAt(Instant.now())
                .build();
    }

    @Test
    void testSendMessage_Success() {
        // Arrange
        SendMessageRequest request = new SendMessageRequest(conversationId, "Hello!");
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);
        when(messageMapper.toMessageResponse(testMessage)).thenReturn(testMessageResponse);

        // Act
        MessageResponse response = messageService.sendMessage(senderId, request);

        // Assert
        assertNotNull(response);
        assertEquals(messageId, response.getId());
        assertEquals(conversationId, response.getConversationId());
        assertEquals(senderId, response.getSenderId());
        assertEquals("Hello!", response.getContent());
        assertEquals(MessageStatus.SENT, response.getStatus());

        // Verify interactions
        verify(messageRepository, times(1)).save(any(Message.class));
        verify(eventPublisher, times(1)).publishMessageSent(testMessage);
    }

    @Test
    void testSendMessage_VerifiesMessageContent() {
        // Arrange
        SendMessageRequest request = new SendMessageRequest(conversationId, "Test message");
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        when(messageRepository.save(messageCaptor.capture())).thenReturn(testMessage);
        when(messageMapper.toMessageResponse(any())).thenReturn(testMessageResponse);

        // Act
        messageService.sendMessage(senderId, request);

        // Assert
        Message savedMessage = messageCaptor.getValue();
        assertEquals(conversationId, savedMessage.getMatchId());
        assertEquals(senderId, savedMessage.getSenderId());
        assertEquals("Test message", savedMessage.getContent());
    }

    @Test
    void testGetMessages_Success() {
        // Arrange
        List<Message> messages = List.of(testMessage);
        Page<Message> page = new PageImpl<>(messages);
        when(messageRepository.findByMatchIdOrderByCreatedAtDesc(eq(conversationId), any(PageRequest.class)))
                .thenReturn(page);
        when(messageMapper.toMessageResponse(testMessage)).thenReturn(testMessageResponse);

        // Act
        List<MessageResponse> responses = messageService.getMessages(conversationId, 50, 0);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(messageId, responses.get(0).getId());
    }

    @Test
    void testGetMessagesWithMetadata_Success() {
        // Arrange
        List<Message> messages = List.of(testMessage);
        Page<Message> page = new PageImpl<>(messages);
        when(messageRepository.findByMatchIdOrderByCreatedAtDesc(eq(conversationId), any(PageRequest.class)))
                .thenReturn(page);
        when(messageMapper.toMessageResponse(testMessage)).thenReturn(testMessageResponse);
        when(messageRepository.countByMatchId(conversationId)).thenReturn(10L);

        // Act
        MessageListResponse response = messageService.getMessagesWithMetadata(conversationId, 50, 0);

        // Assert
        assertNotNull(response);
        assertEquals(conversationId, response.getConversationId());
        assertEquals(1, response.getMessages().size());
        assertEquals(10, response.getTotal());
        assertTrue(response.isHasMore());
    }

    @Test
    void testGetMessagesWithMetadata_NoMoreMessages() {
        // Arrange
        List<Message> messages = List.of(testMessage);
        Page<Message> page = new PageImpl<>(messages);
        when(messageRepository.findByMatchIdOrderByCreatedAtDesc(eq(conversationId), any(PageRequest.class)))
                .thenReturn(page);
        when(messageMapper.toMessageResponse(testMessage)).thenReturn(testMessageResponse);
        when(messageRepository.countByMatchId(conversationId)).thenReturn(1L);

        // Act
        MessageListResponse response = messageService.getMessagesWithMetadata(conversationId, 50, 0);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotal());
        assertFalse(response.isHasMore());
    }

    @Test
    void testGetMessageById_Success() {
        // Arrange
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
        when(messageMapper.toMessageResponse(testMessage)).thenReturn(testMessageResponse);

        // Act
        MessageResponse response = messageService.getMessageById(messageId);

        // Assert
        assertNotNull(response);
        assertEquals(messageId, response.getId());
    }

    @Test
    void testGetMessageById_NotFound() {
        // Arrange
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(MessageNotFoundException.class, () -> {
            messageService.getMessageById(messageId);
        });
    }

    @Test
    void testMarkAllAsRead_Success() {
        // Arrange
        when(messageRepository.markAllAsRead(eq(conversationId), eq(otherUserId), any(Instant.class)))
                .thenReturn(5);

        // Act
        int count = messageService.markAllAsRead(conversationId, otherUserId);

        // Assert
        assertEquals(5, count);
        verify(eventPublisher, times(1)).publishMessagesRead(conversationId, otherUserId, 5);
    }

    @Test
    void testMarkAllAsRead_NoMessages() {
        // Arrange
        when(messageRepository.markAllAsRead(eq(conversationId), eq(otherUserId), any(Instant.class)))
                .thenReturn(0);

        // Act
        int count = messageService.markAllAsRead(conversationId, otherUserId);

        // Assert
        assertEquals(0, count);
        verify(eventPublisher, never()).publishMessagesRead(any(), any(), anyInt());
    }

    @Test
    void testCountUnread_Success() {
        // Arrange
        when(messageRepository.countUnreadByMatchIdAndUserId(conversationId, otherUserId))
                .thenReturn(3L);

        // Act
        long count = messageService.countUnread(conversationId, otherUserId);

        // Assert
        assertEquals(3L, count);
    }

    @Test
    void testCountTotalUnread_Success() {
        // Arrange
        List<UUID> matchIds = List.of(conversationId);
        when(messageRepository.findMatchIdsByUserId(otherUserId)).thenReturn(matchIds);
        when(messageRepository.countUnreadByUserIdAndMatchIds(otherUserId, matchIds)).thenReturn(10L);

        // Act
        long count = messageService.countTotalUnread(otherUserId);

        // Assert
        assertEquals(10L, count);
    }

    @Test
    void testGetLastMessage_Success() {
        // Arrange
        when(messageRepository.findLastMessageByMatchId(conversationId)).thenReturn(testMessage);
        when(messageMapper.toMessageResponse(testMessage)).thenReturn(testMessageResponse);

        // Act
        MessageResponse response = messageService.getLastMessage(conversationId);

        // Assert
        assertNotNull(response);
        assertEquals(messageId, response.getId());
    }

    @Test
    void testGetLastMessage_NoMessages() {
        // Arrange
        when(messageRepository.findLastMessageByMatchId(conversationId)).thenReturn(null);

        // Act
        MessageResponse response = messageService.getLastMessage(conversationId);

        // Assert
        assertNull(response);
    }

    @Test
    void testGetMessagesSince_Success() {
        // Arrange
        Instant since = Instant.now().minusSeconds(3600);
        List<Message> messages = List.of(testMessage);
        when(messageRepository.findByMatchIdAndCreatedAtAfter(conversationId, since))
                .thenReturn(messages);
        when(messageMapper.toMessageResponse(testMessage)).thenReturn(testMessageResponse);

        // Act
        List<MessageResponse> responses = messageService.getMessagesSince(conversationId, since);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
    }
}
