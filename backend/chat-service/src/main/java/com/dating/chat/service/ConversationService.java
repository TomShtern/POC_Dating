package com.dating.chat.service;

import com.dating.chat.config.CacheConfig;
import com.dating.chat.dto.response.ConversationResponse;
import com.dating.chat.dto.response.MessageResponse;
import com.dating.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for conversation management.
 * A conversation is tied to a match - matchId = conversationId.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final MessageRepository messageRepository;
    private final MessageService messageService;

    /**
     * Get all conversations for a user.
     * Returns list sorted by last message time.
     *
     * @param userId User ID
     * @param limit Maximum number of conversations
     * @return List of conversations
     */
    @Cacheable(value = CacheConfig.CONVERSATIONS_CACHE, key = "#userId + '-' + #limit")
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(UUID userId, int limit) {
        log.debug("Getting conversations for user {}, limit={}", userId, limit);

        // Get all match IDs where user has messages
        List<UUID> matchIds = messageRepository.findMatchIdsByUserId(userId);

        // Build conversation responses
        List<ConversationResponse> conversations = matchIds.stream()
                .map(matchId -> buildConversationResponse(matchId, userId))
                .sorted((c1, c2) -> {
                    // Sort by last message time, most recent first
                    if (c1.getLastMessageAt() == null) return 1;
                    if (c2.getLastMessageAt() == null) return -1;
                    return c2.getLastMessageAt().compareTo(c1.getLastMessageAt());
                })
                .limit(limit)
                .toList();

        log.debug("Found {} conversations for user {}", conversations.size(), userId);
        return conversations;
    }

    /**
     * Get a specific conversation.
     *
     * @param conversationId Conversation/match ID
     * @param userId Current user ID
     * @return Conversation response
     */
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID conversationId, UUID userId) {
        log.debug("Getting conversation {} for user {}", conversationId, userId);
        return buildConversationResponse(conversationId, userId);
    }

    /**
     * Check if a conversation exists.
     *
     * @param conversationId Conversation/match ID
     * @return true if exists
     */
    @Transactional(readOnly = true)
    public boolean conversationExists(UUID conversationId) {
        return messageRepository.existsByMatchId(conversationId);
    }

    /**
     * Build a conversation response with last message and unread count.
     *
     * @param matchId Match/conversation ID
     * @param userId Current user ID
     * @return Conversation response
     */
    private ConversationResponse buildConversationResponse(UUID matchId, UUID userId) {
        MessageResponse lastMessage = messageService.getLastMessage(matchId);
        long unreadCount = messageService.countUnread(matchId, userId);

        // Determine the other participant by looking at messages
        // Find a message from someone other than the current user
        UUID participantId = null;
        var messages = messageService.getMessages(matchId, 50, 0);
        if (!messages.isEmpty()) {
            participantId = messages.stream()
                    .map(MessageResponse::getSenderId)
                    .filter(senderId -> !senderId.equals(userId))
                    .findFirst()
                    .orElse(null);
        }
        // TODO: For production, get participant info from Match Service and User Service
        // For now, build a placeholder MatchedUser
        ConversationResponse.MatchedUser matchedUser = null;
        if (participantId != null) {
            matchedUser = ConversationResponse.MatchedUser.builder()
                    .id(participantId)
                    .name(null)  // Would be fetched from User Service
                    .profilePictureUrl(null)  // Would be fetched from User Service
                    .build();
        }

        return ConversationResponse.builder()
                .id(matchId)
                .matchedUser(matchedUser)
                .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                .unreadCount((int) unreadCount)
                .lastMessageTime(lastMessage != null ? lastMessage.getCreatedAt() : null)
                .createdAt(null)  // Would be fetched from Match Service
                .build();
    }
}
