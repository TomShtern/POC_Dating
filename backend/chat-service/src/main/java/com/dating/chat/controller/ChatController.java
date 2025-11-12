package com.dating.chat.controller;

import com.dating.chat.dto.*;
import com.dating.chat.service.ConversationService;
import com.dating.chat.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Chat operations
 *
 * Handles HTTP requests for chat management
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chat service is running");
    }

    /**
     * Get all conversations for the authenticated user
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummaryDTO>> getUserConversations(Authentication authentication) {
        UUID userId = getUserIdFromAuth(authentication);
        log.info("Fetching conversations for user: {}", userId);

        List<ConversationSummaryDTO> conversations = conversationService.getUserConversations(userId);
        return ResponseEntity.ok(conversations);
    }

    /**
     * Get conversation by ID
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ConversationDTO> getConversation(
            @PathVariable UUID conversationId,
            Authentication authentication) {

        UUID userId = getUserIdFromAuth(authentication);
        log.info("User {} fetching conversation {}", userId, conversationId);

        ConversationDTO conversation = conversationService.getConversationById(conversationId, userId);
        return ResponseEntity.ok(conversation);
    }

    /**
     * Create conversation from a match
     */
    @PostMapping("/conversations/from-match/{matchId}")
    public ResponseEntity<ConversationDTO> createConversationFromMatch(
            @PathVariable UUID matchId,
            Authentication authentication) {

        UUID userId = getUserIdFromAuth(authentication);
        log.info("User {} creating conversation from match {}", userId, matchId);

        ConversationDTO conversation = conversationService.createConversationFromMatch(matchId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
    }

    /**
     * Archive a conversation
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> archiveConversation(
            @PathVariable UUID conversationId,
            Authentication authentication) {

        UUID userId = getUserIdFromAuth(authentication);
        log.info("User {} archiving conversation {}", userId, conversationId);

        conversationService.archiveConversation(conversationId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get message history for a conversation (paginated)
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Page<MessageDTO>> getMessageHistory(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {

        UUID userId = getUserIdFromAuth(authentication);
        log.info("User {} fetching message history for conversation {} (page {}, size {})",
                userId, conversationId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<MessageDTO> messages = messageService.getMessageHistory(conversationId, userId, pageable);

        return ResponseEntity.ok(messages);
    }

    /**
     * Get recent messages for a conversation
     */
    @GetMapping("/conversations/{conversationId}/messages/recent")
    public ResponseEntity<List<MessageDTO>> getRecentMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {

        UUID userId = getUserIdFromAuth(authentication);
        log.info("User {} fetching {} recent messages for conversation {}",
                userId, limit, conversationId);

        List<MessageDTO> messages = messageService.getRecentMessages(conversationId, userId, limit);
        return ResponseEntity.ok(messages);
    }

    /**
     * Send a message (REST endpoint - alternative to WebSocket)
     */
    @PostMapping("/messages")
    public ResponseEntity<MessageDTO> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {

        UUID userId = getUserIdFromAuth(authentication);
        log.info("User {} sending message via REST to conversation {}",
                userId, request.getConversationId());

        MessageDTO message = messageService.sendMessage(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    /**
     * Get a specific message
     */
    @GetMapping("/messages/{messageId}")
    public ResponseEntity<MessageDTO> getMessage(
            @PathVariable UUID messageId,
            Authentication authentication) {

        UUID userId = getUserIdFromAuth(authentication);
        log.info("User {} fetching message {}", userId, messageId);

        MessageDTO message = messageService.getMessageById(messageId, userId);
        return ResponseEntity.ok(message);
    }

    /**
     * Mark messages as read
     */
    @PutMapping("/messages/read")
    public ResponseEntity<Void> markMessagesAsRead(
            @Valid @RequestBody MarkAsReadRequest request,
            Authentication authentication) {

        UUID userId = getUserIdFromAuth(authentication);
        log.info("User {} marking {} messages as read in conversation {}",
                userId, request.getMessageIds().size(), request.getConversationId());

        messageService.markMessagesAsRead(request.getConversationId(), request.getMessageIds(), userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete a message
     */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID messageId,
            Authentication authentication) {

        UUID userId = getUserIdFromAuth(authentication);
        log.info("User {} deleting message {}", userId, messageId);

        messageService.deleteMessage(messageId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Helper method to extract user ID from authentication
     */
    private UUID getUserIdFromAuth(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }

    /**
     * Exception handler for security exceptions
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException e) {
        log.error("Security exception: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("FORBIDDEN", e.getMessage()));
    }

    /**
     * Exception handler for illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Invalid argument: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
    }

    /**
     * Exception handler for illegal state exceptions
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        log.error("Invalid state: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONFLICT", e.getMessage()));
    }

    /**
     * Generic exception handler
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred"));
    }

    /**
     * Simple error response DTO
     */
    record ErrorResponse(String code, String message) {}
}
