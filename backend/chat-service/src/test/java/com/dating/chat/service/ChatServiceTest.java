package com.dating.chat.service;

import com.dating.chat.dto.request.SendMessageRequest;
import com.dating.chat.dto.response.ConversationResponse;
import com.dating.chat.dto.response.ConversationsListResponse;
import com.dating.chat.dto.response.MessageListResponse;
import com.dating.chat.dto.response.MessageResponse;
import com.dating.chat.dto.websocket.ChatMessage;
import com.dating.chat.dto.websocket.TypingEvent;
import com.dating.chat.dto.websocket.TypingIndicator;
import com.dating.chat.websocket.WebSocketSessionManager;
import com.dating.common.constant.MessageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatService.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private MessageService messageService;

    @Mock
    private ConversationService conversationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private WebSocketSessionManager sessionManager;

    @InjectMocks
    private ChatService chatService;

    private UUID senderId;
    private UUID receiverId;
    private UUID conversationId;
    private UUID messageId;
    private MessageResponse testMessageResponse;
    private MessageResponse otherUserMessageResponse;

    @BeforeEach
    void setUp() {
        senderId = UUID.randomUUID();
        receiverId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        messageId = UUID.randomUUID();

        testMessageResponse = MessageResponse.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(senderId)
                .content("Hello!")
                .status(MessageStatus.SENT)
                .createdAt(Instant.now())
                .build();

        // Message from the other user (to help determine participant)
        otherUserMessageResponse = MessageResponse.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .senderId(receiverId)
                .content("Hi there!")
                .status(MessageStatus.SENT)
                .createdAt(Instant.now().minusSeconds(60))
                .build();
    }

    @Test
    void testSendMessage_Success() {
        // Arrange
        SendMessageRequest request = new SendMessageRequest(conversationId, "Hello!");
        when(messageService.sendMessage(senderId, request)).thenReturn(testMessageResponse);
        // Return a message from the other user to determine receiver
        when(messageService.getMessages(conversationId, 50, 0))
                .thenReturn(List.of(testMessageResponse, otherUserMessageResponse));
        when(sessionManager.isUserOnline(receiverId)).thenReturn(false);
        when(sessionManager.isUserOnline(senderId)).thenReturn(false);

        // Act
        MessageResponse response = chatService.sendMessage(senderId, request);

        // Assert
        assertNotNull(response);
        assertEquals(messageId, response.getId());
        verify(messageService, times(1)).sendMessage(senderId, request);
    }

    @Test
    void testSendMessage_BroadcastsToOnlineReceiver() {
        // Arrange
        SendMessageRequest request = new SendMessageRequest(conversationId, "Hello!");
        when(messageService.sendMessage(senderId, request)).thenReturn(testMessageResponse);
        // Return a message from the other user to determine receiver
        when(messageService.getMessages(conversationId, 50, 0))
                .thenReturn(List.of(testMessageResponse, otherUserMessageResponse));
        when(sessionManager.isUserOnline(receiverId)).thenReturn(true);
        when(sessionManager.isUserOnline(senderId)).thenReturn(false);

        // Act
        chatService.sendMessage(senderId, request);

        // Assert
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq(receiverId.toString()),
                eq("/queue/messages"),
                any(ChatMessage.class)
        );
    }

    @Test
    void testSendWebSocketMessage_Success() {
        // Arrange
        ChatMessage chatMessage = ChatMessage.newMessage(conversationId, senderId, receiverId, "Hello!");
        when(messageService.sendMessage(eq(senderId), any(SendMessageRequest.class)))
                .thenReturn(testMessageResponse);
        // Return a message from the other user to determine receiver
        when(messageService.getMessages(conversationId, 50, 0))
                .thenReturn(List.of(testMessageResponse, otherUserMessageResponse));
        when(sessionManager.isUserOnline(any())).thenReturn(false);

        // Act
        MessageResponse response = chatService.sendWebSocketMessage(chatMessage);

        // Assert
        assertNotNull(response);
        assertEquals(messageId, response.getId());
    }

    @Test
    void testGetConversations_Success() {
        // Arrange
        List<ConversationResponse> conversations = List.of(
                ConversationResponse.builder().id(conversationId).build()
        );
        when(conversationService.getConversations(senderId, 20)).thenReturn(conversations);

        // Act
        ConversationsListResponse result = chatService.getConversations(senderId, 20);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getConversations().size());
        assertEquals(1, result.getTotal());
    }

    @Test
    void testGetMessages_Success() {
        // Arrange
        List<MessageResponse> messages = List.of(testMessageResponse);
        MessageListResponse expectedResponse = MessageListResponse.builder()
                .conversationId(conversationId)
                .messages(messages)
                .total(1)
                .limit(50)
                .offset(0)
                .hasMore(false)
                .build();
        when(messageService.getMessagesWithMetadata(conversationId, 50, 0)).thenReturn(expectedResponse);

        // Act
        MessageListResponse result = chatService.getMessages(conversationId, 50, 0);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getMessages().size());
        assertEquals(conversationId, result.getConversationId());
        assertEquals(1, result.getTotal());
        assertFalse(result.isHasMore());
    }

    @Test
    void testMarkAsRead_Success() {
        // Arrange
        when(messageService.markAllAsRead(conversationId, receiverId)).thenReturn(5);
        // Return messages to determine other participant
        when(messageService.getMessages(conversationId, 50, 0))
                .thenReturn(List.of(testMessageResponse, otherUserMessageResponse));
        when(sessionManager.isUserOnline(senderId)).thenReturn(false);

        // Act
        int count = chatService.markAsRead(conversationId, receiverId);

        // Assert
        assertEquals(5, count);
        verify(messageService, times(1)).markAllAsRead(conversationId, receiverId);
    }

    @Test
    void testMarkAsRead_BroadcastsToSender() {
        // Arrange
        when(messageService.markAllAsRead(conversationId, receiverId)).thenReturn(3);
        // Return messages to determine other participant
        when(messageService.getMessages(conversationId, 50, 0))
                .thenReturn(List.of(testMessageResponse, otherUserMessageResponse));
        when(sessionManager.isUserOnline(senderId)).thenReturn(true);

        // Act
        chatService.markAsRead(conversationId, receiverId);

        // Assert
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq(senderId.toString()),
                eq("/queue/messages"),
                any(ChatMessage.class)
        );
    }

    @Test
    void testMarkAsRead_NoBroadcastWhenNoMessages() {
        // Arrange
        when(messageService.markAllAsRead(conversationId, receiverId)).thenReturn(0);

        // Act
        chatService.markAsRead(conversationId, receiverId);

        // Assert
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any(ChatMessage.class));
    }

    @Test
    void testHandleTypingIndicator_BroadcastsToOtherParticipant() {
        // Arrange
        TypingIndicator indicator = new TypingIndicator(conversationId, true);
        // Return messages to determine other participant
        when(messageService.getMessages(conversationId, 50, 0))
                .thenReturn(List.of(testMessageResponse, otherUserMessageResponse));

        // Act
        chatService.handleTypingIndicator(indicator, senderId);

        // Assert
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq(receiverId.toString()),
                eq("/queue/typing"),
                any(TypingEvent.class)
        );
    }
}
