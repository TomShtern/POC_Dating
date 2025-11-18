package com.dating.chat.service;

import com.dating.chat.config.CacheConfig;
import com.dating.chat.dto.request.SendMessageRequest;
import com.dating.chat.dto.response.MessageListResponse;
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
     * Note: The receiver is implicitly the other participant in the match (not stored).
     *
     * @param senderId ID of the sender
     * @param matchId Match/conversation ID
     * @param request Message request
     * @return Created message response
     */
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CONVERSATION_MESSAGES_CACHE, key = "#request.conversationId()"),
        @CacheEvict(value = CacheConfig.CONVERSATIONS_CACHE, key = "#senderId"),
        @CacheEvict(value = CacheConfig.UNREAD_COUNT_CACHE, allEntries = true)
    })
    @Transactional
    public MessageResponse sendMessage(UUID senderId, SendMessageRequest request) {
        log.debug("Sending message from {} in conversation {}",
                senderId, request.conversationId());

        Message message = Message.builder()
                .matchId(request.conversationId())
                .senderId(senderId)
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
     * Get messages for a conversation with metadata.
     *
     * @param conversationId Match/conversation ID
     * @param limit Number of messages to return
     * @param offset Offset for pagination
     * @return MessageListResponse with messages and metadata
     */
    @Transactional(readOnly = true)
    public MessageListResponse getMessagesWithMetadata(UUID conversationId, int limit, int offset) {
        log.debug("Getting messages with metadata for conversation {}, limit={}, offset={}",
                conversationId, limit, offset);

        // Get messages
        List<MessageResponse> messages = getMessages(conversationId, limit, offset);

        // Get total count
        long totalCount = messageRepository.countByMatchId(conversationId);

        // Calculate hasMore
        boolean hasMore = (offset + messages.size()) < totalCount;

        return MessageListResponse.builder()
                .conversationId(conversationId)
                .messages(messages)
                .total((int) totalCount)
                .limit(limit)
                .offset(offset)
                .hasMore(hasMore)
                .build();
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
     * This marks messages sent by others (not the current user) as read.
     *
     * @param conversationId Match/conversation ID
     * @param userId User marking messages as read
     * @return Number of messages marked as read
     */
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CONVERSATION_MESSAGES_CACHE, key = "#conversationId"),
        @CacheEvict(value = CacheConfig.UNREAD_COUNT_CACHE, allEntries = true)
    })
    @Transactional
    public int markAllAsRead(UUID conversationId, UUID userId) {
        log.debug("Marking all messages as read in conversation {} for user {}",
                conversationId, userId);

        int updated = messageRepository.markAllAsRead(conversationId, userId, Instant.now());
        log.info("Marked {} messages as read in conversation {}", updated, conversationId);

        if (updated > 0) {
            eventPublisher.publishMessagesRead(conversationId, userId, updated);
        }

        return updated;
    }

    /**
     * Count unread messages for a user in a conversation.
     * Counts messages sent by others (not the current user) that are not read.
     *
     * @param conversationId Match/conversation ID
     * @param userId User ID
     * @return Number of unread messages
     */
    @Cacheable(value = CacheConfig.UNREAD_COUNT_CACHE,
               key = "#conversationId + '-' + #userId")
    @Transactional(readOnly = true)
    public long countUnread(UUID conversationId, UUID userId) {
        return messageRepository.countUnreadByMatchIdAndUserId(conversationId, userId);
    }

    /**
     * Count total unread messages for a user across all their conversations.
     * Note: This requires knowing all matchIds the user participates in.
     * TODO: For proper implementation, get matchIds from Match Service.
     *
     * @param userId User ID
     * @return Total number of unread messages
     */
    @Transactional(readOnly = true)
    public long countTotalUnread(UUID userId) {
        // Get all matchIds where user has sent messages
        List<UUID> matchIds = messageRepository.findMatchIdsByUserId(userId);
        if (matchIds.isEmpty()) {
            return 0L;
        }
        return messageRepository.countUnreadByUserIdAndMatchIds(userId, matchIds);
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
