package com.dating.ui.service;

import com.dating.ui.client.ChatServiceClient;
import com.dating.ui.dto.Conversation;
import com.dating.ui.dto.Message;
import com.dating.ui.dto.SendMessageRequest;
import com.dating.ui.exception.ServiceException;
import com.dating.ui.security.SecurityUtils;
import feign.FeignException;
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
    private final Counter messagesReadCounter;

    public ChatService(ChatServiceClient chatClient, MeterRegistry meterRegistry) {
        this.chatClient = chatClient;
        this.apiCallTimer = Timer.builder("ui.api.call.time")
            .description("Time spent calling backend services")
            .tag("service", "chat-service")
            .register(meterRegistry);
        this.messagesSentCounter = Counter.builder("ui.messages.sent.total")
            .description("Total number of messages sent")
            .register(meterRegistry);
        this.messagesReadCounter = Counter.builder("ui.messages.read.total")
            .description("Total number of messages read/retrieved")
            .register(meterRegistry);
    }

    /**
     * Get all conversations for current user
     */
    public List<Conversation> getConversations() {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new ServiceException("User not authenticated");
        }

        try {
            List<Conversation> conversations = chatClient.getConversations("Bearer " + token);
            if (conversations == null) {
                throw new ServiceException("Failed to retrieve conversations");
            }
            return conversations;
        } catch (FeignException e) {
            log.error("Failed to get conversations for user: {}", SecurityUtils.getCurrentUserId(), e);
            throw new ServiceException("Unable to load conversations. Please try again.", e);
        }
    }

    /**
     * Get message history for a conversation
     */
    public List<Message> getMessages(String conversationId) {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new ServiceException("User not authenticated");
        }

        try {
            List<Message> messages = chatClient.getMessages(conversationId, "Bearer " + token);
            if (messages == null) {
                throw new ServiceException("Failed to retrieve messages");
            }
            return messages;
        } catch (FeignException e) {
            log.error("Failed to get messages for conversation: {} user: {}",
                conversationId, SecurityUtils.getCurrentUserId(), e);
            if (e.status() == 404) {
                throw new ServiceException("Conversation not found", e);
            }
            if (e.status() == 403) {
                throw new ServiceException("You don't have access to this conversation", e);
            }
            throw new ServiceException("Unable to load messages. Please try again.", e);
        }
    }

    /**
     * Send a message (used as fallback, WebSocket is primary)
     */
    public Message sendMessage(String conversationId, String text) {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new ServiceException("User not authenticated");
        }

        try {
            SendMessageRequest request = new SendMessageRequest(text);
            Message message = chatClient.sendMessage(conversationId, request, "Bearer " + token);
            if (message == null) {
                throw new ServiceException("Failed to send message");
            }
            // Only count successfully sent messages
            messagesSentCounter.increment();
            return message;
        } catch (FeignException e) {
            log.error("Failed to send message to conversation: {} user: {}",
                conversationId, SecurityUtils.getCurrentUserId(), e);
            if (e.status() == 404) {
                throw new ServiceException("Conversation not found", e);
            }
            if (e.status() == 403) {
                throw new ServiceException("You can't send messages to this conversation", e);
            }
            throw new ServiceException("Unable to send message. Please try again.", e);
        }
    }

    /**
     * Get conversation details
     */
    public Conversation getConversation(String conversationId) {
        String token = SecurityUtils.getCurrentToken();

        if (token == null) {
            throw new ServiceException("User not authenticated");
        }

        try {
            Conversation conversation = chatClient.getConversation(conversationId, "Bearer " + token);
            if (conversation == null) {
                throw new ServiceException("Conversation not found");
            }
            return conversation;
        } catch (FeignException e) {
            log.error("Failed to get conversation: {} for user: {}",
                conversationId, SecurityUtils.getCurrentUserId(), e);
            if (e.status() == 404) {
                throw new ServiceException("Conversation not found", e);
            }
            if (e.status() == 403) {
                throw new ServiceException("You don't have access to this conversation", e);
            }
            throw new ServiceException("Unable to load conversation. Please try again.", e);
        }
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
        } catch (FeignException e) {
            // Log but don't throw - typing indicators are not critical
            log.debug("Failed to send typing indicator for conversation: {}", conversationId, e);
        }
    }
}
