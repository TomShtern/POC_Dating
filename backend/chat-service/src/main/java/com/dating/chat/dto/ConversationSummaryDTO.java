package com.dating.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for conversation list summary
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationSummaryDTO {

    private UUID id;
    private UUID matchId;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;

    // Other user information
    private UUID otherUserId;
    private String otherUserName;
    private String otherUserPhotoUrl;

    // Last message preview
    private String lastMessageContent;
    private LocalDateTime lastMessageTimestamp;
    private Boolean lastMessageFromMe;

    // Unread count
    private Long unreadCount;
}
