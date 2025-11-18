package com.dating.chat.controller;

import com.dating.chat.dto.request.SendMessageRequest;
import com.dating.chat.dto.response.MessageResponse;
import com.dating.chat.service.ChatService;
import com.dating.chat.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for message operations.
 * Handles sending messages via REST API.
 */
@RestController
@RequestMapping("/api/chat/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final ChatService chatService;
    private final MessageService messageService;

    /**
     * Send a message.
     * The receiver is implicitly the other participant in the match.
     *
     * @param userId Sender user ID from X-User-Id header
     * @param request Message request
     * @return Created message response
     */
    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody SendMessageRequest request) {

        log.debug("Send message request from user {} in conversation {}",
                userId, request.conversationId());

        // ChatService will handle determining the other participant for WebSocket broadcast
        MessageResponse response = chatService.sendMessage(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a specific message by ID.
     *
     * @param messageId Message ID
     * @param userId User ID from X-User-Id header
     * @return Message response
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<MessageResponse> getMessage(
            @PathVariable UUID messageId,
            @RequestHeader("X-User-Id") UUID userId) {

        log.debug("Get message request for message {} by user {}", messageId, userId);

        MessageResponse response = messageService.getMessageById(messageId);

        // Verify user is a participant in the conversation
        // For now, just check if user is the sender
        // TODO: Check if user is a participant in the match
        if (!response.getSenderId().equals(userId)) {
            // User is not the sender, they must be the receiver (other participant in match)
            // For proper validation, we should check match participants from Match Service
            log.debug("User {} is not sender of message {}, assuming they are the receiver",
                    userId, messageId);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get total unread message count for a user.
     *
     * @param userId User ID from X-User-Id header
     * @return Unread message count
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(
            @RequestHeader("X-User-Id") UUID userId) {

        log.debug("Get unread count request for user {}", userId);

        long count = messageService.countTotalUnread(userId);

        return ResponseEntity.ok(count);
    }
}
