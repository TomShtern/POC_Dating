package com.dating.chat.service;

import com.dating.chat.dto.websocket.ChatMessageEvent;
import com.dating.chat.dto.websocket.MessageType;
import com.dating.chat.exception.ConversationNotFoundException;
import com.dating.chat.exception.MessageNotFoundException;
import com.dating.chat.model.Conversation;
import com.dating.chat.model.Message;
import com.dating.chat.repository.ConversationRepository;
import com.dating.chat.repository.MessageRepository;
import com.dating.common.constant.MessageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for handling chat messages.
 *
 * Provides business logic for:
 * - Saving messages with delivery status tracking
 * - Validating user access to conversations
 * - Managing read receipts
 * - Caching with proper invalidation
 * - Pagination with validation
 * - Message ordering
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    @Value("${app.chat.max-history-size:100}")
    private int maxHistorySize;

    @Value("${app.chat.max-page-size:100}")
    private int maxPageSize;

    /**
     * Check if a user is part of a match.
     */
    @Transactional(readOnly = true)
    public boolean isUserInMatch(UUID userId, UUID matchId) {
        return conversationRepository.isUserInMatch(userId, matchId);
    }

    /**
     * Get the other user's ID in a match.
     */
    @Transactional(readOnly = true)
    public UUID getOtherUserId(UUID matchId, UUID currentUserId) {
        Conversation conversation = conversationRepository.findByMatchId(matchId)
                .orElseThrow(() -> new ConversationNotFoundException(matchId));
        return conversation.getOtherUserId(currentUserId);
    }

    /**
     * Save a new message and return it as an event.
     * Message is saved with SENT status initially.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "messages", allEntries = true),
            @CacheEvict(value = "conversations", key = "#matchId")
    })
    public ChatMessageEvent saveMessage(UUID matchId, UUID senderId, String senderName,
                                        String content, MessageType type) {
        // Find the conversation
        Conversation conversation = conversationRepository.findByMatchId(matchId)
                .orElseThrow(() -> new ConversationNotFoundException(matchId));

        // Create and save the message with ordering
        Message message = Message.builder()
                .matchId(matchId)
                .senderId(senderId)
                .senderName(senderName)
                .content(content)
                .type(type)
                .status(MessageStatus.SENT)
                .build();

        message = messageRepository.save(message);

        // Update last message timestamp
        conversation.setLastMessageAt(message.getCreatedAt());
        conversationRepository.save(conversation);

        log.info("Message saved: id={}, matchId={}, senderId={}", message.getId(), matchId, senderId);

        return new ChatMessageEvent(
                message.getId(),
                matchId,
                senderId,
                senderName,
                content,
                type,
                message.getCreatedAt()
        );
    }

    /**
     * Mark a message as delivered.
     */
    @Transactional
    @CacheEvict(value = "messages", allEntries = true)
    public void markMessageAsDelivered(UUID messageId) {
        messageRepository.findById(messageId).ifPresent(message -> {
            if (message.getStatus() == MessageStatus.SENT) {
                message.setStatus(MessageStatus.DELIVERED);
                message.setDeliveredAt(Instant.now());
                messageRepository.save(message);
                log.debug("Message marked as delivered: {}", messageId);
            }
        });
    }

    /**
     * Mark messages as read.
     */
    @Transactional
    @CacheEvict(value = "messages", allEntries = true)
    public void markMessagesAsRead(UUID matchId, UUID readerId, UUID lastReadMessageId) {
        int updated = messageRepository.markAllAsRead(matchId, readerId, Instant.now());
        log.info("Marked {} messages as read: matchId={}, readerId={}", updated, matchId, readerId);
    }

    /**
     * Get message history for a match with pagination validation.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "messages", key = "#matchId + '-' + #page + '-' + #size")
    public Page<Message> getMessageHistory(UUID matchId, int page, int size) {
        // Validate pagination parameters
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }
        if (size > maxPageSize) {
            size = maxPageSize;
            log.warn("Page size exceeded max, capped to {}", maxPageSize);
        }

        // Order by created_at DESC for message ordering guarantee
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return messageRepository.findByMatchIdOrderByCreatedAtDesc(matchId, pageRequest);
    }

    /**
     * Get or create a conversation for a match.
     */
    @Transactional
    public Conversation getOrCreateConversation(UUID matchId, UUID user1Id, UUID user2Id) {
        return conversationRepository.findByMatchId(matchId)
                .orElseGet(() -> {
                    // Ensure user1Id < user2Id for consistency
                    UUID sortedUser1 = user1Id.compareTo(user2Id) < 0 ? user1Id : user2Id;
                    UUID sortedUser2 = user1Id.compareTo(user2Id) < 0 ? user2Id : user1Id;

                    Conversation conversation = Conversation.builder()
                            .matchId(matchId)
                            .user1Id(sortedUser1)
                            .user2Id(sortedUser2)
                            .build();

                    conversation = conversationRepository.save(conversation);
                    log.info("Created conversation: id={}, matchId={}", conversation.getId(), matchId);
                    return conversation;
                });
    }

    /**
     * Count unread messages for a user in a conversation.
     */
    @Transactional(readOnly = true)
    public long countUnreadMessages(UUID conversationId, UUID userId) {
        return messageRepository.countUnreadByMatchIdAndUserId(conversationId, userId);
    }

    /**
     * Get a message by ID.
     * @throws MessageNotFoundException if message not found
     */
    @Transactional(readOnly = true)
    public Message getMessage(UUID messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));
    }
}
