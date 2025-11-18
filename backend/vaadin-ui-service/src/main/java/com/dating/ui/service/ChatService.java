package com.dating.ui.service;

import com.dating.ui.client.ChatServiceClient;
import com.dating.ui.dto.Conversation;
import com.dating.ui.dto.Message;
import com.dating.ui.dto.SendMessageRequest;
import com.dating.ui.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for chat-related operations
 * Handles conversations and message history
 * Note: Real-time messaging will be handled via WebSocket in views
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatServiceClient chatClient;

    /**
     * Get all conversations for current user
     */
    public List<Conversation> getConversations() {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return chatClient.getConversations("Bearer " + token);
    }

    /**
     * Get message history for a conversation
     */
    public List<Message> getMessages(String conversationId) {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return chatClient.getMessages(conversationId, "Bearer " + token);
    }

    /**
     * Send a message (used as fallback, WebSocket is primary)
     */
    public Message sendMessage(String conversationId, String text) {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        SendMessageRequest request = new SendMessageRequest(text);
        return chatClient.sendMessage(conversationId, request, "Bearer " + token);
    }

    /**
     * Get conversation details
     */
    public Conversation getConversation(String conversationId) {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return chatClient.getConversation(conversationId, "Bearer " + token);
    }

    /**
     * Send typing indicator to the other user
     */
    public void sendTypingIndicator(String conversationId) {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            return; // Silently ignore if not authenticated
        }

        try {
            chatClient.sendTypingIndicator(conversationId, "Bearer " + token);
        } catch (Exception ex) {
            // Ignore typing indicator errors - they're not critical
            log.debug("Failed to send typing indicator: {}", ex.getMessage());
        }
    }
}
