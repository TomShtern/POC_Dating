package com.dating.chat.websocket;

import com.dating.chat.dto.response.MessageResponse;
import com.dating.chat.dto.websocket.ChatMessage;
import com.dating.chat.dto.websocket.TypingIndicator;
import com.dating.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.UUID;

/**
 * WebSocket controller for real-time chat messaging.
 * Handles STOMP messages for sending/receiving chat messages and typing indicators.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler {

    private final ChatService chatService;
    private final WebSocketSessionManager sessionManager;

    /**
     * Handle new WebSocket connection.
     * Extracts user ID from session attributes and registers the session.
     */
    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();

        // Get user ID from native headers (set by client)
        Map<String, Object> nativeHeaders = headers.getSessionAttributes();
        if (nativeHeaders != null) {
            // In a real implementation, would extract from JWT or X-User-Id
            Object userIdHeader = headers.getFirstNativeHeader("X-User-Id");
            if (userIdHeader != null) {
                try {
                    UUID userId = UUID.fromString(userIdHeader.toString());
                    sessionManager.registerSession(userId, sessionId);
                    log.info("WebSocket connected: session={}, user={}", sessionId, userId);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid user ID in WebSocket connection: {}", userIdHeader);
                }
            } else {
                log.debug("WebSocket connected without user ID: session={}", sessionId);
            }
        }
    }

    /**
     * Handle WebSocket disconnection.
     */
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        UUID userId = sessionManager.getUserId(sessionId);

        sessionManager.removeSession(sessionId);
        log.info("WebSocket disconnected: session={}, user={}", sessionId, userId);
    }

    /**
     * Handle incoming chat message.
     * Saves the message and broadcasts to participants.
     *
     * @param chatMessage Incoming message
     * @param headerAccessor Session headers
     * @return Confirmation message to sender
     */
    @MessageMapping("/chat.send")
    @SendToUser("/queue/messages")
    public ChatMessage handleSendMessage(
            @Payload ChatMessage chatMessage,
            SimpMessageHeaderAccessor headerAccessor) {

        log.debug("Received message: {} -> {} in conversation {}",
                chatMessage.getSenderId(),
                chatMessage.getReceiverId(),
                chatMessage.getConversationId());

        try {
            MessageResponse response = chatService.sendWebSocketMessage(chatMessage);

            // Use receiverId from the incoming chatMessage since MessageResponse doesn't store it
            return ChatMessage.messageReceived(
                    response.getId(),
                    response.getConversationId(),
                    response.getSenderId(),
                    chatMessage.getReceiverId(), // receiverId from WebSocket message
                    response.getContent(),
                    response.getStatus()
            );
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ChatMessage.error("Failed to send message: " + e.getMessage());
        }
    }

    /**
     * Handle mark as read request.
     *
     * @param chatMessage Message with conversation ID
     * @param headerAccessor Session headers
     * @return Confirmation
     */
    @MessageMapping("/chat.read")
    @SendToUser("/queue/messages")
    public ChatMessage handleMarkAsRead(
            @Payload ChatMessage chatMessage,
            SimpMessageHeaderAccessor headerAccessor) {

        log.debug("Mark as read: conversation {} by user {}",
                chatMessage.getConversationId(),
                chatMessage.getSenderId());

        try {
            int count = chatService.markAsRead(
                    chatMessage.getConversationId(),
                    chatMessage.getSenderId()
            );

            return ChatMessage.statusUpdate(
                    ChatMessage.MessageType.MESSAGE_READ,
                    null,
                    chatMessage.getConversationId(),
                    com.dating.common.constant.MessageStatus.READ
            );
        } catch (Exception e) {
            log.error("Error marking messages as read", e);
            return ChatMessage.error("Failed to mark as read: " + e.getMessage());
        }
    }

    /**
     * Handle typing indicator.
     * Broadcasts to the other participant.
     *
     * @param indicator Typing indicator
     * @param headerAccessor Session headers
     */
    @MessageMapping("/chat.typing")
    public void handleTypingIndicator(
            @Payload TypingIndicator indicator,
            SimpMessageHeaderAccessor headerAccessor) {

        log.debug("Typing indicator: {} in conversation {} from user {}",
                indicator.getType(),
                indicator.getConversationId(),
                indicator.getUserId());

        chatService.handleTypingIndicator(indicator);
    }
}
