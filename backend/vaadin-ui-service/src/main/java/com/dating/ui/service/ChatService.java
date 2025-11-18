package com.dating.ui.service;

import com.dating.ui.client.ChatServiceClient;
import com.dating.ui.dto.Conversation;
import com.dating.ui.dto.Message;
import com.dating.ui.dto.SendMessageRequest;
import com.dating.ui.security.SecurityUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for chat-related operations
 * Handles conversations and message history
 * Note: Real-time messaging will be handled via WebSocket in views
 */
@Service
@Slf4j
public class ChatService {

    private final ChatServiceClient chatClient;
    private final Timer apiCallTimer;
    private final Counter messagesSentCounter;

    public ChatService(ChatServiceClient chatClient, MeterRegistry meterRegistry) {
        this.chatClient = chatClient;
        this.apiCallTimer = Timer.builder("ui.api.call.time")
            .description("Time spent calling backend services")
            .tag("service", "chat-service")
            .register(meterRegistry);
        this.messagesSentCounter = Counter.builder("ui.messages.sent.total")
            .description("Total number of messages sent")
            .register(meterRegistry);
    }

    /**
     * Get all conversations for current user
     */
    public List<Conversation> getConversations() {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return apiCallTimer.record(() -> chatClient.getConversations("Bearer " + token));
    }

    /**
     * Get message history for a conversation
     */
    public List<Message> getMessages(String conversationId) {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return apiCallTimer.record(() -> chatClient.getMessages(conversationId, "Bearer " + token));
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
        Message message = apiCallTimer.record(() -> chatClient.sendMessage(conversationId, request, "Bearer " + token));
        messagesSentCounter.increment();
        return message;
    }

    /**
     * Get conversation details
     */
    public Conversation getConversation(String conversationId) {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new IllegalStateException("User not authenticated");
        }

        return apiCallTimer.record(() -> chatClient.getConversation(conversationId, "Bearer " + token));
    }
}
