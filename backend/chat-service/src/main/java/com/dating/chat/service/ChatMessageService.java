package com.dating.chat.service;

import com.dating.chat.dto.websocket.ChatMessageEvent;
import com.dating.chat.dto.websocket.MessageType;
import com.dating.chat.exception.ConversationNotFoundException;
import com.dating.chat.model.Conversation;
import com.dating.chat.model.Message;
import com.dating.chat.repository.ConversationRepository;
import com.dating.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for handling chat messages.
 *
 * Provides business logic for:
 * - Saving messages
 * - Validating user access to conversations
 * - Managing read receipts
 * - Caching
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    /**
     * Check if a user is part of a match.
     *
     * @param userId The user ID to check
     * @param matchId The match ID
     * @return true if the user is part of the match
     */
    public boolean isUserInMatch(UUID userId, UUID matchId) {
        return conversationRepository.isUserInMatch(userId, matchId);
    }

    /**
     * Get the other user's ID in a match.
     *
     * @param matchId The match ID
     * @param currentUserId The current user's ID
     * @return The other user's ID
     */
    public UUID getOtherUserId(UUID matchId, UUID currentUserId) {
        Conversation conversation = conversationRepository.findByMatchId(matchId)
                .orElseThrow(() -> new ConversationNotFoundException(matchId));
        return conversation.getOtherUserId(currentUserId);
    }

    /**
     * Save a new message and return it as an event.
     *
     * @param matchId The match ID
     * @param senderId The sender's user ID
     * @param content The message content
     * @param type The message type
     * @return The saved message as an event
     */
    @Transactional
    @CacheEvict(value = "messages", key = "#matchId")
    public ChatMessageEvent saveMessage(UUID matchId, UUID senderId, String senderName, String content, MessageType type) {
        // Find the conversation
        Conversation conversation = conversationRepository.findByMatchId(matchId)
                .orElseThrow(() -> new ConversationNotFoundException(matchId));

        // Create and save the message
        Message message = Message.builder()
                .conversation(conversation)
                .senderId(senderId)
                .senderName(senderName)
                .content(content)
                .type(type)
                .build();

        message = messageRepository.save(message);

        // Update last message timestamp
        conversation.setLastMessageAt(message.getCreatedAt());
        conversationRepository.save(conversation);

        log.info("Message saved: id={}, matchId={}, senderId={}", message.getId(), matchId, senderId);

        // Return as event
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
     * Mark messages as read.
     *
     * @param matchId The match ID
     * @param readerId The reader's user ID
     * @param lastReadMessageId The ID of the last read message
     */
    @Transactional
    @CacheEvict(value = "messages", key = "#matchId")
    public void markMessagesAsRead(UUID matchId, UUID readerId, UUID lastReadMessageId) {
        int updated = messageRepository.markMessagesAsRead(matchId, readerId, lastReadMessageId, Instant.now());
        log.info("Marked {} messages as read: matchId={}, readerId={}", updated, matchId, readerId);
    }

    /**
     * Get message history for a match.
     *
     * @param matchId The match ID
     * @param page The page number
     * @param size The page size
     * @return Page of messages
     */
    @Cacheable(value = "messages", key = "#matchId + '-' + #page + '-' + #size")
    public Page<Message> getMessageHistory(UUID matchId, int page, int size) {
        return messageRepository.findByMatchId(matchId, PageRequest.of(page, size));
    }

    /**
     * Get or create a conversation for a match.
     *
     * @param matchId The match ID
     * @param user1Id First user ID
     * @param user2Id Second user ID
     * @return The conversation
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
     *
     * @param conversationId The conversation ID
     * @param userId The user ID
     * @return The count of unread messages
     */
    public long countUnreadMessages(UUID conversationId, UUID userId) {
        return messageRepository.countUnreadMessages(conversationId, userId);
    }
}
