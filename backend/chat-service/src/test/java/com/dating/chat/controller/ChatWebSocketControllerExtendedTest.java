package com.dating.chat.controller;

import com.dating.chat.dto.websocket.ChatMessageEvent;
import com.dating.chat.dto.websocket.MessageType;
import com.dating.chat.dto.websocket.SendMessageRequest;
import com.dating.chat.security.StompPrincipal;
import com.dating.chat.service.ChatMessageService;
import com.dating.chat.service.IdempotencyService;
import com.dating.chat.service.PresenceService;
import com.dating.chat.service.PushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerExtendedTest {

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private PresenceService presenceService;

    @Mock
    private PushNotificationService pushNotificationService;

    private ChatWebSocketController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatWebSocketController(
                chatMessageService,
                messagingTemplate,
                idempotencyService,
                presenceService,
                pushNotificationService
        );
    }

    @Test
    void sendMessage_WithIdempotencyKey_ChecksDuplicates() {
        UUID userId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        StompPrincipal principal = new StompPrincipal(userId.toString(), "john@test.com", "John");

        SendMessageRequest request = new SendMessageRequest(
                matchId, "Hello", MessageType.TEXT, "idempotency-key-123", null
        );

        when(idempotencyService.checkAndSet(userId.toString(), "idempotency-key-123")).thenReturn(true);
        when(chatMessageService.isUserInMatch(userId, matchId)).thenReturn(true);
        when(chatMessageService.getOtherUserId(matchId, userId)).thenReturn(recipientId);
        when(chatMessageService.saveMessage(eq(matchId), eq(userId), eq("John"), eq("Hello"), eq(MessageType.TEXT)))
                .thenReturn(createTestMessageEvent(matchId, userId, recipientId));
        when(presenceService.isOnline(recipientId.toString())).thenReturn(true);

        controller.sendMessage(request, principal);

        verify(idempotencyService).checkAndSet(userId.toString(), "idempotency-key-123");
        verify(chatMessageService).saveMessage(eq(matchId), eq(userId), eq("John"), eq("Hello"), eq(MessageType.TEXT));
    }

    @Test
    void sendMessage_DuplicateIdempotencyKey_IgnoresMessage() {
        UUID userId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        StompPrincipal principal = new StompPrincipal(userId.toString(), "john@test.com", "John");

        SendMessageRequest request = new SendMessageRequest(
                matchId, "Hello", MessageType.TEXT, "duplicate-key", null
        );

        when(idempotencyService.checkAndSet(userId.toString(), "duplicate-key")).thenReturn(false);

        controller.sendMessage(request, principal);

        verify(chatMessageService, never()).saveMessage(any(), any(), any(), any(), any());
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void sendMessage_RecipientOnline_MarksAsDelivered() {
        UUID userId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        StompPrincipal principal = new StompPrincipal(userId.toString(), "john@test.com", "John");

        SendMessageRequest request = new SendMessageRequest(
                matchId, "Hello", MessageType.TEXT, null, null
        );

        ChatMessageEvent savedMessage = new ChatMessageEvent(
                messageId, matchId, userId, "John", "Hello", MessageType.TEXT, Instant.now()
        );

        when(chatMessageService.isUserInMatch(userId, matchId)).thenReturn(true);
        when(chatMessageService.getOtherUserId(matchId, userId)).thenReturn(recipientId);
        when(chatMessageService.saveMessage(any(), any(), any(), any(), any())).thenReturn(savedMessage);
        when(presenceService.isOnline(recipientId.toString())).thenReturn(true);

        controller.sendMessage(request, principal);

        verify(chatMessageService).markMessageAsDelivered(messageId);
        verify(pushNotificationService, never()).sendMessageNotification(any(), any());
    }

    @Test
    void sendMessage_RecipientOffline_SendsPushNotification() {
        UUID userId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        StompPrincipal principal = new StompPrincipal(userId.toString(), "john@test.com", "John");

        SendMessageRequest request = new SendMessageRequest(
                matchId, "Hello", MessageType.TEXT, null, null
        );

        ChatMessageEvent savedMessage = new ChatMessageEvent(
                messageId, matchId, userId, "John", "Hello", MessageType.TEXT, Instant.now()
        );

        when(chatMessageService.isUserInMatch(userId, matchId)).thenReturn(true);
        when(chatMessageService.getOtherUserId(matchId, userId)).thenReturn(recipientId);
        when(chatMessageService.saveMessage(any(), any(), any(), any(), any())).thenReturn(savedMessage);
        when(presenceService.isOnline(recipientId.toString())).thenReturn(false);

        controller.sendMessage(request, principal);

        verify(chatMessageService, never()).markMessageAsDelivered(any());
        verify(pushNotificationService).sendMessageNotification(recipientId, savedMessage);
    }

    @Test
    void sendMessage_WithoutIdempotencyKey_SkipsCheck() {
        UUID userId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        StompPrincipal principal = new StompPrincipal(userId.toString(), "john@test.com", "John");

        SendMessageRequest request = new SendMessageRequest(
                matchId, "Hello", MessageType.TEXT, null, null
        );

        when(chatMessageService.isUserInMatch(userId, matchId)).thenReturn(true);
        when(chatMessageService.getOtherUserId(matchId, userId)).thenReturn(recipientId);
        when(chatMessageService.saveMessage(any(), any(), any(), any(), any()))
                .thenReturn(createTestMessageEvent(matchId, userId, recipientId));
        when(presenceService.isOnline(recipientId.toString())).thenReturn(true);

        controller.sendMessage(request, principal);

        verify(idempotencyService, never()).checkAndSet(anyString(), anyString());
    }

    @Test
    void sendMessage_WithEmptyIdempotencyKey_SkipsCheck() {
        UUID userId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        StompPrincipal principal = new StompPrincipal(userId.toString(), "john@test.com", "John");

        SendMessageRequest request = new SendMessageRequest(
                matchId, "Hello", MessageType.TEXT, "", null
        );

        when(chatMessageService.isUserInMatch(userId, matchId)).thenReturn(true);
        when(chatMessageService.getOtherUserId(matchId, userId)).thenReturn(recipientId);
        when(chatMessageService.saveMessage(any(), any(), any(), any(), any()))
                .thenReturn(createTestMessageEvent(matchId, userId, recipientId));
        when(presenceService.isOnline(recipientId.toString())).thenReturn(true);

        controller.sendMessage(request, principal);

        verify(idempotencyService, never()).checkAndSet(anyString(), anyString());
    }

    private ChatMessageEvent createTestMessageEvent(UUID matchId, UUID senderId, UUID recipientId) {
        return new ChatMessageEvent(
                UUID.randomUUID(),
                matchId,
                senderId,
                "John",
                "Hello",
                MessageType.TEXT,
                Instant.now()
        );
    }
}
