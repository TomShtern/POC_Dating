package com.dating.chat.controller;

import com.dating.chat.dto.websocket.ChatMessageEvent;
import com.dating.chat.dto.websocket.MarkReadRequest;
import com.dating.chat.dto.websocket.MessageType;
import com.dating.chat.dto.websocket.SendMessageRequest;
import com.dating.chat.dto.websocket.TypingIndicator;
import com.dating.chat.security.StompPrincipal;
import com.dating.chat.service.ChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatWebSocketController controller;

    private UUID userId;
    private UUID otherUserId;
    private UUID matchId;
    private StompPrincipal principal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        matchId = UUID.randomUUID();
        principal = new StompPrincipal(userId.toString(), "testuser");
    }

    @Test
    void sendMessage_shouldSaveAndBroadcastMessage() {
        SendMessageRequest request = new SendMessageRequest(matchId, "Hello!", MessageType.TEXT, null, null);

        when(chatMessageService.isUserInMatch(userId, matchId)).thenReturn(true);
        when(chatMessageService.saveMessage(any(), any(), any(), any(), any()))
                .thenReturn(new ChatMessageEvent(
                        UUID.randomUUID(),
                        matchId,
                        userId,
                        "testuser",
                        "Hello!",
                        MessageType.TEXT,
                        Instant.now()
                ));
        when(chatMessageService.getOtherUserId(matchId, userId)).thenReturn(otherUserId);

        controller.sendMessage(request, principal);

        verify(chatMessageService).saveMessage(eq(matchId), eq(userId), eq("testuser"), eq("Hello!"), eq(MessageType.TEXT));
        verify(messagingTemplate).convertAndSendToUser(eq(otherUserId.toString()), eq("/queue/messages"), any());
        verify(messagingTemplate).convertAndSendToUser(eq(userId.toString()), eq("/queue/delivered"), any());
    }

    @Test
    void sendMessage_shouldThrowExceptionWhenNotAuthorized() {
        SendMessageRequest request = new SendMessageRequest(matchId, "Hello!", MessageType.TEXT, null, null);
        when(chatMessageService.isUserInMatch(userId, matchId)).thenReturn(false);

        assertThatThrownBy(() -> controller.sendMessage(request, principal))
                .isInstanceOf(AccessDeniedException.class);

        verify(chatMessageService, never()).saveMessage(any(), any(), any(), any(), any());
    }

    @Test
    void sendMessage_shouldThrowExceptionWhenPrincipalIsNull() {
        SendMessageRequest request = new SendMessageRequest(matchId, "Hello!", MessageType.TEXT, null, null);

        assertThatThrownBy(() -> controller.sendMessage(request, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Authentication required");
    }

    @Test
    void typing_shouldBroadcastTypingIndicator() {
        TypingIndicator indicator = new TypingIndicator(matchId, true);

        when(chatMessageService.isUserInMatch(userId, matchId)).thenReturn(true);
        when(chatMessageService.getOtherUserId(matchId, userId)).thenReturn(otherUserId);

        controller.typing(indicator, principal);

        verify(messagingTemplate).convertAndSendToUser(eq(otherUserId.toString()), eq("/queue/typing"), any());
    }

    @Test
    void typing_shouldSilentlyIgnoreWhenNotAuthorized() {
        TypingIndicator indicator = new TypingIndicator(matchId, true);
        when(chatMessageService.isUserInMatch(userId, matchId)).thenReturn(false);

        controller.typing(indicator, principal);

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void markRead_shouldUpdateAndNotify() {
        UUID lastReadMessageId = UUID.randomUUID();
        MarkReadRequest request = new MarkReadRequest(matchId, lastReadMessageId);

        when(chatMessageService.isUserInMatch(userId, matchId)).thenReturn(true);
        when(chatMessageService.getOtherUserId(matchId, userId)).thenReturn(otherUserId);

        controller.markRead(request, principal);

        verify(chatMessageService).markMessagesAsRead(matchId, userId, lastReadMessageId);
        verify(messagingTemplate).convertAndSendToUser(eq(otherUserId.toString()), eq("/queue/read"), any());
    }

    @Test
    void markRead_shouldThrowExceptionWhenNotAuthorized() {
        UUID lastReadMessageId = UUID.randomUUID();
        MarkReadRequest request = new MarkReadRequest(matchId, lastReadMessageId);
        when(chatMessageService.isUserInMatch(userId, matchId)).thenReturn(false);

        assertThatThrownBy(() -> controller.markRead(request, principal))
                .isInstanceOf(AccessDeniedException.class);

        verify(chatMessageService, never()).markMessagesAsRead(any(), any(), any());
    }
}
