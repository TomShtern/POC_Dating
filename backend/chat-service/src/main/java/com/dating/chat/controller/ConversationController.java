package com.dating.chat.controller;

import com.dating.chat.dto.response.ConversationResponse;
import com.dating.chat.dto.response.MessageResponse;
import com.dating.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for conversation management.
 * Handles conversation listing and message retrieval.
 */
@RestController
@RequestMapping("/api/chat/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

    private final ChatService chatService;

    /**
     * Get all conversations for the current user.
     *
     * @param userId User ID from X-User-Id header (set by API Gateway)
     * @param limit Maximum number of conversations (default 20)
     * @return List of conversations
     */
    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getConversations(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "20") int limit) {

        log.debug("Get conversations request for user {}, limit={}", userId, limit);

        List<ConversationResponse> conversations = chatService.getConversations(userId, limit);

        return ResponseEntity.ok(conversations);
    }

    /**
     * Get messages for a specific conversation.
     *
     * @param conversationId Conversation/match ID
     * @param userId User ID from X-User-Id header
     * @param limit Maximum number of messages (default 50)
     * @param offset Offset for pagination (default 0)
     * @return List of messages
     */
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable UUID conversationId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        log.debug("Get messages request for conversation {}, user {}, limit={}, offset={}",
                conversationId, userId, limit, offset);

        List<MessageResponse> messages = chatService.getMessages(conversationId, limit, offset);

        return ResponseEntity.ok(messages);
    }

    /**
     * Mark all messages in a conversation as read.
     *
     * @param conversationId Conversation/match ID
     * @param userId User ID from X-User-Id header
     * @return Number of messages marked as read
     */
    @PostMapping("/{conversationId}/read")
    public ResponseEntity<Integer> markAsRead(
            @PathVariable UUID conversationId,
            @RequestHeader("X-User-Id") UUID userId) {

        log.debug("Mark as read request for conversation {}, user {}", conversationId, userId);

        int count = chatService.markAsRead(conversationId, userId);

        return ResponseEntity.ok(count);
    }
}
