package com.dating.chat.controller;

import com.dating.chat.dto.MarkAsReadRequest;
import com.dating.chat.dto.MessageDTO;
import com.dating.chat.dto.SendMessageRequest;
import com.dating.chat.dto.WebSocketMessageDTO;
import com.dating.chat.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WebSocket Controller for real-time messaging
 *
 * Handles WebSocket STOMP messages for chat operations
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send a message via WebSocket
     *
     * Client sends to: /app/chat.send
     * Response sent to: /user/queue/messages
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Valid @Payload SendMessageRequest request, Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        log.info("User {} sending WebSocket message to conversation {}", userId, request.getConversationId());

        try {
            MessageDTO message = messageService.sendMessage(request, userId);

            // Send confirmation back to sender
            WebSocketMessageDTO confirmation = WebSocketMessageDTO.builder()
                    .type("MESSAGE_SENT")
                    .messageId(message.getId())
                    .conversationId(message.getConversationId())
                    .senderId(message.getSenderId())
                    .recipientId(message.getRecipientId())
                    .content(message.getContent())
                    .timestamp(message.getTimestamp())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    confirmation
            );

        } catch (Exception e) {
            log.error("Error sending WebSocket message: {}", e.getMessage());
            sendErrorToUser(userId, "Failed to send message: " + e.getMessage());
        }
    }

    /**
     * Mark messages as read via WebSocket
     *
     * Client sends to: /app/chat.markRead
     */
    @MessageMapping("/chat.markRead")
    public void markMessagesAsRead(@Valid @Payload MarkAsReadRequest request, Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        log.info("User {} marking messages as read in conversation {} via WebSocket",
                userId, request.getConversationId());

        try {
            messageService.markMessagesAsRead(request.getConversationId(), request.getMessageIds(), userId);
        } catch (Exception e) {
            log.error("Error marking messages as read: {}", e.getMessage());
            sendErrorToUser(userId, "Failed to mark messages as read: " + e.getMessage());
        }
    }

    /**
     * Typing indicator - start typing
     *
     * Client sends to: /app/chat.typing.start
     * Broadcast to: /topic/conversation/{conversationId}/typing
     */
    @MessageMapping("/chat.typing.start")
    public void typingStart(@Payload TypingIndicatorRequest request, Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        log.debug("User {} started typing in conversation {}", userId, request.conversationId());

        WebSocketMessageDTO typingMessage = WebSocketMessageDTO.builder()
                .type("TYPING_START")
                .conversationId(request.conversationId())
                .senderId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        // Broadcast to conversation topic (all participants will receive)
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + request.conversationId() + "/typing",
                typingMessage
        );
    }

    /**
     * Typing indicator - stop typing
     *
     * Client sends to: /app/chat.typing.stop
     * Broadcast to: /topic/conversation/{conversationId}/typing
     */
    @MessageMapping("/chat.typing.stop")
    public void typingStop(@Payload TypingIndicatorRequest request, Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        log.debug("User {} stopped typing in conversation {}", userId, request.conversationId());

        WebSocketMessageDTO typingMessage = WebSocketMessageDTO.builder()
                .type("TYPING_STOP")
                .conversationId(request.conversationId())
                .senderId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        // Broadcast to conversation topic
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + request.conversationId() + "/typing",
                typingMessage
        );
    }

    /**
     * Message delivery acknowledgment
     *
     * Client sends to: /app/chat.delivered
     */
    @MessageMapping("/chat.delivered")
    public void messageDelivered(@Payload MessageDeliveredRequest request, Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        log.debug("User {} acknowledging delivery of message {}", userId, request.messageId());

        try {
            messageService.markMessageAsDelivered(request.messageId(), userId);
        } catch (Exception e) {
            log.error("Error marking message as delivered: {}", e.getMessage());
        }
    }

    /**
     * User online status
     *
     * Client sends to: /app/chat.online
     */
    @MessageMapping("/chat.online")
    public void userOnline(Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        log.info("User {} is online", userId);

        WebSocketMessageDTO onlineMessage = WebSocketMessageDTO.builder()
                .type("USER_ONLINE")
                .senderId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        // Broadcast to all active conversations
        messagingTemplate.convertAndSend("/topic/users/status", onlineMessage);
    }

    /**
     * User offline status
     *
     * Client sends to: /app/chat.offline
     */
    @MessageMapping("/chat.offline")
    public void userOffline(Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        log.info("User {} is offline", userId);

        WebSocketMessageDTO offlineMessage = WebSocketMessageDTO.builder()
                .type("USER_OFFLINE")
                .senderId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        // Broadcast to all active conversations
        messagingTemplate.convertAndSend("/topic/users/status", offlineMessage);
    }

    // Helper methods

    /**
     * Extract user ID from Principal
     */
    private UUID getUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return UUID.fromString(principal.getName());
    }

    /**
     * Send error message to specific user
     */
    private void sendErrorToUser(UUID userId, String errorMessage) {
        WebSocketMessageDTO error = WebSocketMessageDTO.builder()
                .type("ERROR")
                .payload(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/errors",
                error
        );
    }

    // Request DTOs for WebSocket messages

    /**
     * Typing indicator request
     */
    record TypingIndicatorRequest(UUID conversationId) {}

    /**
     * Message delivered request
     */
    record MessageDeliveredRequest(UUID messageId) {}
}
