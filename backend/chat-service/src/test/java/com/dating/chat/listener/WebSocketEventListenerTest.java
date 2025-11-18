package com.dating.chat.listener;

import com.dating.chat.security.StompPrincipal;
import com.dating.chat.service.PresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private PresenceService presenceService;

    private WebSocketEventListener eventListener;

    @BeforeEach
    void setUp() {
        eventListener = new WebSocketEventListener(messagingTemplate, presenceService);
    }

    @Test
    void handleConnect_WithStompPrincipal_SetsUserOnline() {
        StompPrincipal principal = new StompPrincipal("user123", "john@test.com", "John");
        Message<byte[]> message = createMessage(StompCommand.CONNECT, "session123", principal);
        SessionConnectEvent event = new SessionConnectEvent(this, message);

        when(presenceService.setOnline("user123", "session123", "John")).thenReturn(true);

        eventListener.handleConnect(event);

        verify(presenceService).setOnline("user123", "session123", "John");
    }

    @Test
    void handleConnect_WithRegularPrincipal_UsesName() {
        TestPrincipal principal = new TestPrincipal("user456");
        Message<byte[]> message = createMessage(StompCommand.CONNECT, "session456", principal);
        SessionConnectEvent event = new SessionConnectEvent(this, message);

        when(presenceService.setOnline("user456", "session456", "unknown")).thenReturn(false);

        eventListener.handleConnect(event);

        verify(presenceService).setOnline("user456", "session456", "unknown");
    }

    @Test
    void handleDisconnect_WithStompPrincipal_SetsUserOffline() {
        StompPrincipal principal = new StompPrincipal("user123", "john@test.com", "John");
        Message<byte[]> message = createMessage(StompCommand.DISCONNECT, "session123", principal);
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "session123", null);

        when(presenceService.setOffline("user123", "session123")).thenReturn(true);

        eventListener.handleDisconnect(event);

        verify(presenceService).setOffline("user123", "session123");
    }

    @Test
    void handleDisconnect_UserStillHasSessions_DoesNotLogFullyOffline() {
        StompPrincipal principal = new StompPrincipal("user123", "john@test.com", "John");
        Message<byte[]> message = createMessage(StompCommand.DISCONNECT, "session123", principal);
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "session123", null);

        when(presenceService.setOffline("user123", "session123")).thenReturn(false);
        when(presenceService.getSessionCount("user123")).thenReturn(1L);

        eventListener.handleDisconnect(event);

        verify(presenceService).setOffline("user123", "session123");
        verify(presenceService).getSessionCount("user123");
    }

    @Test
    void handleSubscribe_LogsSubscription() {
        TestPrincipal principal = new TestPrincipal("user123");
        Map<String, Object> headers = new HashMap<>();
        headers.put("simpSessionId", "session123");
        headers.put("simpDestination", "/user/queue/messages");
        headers.put("simpUser", principal);
        headers.put("stompCommand", StompCommand.SUBSCRIBE);

        Message<byte[]> message = new GenericMessage<>(new byte[0], new MessageHeaders(headers));
        SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

        // Should not throw
        eventListener.handleSubscribe(event);
    }

    private Message<byte[]> createMessage(StompCommand command, String sessionId, java.security.Principal principal) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("simpSessionId", sessionId);
        headers.put("simpUser", principal);
        headers.put("stompCommand", command);
        return new GenericMessage<>(new byte[0], new MessageHeaders(headers));
    }

    private static class TestPrincipal implements java.security.Principal {
        private final String name;

        TestPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
