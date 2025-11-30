package com.dating.chat.service;

import com.dating.chat.dto.websocket.ChatMessageEvent;
import com.dating.chat.dto.websocket.MessageType;
import com.dating.chat.exception.ConversationNotFoundException;
import com.dating.chat.model.Conversation;
import com.dating.chat.model.Message;
import com.dating.chat.repository.ConversationRepository;
import com.dating.chat.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    private UUID userId;
    private UUID otherUserId;
    private UUID matchId;
    private UUID conversationId;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        matchId = UUID.randomUUID();
        conversationId = UUID.randomUUID();

        // Ensure sorted order
        if (userId.compareTo(otherUserId) > 0) {
            UUID temp = userId;
            userId = otherUserId;
            otherUserId = temp;
        }

        conversation = Conversation.builder()
                .id(conversationId)
                .matchId(matchId)
                .user1Id(userId)
                .user2Id(otherUserId)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void isUserInMatch_shouldReturnTrueWhenUserIsInMatch() {
        when(conversationRepository.isUserInMatch(userId, matchId)).thenReturn(true);

        boolean result = chatMessageService.isUserInMatch(userId, matchId);

        assertThat(result).isTrue();
        verify(conversationRepository).isUserInMatch(userId, matchId);
    }

    @Test
    void isUserInMatch_shouldReturnFalseWhenUserIsNotInMatch() {
        UUID unknownUserId = UUID.randomUUID();
        when(conversationRepository.isUserInMatch(unknownUserId, matchId)).thenReturn(false);

        boolean result = chatMessageService.isUserInMatch(unknownUserId, matchId);

        assertThat(result).isFalse();
    }

    @Test
    void getOtherUserId_shouldReturnOtherUserIdForUser1() {
        when(conversationRepository.findByMatchId(matchId)).thenReturn(Optional.of(conversation));

        UUID result = chatMessageService.getOtherUserId(matchId, userId);

        assertThat(result).isEqualTo(otherUserId);
    }

    @Test
    void getOtherUserId_shouldReturnOtherUserIdForUser2() {
        when(conversationRepository.findByMatchId(matchId)).thenReturn(Optional.of(conversation));

        UUID result = chatMessageService.getOtherUserId(matchId, otherUserId);

        assertThat(result).isEqualTo(userId);
    }

    @Test
    void getOtherUserId_shouldThrowExceptionWhenConversationNotFound() {
        when(conversationRepository.findByMatchId(matchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatMessageService.getOtherUserId(matchId, userId))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    @Test
    void saveMessage_shouldSaveAndReturnChatMessageEvent() {
        String content = "Hello!";
        String senderName = "TestUser";

        when(conversationRepository.findByMatchId(matchId)).thenReturn(Optional.of(conversation));

        Message savedMessage = Message.builder()
                .id(UUID.randomUUID())
                .matchId(matchId)
                .senderId(userId)
                .senderName(senderName)
                .content(content)
                .type(MessageType.TEXT)
                .createdAt(Instant.now())
                .build();

        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        ChatMessageEvent result = chatMessageService.saveMessage(
                matchId, userId, senderName, content, MessageType.TEXT
        );

        assertThat(result).isNotNull();
        assertThat(result.messageId()).isEqualTo(savedMessage.getId());
        assertThat(result.matchId()).isEqualTo(matchId);
        assertThat(result.senderId()).isEqualTo(userId);
        assertThat(result.senderName()).isEqualTo(senderName);
        assertThat(result.content()).isEqualTo(content);
        assertThat(result.type()).isEqualTo(MessageType.TEXT);

        verify(messageRepository).save(any(Message.class));
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void saveMessage_shouldThrowExceptionWhenConversationNotFound() {
        when(conversationRepository.findByMatchId(matchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatMessageService.saveMessage(
                matchId, userId, "Test", "Hello", MessageType.TEXT
        ))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    @Test
    void markMessagesAsRead_shouldUpdateMessages() {
        UUID lastReadMessageId = UUID.randomUUID();
        when(messageRepository.markAllAsRead(any(), any(), any())).thenReturn(5);

        chatMessageService.markMessagesAsRead(matchId, userId, lastReadMessageId);

        verify(messageRepository).markAllAsRead(any(), any(), any());
    }

    @Test
    void getOrCreateConversation_shouldReturnExistingConversation() {
        when(conversationRepository.findByMatchId(matchId)).thenReturn(Optional.of(conversation));

        Conversation result = chatMessageService.getOrCreateConversation(matchId, userId, otherUserId);

        assertThat(result).isEqualTo(conversation);
        verify(conversationRepository).findByMatchId(matchId);
    }

    @Test
    void getOrCreateConversation_shouldCreateNewConversation() {
        when(conversationRepository.findByMatchId(matchId)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        Conversation result = chatMessageService.getOrCreateConversation(matchId, userId, otherUserId);

        assertThat(result).isNotNull();
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void countUnreadMessages_shouldReturnCount() {
        when(messageRepository.countUnreadByMatchIdAndUserId(conversationId, userId)).thenReturn(3L);

        long result = chatMessageService.countUnreadMessages(conversationId, userId);

        assertThat(result).isEqualTo(3L);
    }
}
