package com.dating.chat.controller;

import com.dating.chat.dto.websocket.ChatMessageEvent;
import com.dating.chat.dto.websocket.MessageType;
import com.dating.chat.model.Conversation;
import com.dating.chat.model.Message;
import com.dating.chat.repository.ConversationRepository;
import com.dating.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Chat operations.
 *
 * Provides endpoints for:
 * - Getting conversation list
 * - Getting message history
 * - Getting conversation details
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatRestController {

    private final ChatMessageService chatMessageService;
    private final ConversationRepository conversationRepository;

    /**
     * Get all conversations for the current user.
     *
     * @param userId The user ID from the X-User-Id header (set by API Gateway)
     * @return List of conversations
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getConversations(
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("Getting conversations for user: {}", userId);

        List<Conversation> conversations = conversationRepository.findByUserId(userId);

        List<ConversationResponse> response = conversations.stream()
                .map(conv -> new ConversationResponse(
                        conv.getId(),
                        conv.getMatchId(),
                        conv.getOtherUserId(userId),
                        conv.getCreatedAt().toString(),
                        conv.getLastMessageAt() != null ? conv.getLastMessageAt().toString() : null,
                        chatMessageService.countUnreadMessages(conv.getId(), userId)
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Get message history for a match.
     *
     * @param matchId The match ID
     * @param userId The user ID from the X-User-Id header
     * @param page Page number (0-indexed)
     * @param size Page size (max 100)
     * @return Page of messages
     */
    @GetMapping("/conversations/{matchId}/messages")
    public ResponseEntity<Page<ChatMessageEvent>> getMessages(
            @PathVariable UUID matchId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.info("Getting messages for match: {}, user: {}, page: {}, size: {}",
                matchId, userId, page, size);

        // Validate user is part of this match
        if (!chatMessageService.isUserInMatch(userId, matchId)) {
            log.warn("User {} attempted to access messages for match {} they're not part of",
                    userId, matchId);
            return ResponseEntity.status(403).build();
        }

        // Enforce max page size
        if (size > 100) {
            size = 100;
        }

        Page<Message> messages = chatMessageService.getMessageHistory(matchId, page, size);

        // Convert to events
        Page<ChatMessageEvent> response = messages.map(msg -> new ChatMessageEvent(
                msg.getId(),
                matchId,
                msg.getSenderId(),
                msg.getSenderName(),
                msg.getContent(),
                msg.getType(),
                msg.getCreatedAt()
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Get conversation details by match ID.
     *
     * @param matchId The match ID
     * @param userId The user ID from the X-User-Id header
     * @return Conversation details
     */
    @GetMapping("/conversations/{matchId}")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable UUID matchId,
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("Getting conversation for match: {}, user: {}", matchId, userId);

        return conversationRepository.findByMatchId(matchId)
                .filter(conv -> conv.hasUser(userId))
                .map(conv -> new ConversationResponse(
                        conv.getId(),
                        conv.getMatchId(),
                        conv.getOtherUserId(userId),
                        conv.getCreatedAt().toString(),
                        conv.getLastMessageAt() != null ? conv.getLastMessageAt().toString() : null,
                        chatMessageService.countUnreadMessages(conv.getId(), userId)
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Response DTO for conversation.
     */
    public record ConversationResponse(
            UUID id,
            UUID matchId,
            UUID otherUserId,
            String createdAt,
            String lastMessageAt,
            long unreadCount
    ) {}
}
