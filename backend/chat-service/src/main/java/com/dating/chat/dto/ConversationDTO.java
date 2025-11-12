package com.dating.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for full conversation details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDTO {

    private UUID id;
    private UUID user1Id;
    private UUID user2Id;
    private UUID matchId;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
    private Boolean archived;

    // Optional: User details from user-service
    private UserSummaryDTO otherUser;

    // Optional: Last message preview
    private MessageDTO lastMessage;
}
