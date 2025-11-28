package com.dating.ui.client;

import com.dating.ui.dto.ConversationsListResponse;
import com.dating.ui.dto.Message;
import com.dating.ui.dto.MessageListResponse;
import com.dating.ui.dto.SendMessageRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Feign Client for Chat Service
 * Handles conversations and message history
 * Note: Real-time messaging uses WebSocket directly
 */
@FeignClient(name = "chat-service", url = "${services.chat-service.url}")
public interface ChatServiceClient {

    /**
     * Get all conversations for current user.
     */
    @GetMapping("/api/chat/conversations")
    ConversationsListResponse getConversations(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "20") int limit);

    /**
     * Get messages for a specific conversation.
     */
    @GetMapping("/api/chat/conversations/{conversationId}/messages")
    MessageListResponse getMessages(
            @PathVariable UUID conversationId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset);

    /**
     * Send a message.
     */
    @PostMapping("/api/chat/messages")
    Message sendMessage(
            @RequestBody SendMessageRequest request,
            @RequestHeader("X-User-Id") UUID userId);

    @GetMapping("/api/chat/conversations/{conversationId}")
    Conversation getConversation(@PathVariable String conversationId,
                                 @RequestHeader("Authorization") String token);

    @PostMapping("/api/chat/conversations/{conversationId}/typing")
    void sendTypingIndicator(@PathVariable String conversationId,
                            @RequestHeader("Authorization") String token);
}
