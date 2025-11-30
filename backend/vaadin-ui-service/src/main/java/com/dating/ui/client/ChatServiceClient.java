package com.dating.ui.client;

import com.dating.ui.dto.ConversationsListResponse;
import com.dating.ui.dto.Conversation;
import com.dating.ui.dto.Message;
import com.dating.ui.dto.MessageListResponse;
import com.dating.ui.dto.SendMessageRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    List<Conversation> getConversations(
            @RequestHeader("Authorization") String token);

    /**
     * Get messages for a specific conversation.
     */
    @GetMapping("/api/chat/conversations/{conversationId}/messages")
    List<Message> getMessages(
            @PathVariable String conversationId,
            @RequestHeader("Authorization") String token);

    /**
     * Send a message.
     */
    @PostMapping("/api/chat/conversations/{conversationId}/messages")
    Message sendMessage(
            @PathVariable String conversationId,
            @RequestBody SendMessageRequest request,
            @RequestHeader("Authorization") String token);

    @GetMapping("/api/chat/conversations/{conversationId}")
    Conversation getConversation(@PathVariable String conversationId,
                                 @RequestHeader("Authorization") String token);

    @PostMapping("/api/chat/conversations/{conversationId}/typing")
    void sendTypingIndicator(@PathVariable String conversationId,
                            @RequestHeader("Authorization") String token);
}
