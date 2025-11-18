package com.dating.chat.controller;

import com.dating.chat.dto.websocket.ChatMessageEvent;
import com.dating.chat.dto.websocket.MarkReadRequest;
import com.dating.chat.dto.websocket.MessageDeliveredEvent;
import com.dating.chat.dto.websocket.MessagesReadEvent;
import com.dating.chat.dto.websocket.SendMessageRequest;
import com.dating.chat.dto.websocket.TypingEvent;
import com.dating.chat.dto.websocket.TypingIndicator;
import com.dating.chat.security.StompPrincipal;
import com.dating.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/**
 * Chat WebSocket Controller
 *
 * Handles incoming WebSocket messages and routes to appropriate handlers.
 *
 * STOMP DESTINATIONS:
 * - /app/chat.send - Send a message
 * - /app/chat.typing - Send typing indicator
 * - /app/chat.read - Mark messages as read
 *
 * SUBSCRIPTIONS:
 * - /user/queue/messages - Receive messages for this user
 * - /user/queue/typing - Receive typing indicators
 * - /user/queue/read - Receive read receipts
 * - /user/queue/delivered - Receive delivery confirmations
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle sending a chat message.
     *
     * Client sends to: /app/chat.send
     * Server broadcasts to: /user/{recipientId}/queue/messages
     */
    @MessageMapping("/chat.send")
    public void sendMessage(SendMessageRequest request, Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());
        String senderName = getSenderName(principal);

        log.info("Message received: matchId={}, senderId={}", request.matchId(), senderId);

        // Validate sender is part of this match
        if (!chatMessageService.isUserInMatch(senderId, request.matchId())) {
            log.warn("User {} attempted to send message to match {} they're not part of",
                    senderId, request.matchId());
            throw new AccessDeniedException("Not authorized for this conversation");
        }

        // Save message to database
        ChatMessageEvent savedMessage = chatMessageService.saveMessage(
                request.matchId(),
                senderId,
                senderName,
                request.content(),
                request.type()
        );

        // Send to recipient via their personal queue
        UUID recipientId = chatMessageService.getOtherUserId(request.matchId(), senderId);

        // Send to recipient
        messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/messages",
                savedMessage
        );

        // Send delivery confirmation back to sender
        messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/queue/delivered",
                new MessageDeliveredEvent(
                        savedMessage.messageId(),
                        savedMessage.matchId(),
                        Instant.now()
                )
        );

        log.info("Message delivered: messageId={}, to={}", savedMessage.messageId(), recipientId);
    }

    /**
     * Handle typing indicator.
     *
     * Client sends to: /app/chat.typing
     * Server broadcasts to: /user/{recipientId}/queue/typing
     */
    @MessageMapping("/chat.typing")
    public void typing(TypingIndicator indicator, Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());

        // Validate user is in match (silently ignore if not)
        if (!chatMessageService.isUserInMatch(senderId, indicator.matchId())) {
            log.warn("User {} sent typing indicator for match {} they're not part of",
                    senderId, indicator.matchId());
            return;
        }

        // Get recipient
        UUID recipientId = chatMessageService.getOtherUserId(indicator.matchId(), senderId);

        // Send typing indicator to recipient
        messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/typing",
                new TypingEvent(indicator.matchId(), senderId, indicator.isTyping())
        );

        log.debug("Typing indicator sent: matchId={}, userId={}, isTyping={}",
                indicator.matchId(), senderId, indicator.isTyping());
    }

    /**
     * Handle marking messages as read.
     *
     * Client sends to: /app/chat.read
     * Server broadcasts to: /user/{senderId}/queue/read
     */
    @MessageMapping("/chat.read")
    public void markRead(MarkReadRequest request, Principal principal) {
        UUID readerId = UUID.fromString(principal.getName());

        // Validate user is in match
        if (!chatMessageService.isUserInMatch(readerId, request.matchId())) {
            log.warn("User {} attempted to mark messages read in match {} they're not part of",
                    readerId, request.matchId());
            throw new AccessDeniedException("Not authorized for this conversation");
        }

        // Update read status in database
        chatMessageService.markMessagesAsRead(
                request.matchId(),
                readerId,
                request.lastReadMessageId()
        );

        // Notify the original sender that their messages were read
        UUID senderId = chatMessageService.getOtherUserId(request.matchId(), readerId);

        messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/queue/read",
                new MessagesReadEvent(
                        request.matchId(),
                        readerId,
                        request.lastReadMessageId(),
                        Instant.now()
                )
        );

        log.info("Messages marked as read: matchId={}, readerId={}, lastMessageId={}",
                request.matchId(), readerId, request.lastReadMessageId());
    }

    /**
     * Extract sender name from principal.
     */
    private String getSenderName(Principal principal) {
        if (principal instanceof StompPrincipal stompPrincipal) {
            return stompPrincipal.username();
        }
        return "Unknown";
    }
}
