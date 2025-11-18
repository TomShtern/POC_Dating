package com.dating.chat.service;

import com.dating.chat.dto.request.SendMessageRequest;
import com.dating.chat.dto.response.ConversationResponse;
import com.dating.chat.dto.response.MessageResponse;
import com.dating.chat.dto.websocket.ChatMessage;
import com.dating.chat.dto.websocket.TypingIndicator;
import com.dating.chat.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Main service for chat operations.
 * Orchestrates message sending, WebSocket broadcasting, and real-time features.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager sessionManager;

    /**
     * Send a message via REST API.
     * Also broadcasts to WebSocket if receiver is online.
     *
     * @param senderId Sender user ID
     * @param receiverId Receiver user ID
     * @param request Message request
     * @return Created message response
     */
    public MessageResponse sendMessage(UUID senderId, UUID receiverId, SendMessageRequest request) {
        // Save message
        MessageResponse message = messageService.sendMessage(senderId, receiverId, request);

        // Broadcast via WebSocket
        broadcastNewMessage(message);

        return message;
    }

    /**
     * Send a message via WebSocket.
     *
     * @param chatMessage WebSocket message
     * @return Created message response
     */
    public MessageResponse sendWebSocketMessage(ChatMessage chatMessage) {
        SendMessageRequest request = new SendMessageRequest(
                chatMessage.getConversationId(),
                chatMessage.getContent()
        );

        return sendMessage(
                chatMessage.getSenderId(),
                chatMessage.getReceiverId(),
                request
        );
    }

    /**
     * Get conversations for a user.
     *
     * @param userId User ID
     * @param limit Maximum number
     * @return List of conversations
     */
    public List<ConversationResponse> getConversations(UUID userId, int limit) {
        return conversationService.getConversations(userId, limit);
    }

    /**
     * Get messages for a conversation.
     *
     * @param conversationId Conversation ID
     * @param limit Number of messages
     * @param offset Offset
     * @return List of messages
     */
    public List<MessageResponse> getMessages(UUID conversationId, int limit, int offset) {
        return messageService.getMessages(conversationId, limit, offset);
    }

    /**
     * Mark messages as read.
     *
     * @param conversationId Conversation ID
     * @param userId User marking as read
     * @return Number of messages marked as read
     */
    public int markAsRead(UUID conversationId, UUID userId) {
        int count = messageService.markAllAsRead(conversationId, userId);

        // Broadcast read status via WebSocket
        if (count > 0) {
            broadcastReadStatus(conversationId, userId);
        }

        return count;
    }

    /**
     * Handle typing indicator.
     *
     * @param indicator Typing indicator
     */
    public void handleTypingIndicator(TypingIndicator indicator) {
        log.debug("Typing indicator: {} in conversation {} from user {}",
                indicator.getType(), indicator.getConversationId(), indicator.getUserId());

        // Broadcast to the other participant
        String destination = "/queue/typing";
        messagingTemplate.convertAndSendToUser(
                getOtherParticipant(indicator.getConversationId(), indicator.getUserId()).toString(),
                destination,
                indicator
        );
    }

    /**
     * Broadcast a new message to participants via WebSocket.
     *
     * @param message Message to broadcast
     */
    private void broadcastNewMessage(MessageResponse message) {
        ChatMessage chatMessage = ChatMessage.messageReceived(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                message.getReceiverId(),
                message.getContent(),
                message.getStatus()
        );

        // Send to receiver
        String destination = "/queue/messages";

        // Notify receiver
        if (sessionManager.isUserOnline(message.getReceiverId())) {
            messagingTemplate.convertAndSendToUser(
                    message.getReceiverId().toString(),
                    destination,
                    chatMessage
            );
            log.debug("Broadcasted message {} to user {}", message.getId(), message.getReceiverId());
        }

        // Also notify sender (for multi-device sync)
        if (sessionManager.isUserOnline(message.getSenderId())) {
            messagingTemplate.convertAndSendToUser(
                    message.getSenderId().toString(),
                    destination,
                    chatMessage
            );
        }
    }

    /**
     * Broadcast read status to the sender.
     *
     * @param conversationId Conversation ID
     * @param readerId User who read the messages
     */
    private void broadcastReadStatus(UUID conversationId, UUID readerId) {
        UUID senderId = getOtherParticipant(conversationId, readerId);

        ChatMessage statusUpdate = ChatMessage.statusUpdate(
                ChatMessage.MessageType.MESSAGE_READ,
                null, // All messages in conversation
                conversationId,
                com.dating.common.constant.MessageStatus.READ
        );

        if (sessionManager.isUserOnline(senderId)) {
            messagingTemplate.convertAndSendToUser(
                    senderId.toString(),
                    "/queue/messages",
                    statusUpdate
            );
            log.debug("Broadcasted read status for conversation {} to user {}",
                    conversationId, senderId);
        }
    }

    /**
     * Get the other participant in a conversation.
     * This is a simplified version - in production, would need to look up the match.
     *
     * @param conversationId Conversation ID
     * @param userId Known user ID
     * @return Other participant ID
     */
    private UUID getOtherParticipant(UUID conversationId, UUID userId) {
        // In a real implementation, we would look up the match to get both user IDs
        // For this POC, we'll need to get this from the last message
        MessageResponse lastMessage = messageService.getLastMessage(conversationId);
        if (lastMessage == null) {
            return null;
        }

        return lastMessage.getSenderId().equals(userId)
                ? lastMessage.getReceiverId()
                : lastMessage.getSenderId();
    }
}
