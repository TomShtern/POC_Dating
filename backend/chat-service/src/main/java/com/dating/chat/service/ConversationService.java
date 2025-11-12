package com.dating.chat.service;

import com.dating.chat.client.MatchServiceClient;
import com.dating.chat.client.UserServiceClient;
import com.dating.chat.dto.ConversationDTO;
import com.dating.chat.dto.ConversationSummaryDTO;
import com.dating.chat.dto.MessageDTO;
import com.dating.chat.dto.UserSummaryDTO;
import com.dating.chat.entity.Conversation;
import com.dating.chat.entity.Message;
import com.dating.chat.repository.ConversationRepository;
import com.dating.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing conversations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserServiceClient userServiceClient;
    private final MatchServiceClient matchServiceClient;

    /**
     * Get all conversations for a user
     */
    @Transactional(readOnly = true)
    public List<ConversationSummaryDTO> getUserConversations(UUID userId) {
        log.debug("Fetching conversations for user: {}", userId);

        List<Conversation> conversations = conversationRepository.findAllByUserId(userId);
        List<ConversationSummaryDTO> summaries = new ArrayList<>();

        for (Conversation conversation : conversations) {
            try {
                UUID otherUserId = conversation.getOtherParticipant(userId);
                UserSummaryDTO otherUser = userServiceClient.getUserById(otherUserId);

                // Get last message
                Message lastMessage = messageRepository.findLastMessageByConversationId(conversation.getId())
                        .orElse(null);

                // Count unread messages
                long unreadCount = messageRepository.countUnreadMessages(conversation.getId(), userId);

                ConversationSummaryDTO summary = ConversationSummaryDTO.builder()
                        .id(conversation.getId())
                        .matchId(conversation.getMatchId())
                        .createdAt(conversation.getCreatedAt())
                        .lastMessageAt(conversation.getLastMessageAt())
                        .otherUserId(otherUserId)
                        .otherUserName(otherUser.getName())
                        .otherUserPhotoUrl(otherUser.getPhotoUrl())
                        .unreadCount(unreadCount)
                        .build();

                if (lastMessage != null) {
                    summary.setLastMessageContent(lastMessage.getContent());
                    summary.setLastMessageTimestamp(lastMessage.getTimestamp());
                    summary.setLastMessageFromMe(lastMessage.getSenderId().equals(userId));
                }

                summaries.add(summary);
            } catch (Exception e) {
                log.error("Error building conversation summary for conversation {}: {}",
                        conversation.getId(), e.getMessage());
            }
        }

        return summaries;
    }

    /**
     * Get conversation by ID
     */
    @Transactional(readOnly = true)
    public ConversationDTO getConversationById(UUID conversationId, UUID userId) {
        log.debug("Fetching conversation {} for user {}", conversationId, userId);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // Verify user is a participant
        if (!conversation.isParticipant(userId)) {
            throw new SecurityException("User is not a participant of this conversation");
        }

        UUID otherUserId = conversation.getOtherParticipant(userId);
        UserSummaryDTO otherUser = userServiceClient.getUserById(otherUserId);

        // Get last message
        Message lastMessage = messageRepository.findLastMessageByConversationId(conversationId)
                .orElse(null);

        MessageDTO lastMessageDTO = null;
        if (lastMessage != null) {
            lastMessageDTO = mapToMessageDTO(lastMessage);
        }

        return ConversationDTO.builder()
                .id(conversation.getId())
                .user1Id(conversation.getUser1Id())
                .user2Id(conversation.getUser2Id())
                .matchId(conversation.getMatchId())
                .createdAt(conversation.getCreatedAt())
                .lastMessageAt(conversation.getLastMessageAt())
                .archived(conversation.getArchived())
                .otherUser(otherUser)
                .lastMessage(lastMessageDTO)
                .build();
    }

    /**
     * Create conversation from a match
     */
    @Transactional
    public ConversationDTO createConversationFromMatch(UUID matchId, UUID userId) {
        log.info("Creating conversation from match: {}", matchId);

        // Check if conversation already exists for this match
        if (conversationRepository.existsByMatchId(matchId)) {
            throw new IllegalStateException("Conversation already exists for this match");
        }

        // Verify match exists and get participants
        MatchServiceClient.MatchDTO match = matchServiceClient.getMatchById(matchId);
        if (match == null) {
            throw new IllegalArgumentException("Match not found");
        }

        // Verify requesting user is part of the match
        if (!match.user1Id().equals(userId) && !match.user2Id().equals(userId)) {
            throw new SecurityException("User is not part of this match");
        }

        // Create conversation
        Conversation conversation = Conversation.builder()
                .user1Id(match.user1Id())
                .user2Id(match.user2Id())
                .matchId(matchId)
                .createdAt(LocalDateTime.now())
                .archived(false)
                .build();

        conversation = conversationRepository.save(conversation);
        log.info("Created conversation {} for match {}", conversation.getId(), matchId);

        return mapToConversationDTO(conversation, userId);
    }

    /**
     * Archive a conversation
     */
    @Transactional
    public void archiveConversation(UUID conversationId, UUID userId) {
        log.info("Archiving conversation {} for user {}", conversationId, userId);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // Verify user is a participant
        if (!conversation.isParticipant(userId)) {
            throw new SecurityException("User is not a participant of this conversation");
        }

        conversation.setArchived(true);
        conversationRepository.save(conversation);
    }

    /**
     * Update last message timestamp
     */
    @Transactional
    public void updateLastMessageTimestamp(UUID conversationId, LocalDateTime timestamp) {
        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            conversation.setLastMessageAt(timestamp);
            conversationRepository.save(conversation);
        });
    }

    /**
     * Verify user is participant of conversation
     */
    public boolean isUserParticipant(UUID conversationId, UUID userId) {
        return conversationRepository.findById(conversationId)
                .map(conversation -> conversation.isParticipant(userId))
                .orElse(false);
    }

    /**
     * Get other participant in conversation
     */
    public UUID getOtherParticipant(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        return conversation.getOtherParticipant(userId);
    }

    // Helper methods
    private ConversationDTO mapToConversationDTO(Conversation conversation, UUID currentUserId) {
        UUID otherUserId = conversation.getOtherParticipant(currentUserId);
        UserSummaryDTO otherUser = userServiceClient.getUserById(otherUserId);

        return ConversationDTO.builder()
                .id(conversation.getId())
                .user1Id(conversation.getUser1Id())
                .user2Id(conversation.getUser2Id())
                .matchId(conversation.getMatchId())
                .createdAt(conversation.getCreatedAt())
                .lastMessageAt(conversation.getLastMessageAt())
                .archived(conversation.getArchived())
                .otherUser(otherUser)
                .build();
    }

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
