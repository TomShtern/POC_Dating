package com.dating.chat.service;

import com.dating.chat.config.CacheConfig;
import com.dating.chat.dto.request.SendMessageRequest;
import com.dating.chat.dto.response.MessageResponse;
import com.dating.chat.event.ChatEventPublisher;
import com.dating.chat.mapper.MessageMapper;
import com.dating.chat.model.Message;
import com.dating.chat.repository.MessageRepository;
import com.dating.common.exception.MessageNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for message operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final ChatEventPublisher eventPublisher;

    /**
     * Send a message.
     *
     * @param senderId ID of the sender
     * @param receiverId ID of the receiver
     * @param request Message request
     * @return Created message response
     */
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CONVERSATION_MESSAGES_CACHE, key = "#request.conversationId()"),
        @CacheEvict(value = CacheConfig.CONVERSATIONS_CACHE, key = "#senderId"),
        @CacheEvict(value = CacheConfig.CONVERSATIONS_CACHE, key = "#receiverId"),
        @CacheEvict(value = CacheConfig.UNREAD_COUNT_CACHE, key = "#receiverId")
    })
    @Transactional
    public MessageResponse sendMessage(UUID senderId, UUID receiverId, SendMessageRequest request) {
        log.debug("Sending message from {} to {} in conversation {}",
                senderId, receiverId, request.conversationId());

        Message message = Message.builder()
                .matchId(request.conversationId())
                .senderId(senderId)
                .receiverId(receiverId)
                .content(request.content())
                .build();

        Message saved = messageRepository.save(message);
        log.info("Message sent: {} in conversation {}", saved.getId(), request.conversationId());

        // Publish event
        eventPublisher.publishMessageSent(saved);

        return messageMapper.toMessageResponse(saved);
    }

    /**
     * Get messages for a conversation.
     *
     * @param conversationId Match/conversation ID
     * @param limit Number of messages to return
     * @param offset Offset for pagination
     * @return List of messages
     */
    @Cacheable(value = CacheConfig.CONVERSATION_MESSAGES_CACHE,
               key = "#conversationId + '-' + #limit + '-' + #offset")
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(UUID conversationId, int limit, int offset) {
        log.debug("Getting messages for conversation {}, limit={}, offset={}",
                conversationId, limit, offset);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Message> messages = messageRepository.findByMatchIdOrderByCreatedAtDesc(
                conversationId, pageable);

        return messages.getContent().stream()
                .map(messageMapper::toMessageResponse)
                .toList();
    }

    /**
     * Get a message by ID.
     *
     * @param messageId Message ID
     * @return Message response
     * @throws MessageNotFoundException if message not found
     */
    @Transactional(readOnly = true)
    public MessageResponse getMessageById(UUID messageId) {
        log.debug("Getting message: {}", messageId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found: " + messageId));

        return messageMapper.toMessageResponse(message);
    }

    /**
     * Mark all messages as read in a conversation.
     *
     * @param conversationId Match/conversation ID
     * @param receiverId User marking messages as read
     * @return Number of messages marked as read
     */
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CONVERSATION_MESSAGES_CACHE, key = "#conversationId"),
        @CacheEvict(value = CacheConfig.UNREAD_COUNT_CACHE, key = "#receiverId")
    })
    @Transactional
    public int markAllAsRead(UUID conversationId, UUID receiverId) {
        log.debug("Marking all messages as read in conversation {} for user {}",
                conversationId, receiverId);

        int updated = messageRepository.markAllAsRead(conversationId, receiverId, Instant.now());
        log.info("Marked {} messages as read in conversation {}", updated, conversationId);

        if (updated > 0) {
            eventPublisher.publishMessagesRead(conversationId, receiverId, updated);
        }

        return updated;
    }

    /**
     * Count unread messages for a user in a conversation.
     *
     * @param conversationId Match/conversation ID
     * @param userId User ID
     * @return Number of unread messages
     */
    @Cacheable(value = CacheConfig.UNREAD_COUNT_CACHE,
               key = "#conversationId + '-' + #userId")
    @Transactional(readOnly = true)
    public long countUnread(UUID conversationId, UUID userId) {
        return messageRepository.countUnreadByMatchIdAndReceiverId(conversationId, userId);
    }

    /**
     * Count total unread messages for a user.
     *
     * @param userId User ID
     * @return Total number of unread messages
     */
    @Transactional(readOnly = true)
    public long countTotalUnread(UUID userId) {
        return messageRepository.countUnreadByReceiverId(userId);
    }

    /**
     * Get the last message in a conversation.
     *
     * @param conversationId Match/conversation ID
     * @return Last message response or null
     */
    @Transactional(readOnly = true)
    public MessageResponse getLastMessage(UUID conversationId) {
        Message message = messageRepository.findLastMessageByMatchId(conversationId);
        return message != null ? messageMapper.toMessageResponse(message) : null;
    }

    /**
     * Get messages since a specific time (for sync).
     *
     * @param conversationId Match/conversation ID
     * @param since Timestamp
     * @return List of new messages
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesSince(UUID conversationId, Instant since) {
        log.debug("Getting messages in conversation {} since {}", conversationId, since);

        List<Message> messages = messageRepository.findByMatchIdAndCreatedAtAfter(
                conversationId, since);

        return messages.stream()
                .map(messageMapper::toMessageResponse)
                .toList();
    }
}
