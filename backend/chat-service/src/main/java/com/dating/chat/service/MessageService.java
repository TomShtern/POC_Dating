package com.dating.chat.service;

import com.dating.chat.dto.MessageDTO;
import com.dating.chat.dto.SendMessageRequest;
import com.dating.chat.dto.WebSocketMessageDTO;
import com.dating.chat.entity.Message;
import com.dating.chat.entity.enums.MessageStatus;
import com.dating.chat.repository.ConversationRepository;
import com.dating.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing messages
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send a message in a conversation
     */
    @Transactional
    public MessageDTO sendMessage(SendMessageRequest request, UUID senderId) {
        log.debug("User {} sending message in conversation {}", senderId, request.getConversationId());

        // Verify sender is participant of the conversation
        if (!conversationService.isUserParticipant(request.getConversationId(), senderId)) {
            throw new SecurityException("User is not a participant of this conversation");
        }

        // Get recipient ID
        UUID recipientId = conversationService.getOtherParticipant(request.getConversationId(), senderId);

        // Create message
        Message message = Message.builder()
                .conversationId(request.getConversationId())
                .senderId(senderId)
                .recipientId(recipientId)
                .content(request.getContent())
                .timestamp(LocalDateTime.now())
                .status(MessageStatus.SENT)
                .deleted(false)
                .build();

        message = messageRepository.save(message);
        log.info("Message {} sent in conversation {}", message.getId(), request.getConversationId());

        // Update conversation last message timestamp
        conversationService.updateLastMessageTimestamp(request.getConversationId(), message.getTimestamp());

        // Send via WebSocket to recipient
        sendWebSocketMessage(message, recipientId);

        return mapToMessageDTO(message);
    }

    /**
     * Get message history for a conversation (paginated)
     */
    @Transactional(readOnly = true)
    public Page<MessageDTO> getMessageHistory(UUID conversationId, UUID userId, Pageable pageable) {
        log.debug("Fetching message history for conversation {} by user {}", conversationId, userId);

        // Verify user is participant
        if (!conversationService.isUserParticipant(conversationId, userId)) {
            throw new SecurityException("User is not a participant of this conversation");
        }

        Page<Message> messages = messageRepository.findByConversationIdAndDeletedFalseOrderByTimestampDesc(
                conversationId, pageable);

        return messages.map(this::mapToMessageDTO);
    }

    /**
     * Get recent messages for a conversation (for chat display)
     */
    @Transactional(readOnly = true)
    public List<MessageDTO> getRecentMessages(UUID conversationId, UUID userId, int limit) {
        log.debug("Fetching {} recent messages for conversation {}", limit, conversationId);

        // Verify user is participant
        if (!conversationService.isUserParticipant(conversationId, userId)) {
            throw new SecurityException("User is not a participant of this conversation");
        }

        List<Message> messages = messageRepository.findByConversationIdAndDeletedFalseOrderByTimestampAsc(conversationId);

        // Get last N messages
        int fromIndex = Math.max(0, messages.size() - limit);
        List<Message> recentMessages = messages.subList(fromIndex, messages.size());

        return recentMessages.stream()
                .map(this::mapToMessageDTO)
                .collect(Collectors.toList());
    }

    /**
     * Mark messages as read
     */
    @Transactional
    public void markMessagesAsRead(UUID conversationId, List<UUID> messageIds, UUID userId) {
        log.debug("Marking {} messages as read in conversation {} by user {}",
                messageIds.size(), conversationId, userId);

        // Verify user is participant
        if (!conversationService.isUserParticipant(conversationId, userId)) {
            throw new SecurityException("User is not a participant of this conversation");
        }

        List<Message> messages = messageRepository.findByIdIn(messageIds);

        for (Message message : messages) {
            // Only mark as read if user is the recipient
            if (message.getRecipientId().equals(userId) && message.getStatus() != MessageStatus.READ) {
                message.markAsRead();
                messageRepository.save(message);

                // Notify sender via WebSocket
                notifyMessageRead(message);
            }
        }
    }

    /**
     * Mark message as delivered
     */
    @Transactional
    public void markMessageAsDelivered(UUID messageId, UUID userId) {
        log.debug("Marking message {} as delivered by user {}", messageId, userId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // Only mark as delivered if user is the recipient
        if (message.getRecipientId().equals(userId) && message.getStatus() == MessageStatus.SENT) {
            message.markAsDelivered();
            messageRepository.save(message);

            // Notify sender via WebSocket
            notifyMessageDelivered(message);
        }
    }

    /**
     * Delete a message (soft delete)
     */
    @Transactional
    public void deleteMessage(UUID messageId, UUID userId) {
        log.info("Deleting message {} by user {}", messageId, userId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // Only sender can delete their own messages
        if (!message.getSenderId().equals(userId)) {
            throw new SecurityException("Only the sender can delete a message");
        }

        message.softDelete();
        messageRepository.save(message);
    }

    /**
     * Get message by ID
     */
    @Transactional(readOnly = true)
    public MessageDTO getMessageById(UUID messageId, UUID userId) {
        log.debug("Fetching message {} by user {}", messageId, userId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // Verify user is participant of the conversation
        if (!conversationService.isUserParticipant(message.getConversationId(), userId)) {
            throw new SecurityException("User is not a participant of this conversation");
        }

        return mapToMessageDTO(message);
    }

    // WebSocket notification methods
    private void sendWebSocketMessage(Message message, UUID recipientId) {
        WebSocketMessageDTO wsMessage = WebSocketMessageDTO.builder()
                .type("MESSAGE_RECEIVED")
                .messageId(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .recipientId(message.getRecipientId())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .build();

        try {
            messagingTemplate.convertAndSendToUser(
                    recipientId.toString(),
                    "/queue/messages",
                    wsMessage
            );
            log.debug("Sent WebSocket message to user {}", recipientId);
        } catch (Exception e) {
            log.error("Failed to send WebSocket message to user {}: {}", recipientId, e.getMessage());
        }
    }

    private void notifyMessageDelivered(Message message) {
        WebSocketMessageDTO wsMessage = WebSocketMessageDTO.builder()
                .type("MESSAGE_DELIVERED")
                .messageId(message.getId())
                .conversationId(message.getConversationId())
                .timestamp(message.getDeliveredAt())
                .build();

        try {
            messagingTemplate.convertAndSendToUser(
                    message.getSenderId().toString(),
                    "/queue/notifications",
                    wsMessage
            );
        } catch (Exception e) {
            log.error("Failed to send delivery notification: {}", e.getMessage());
        }
    }

    private void notifyMessageRead(Message message) {
        WebSocketMessageDTO wsMessage = WebSocketMessageDTO.builder()
                .type("MESSAGE_READ")
                .messageId(message.getId())
                .conversationId(message.getConversationId())
                .timestamp(message.getReadAt())
                .build();

        try {
            messagingTemplate.convertAndSendToUser(
                    message.getSenderId().toString(),
                    "/queue/notifications",
                    wsMessage
            );
        } catch (Exception e) {
            log.error("Failed to send read notification: {}", e.getMessage());
        }
    }

    // Helper method
    private MessageDTO mapToMessageDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .recipientId(message.getRecipientId())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .status(message.getStatus())
                .deliveredAt(message.getDeliveredAt())
                .readAt(message.getReadAt())
                .deleted(message.getDeleted())
                .build();
    }
}
