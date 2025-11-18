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
     * The receiver is determined from the conversation participants.
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

        // Get the other participant from the last message in conversation
        MessageResponse lastMessage = messageService.getLastMessage(request.conversationId());
        UUID receiverId;

        if (lastMessage != null) {
            receiverId = lastMessage.getSenderId().equals(userId)
                    ? lastMessage.getReceiverId()
                    : lastMessage.getSenderId();
        } else {
            // This shouldn't happen in a real scenario as conversations are created from matches
            log.error("Cannot determine receiver for conversation {}", request.conversationId());
            return ResponseEntity.badRequest().build();
        }

        MessageResponse response = chatService.sendMessage(userId, receiverId, request);

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

        // Verify user is participant
        if (!response.getSenderId().equals(userId) && !response.getReceiverId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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
