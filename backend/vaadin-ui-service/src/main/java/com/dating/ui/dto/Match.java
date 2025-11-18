package com.dating.ui.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Match {
    private String id;
    private String user1Id;
    private String user2Id;
    private User otherUser;
    private String conversationId;
    private LocalDateTime createdAt;
    private boolean hasUnreadMessages;

    /**
     * Alias for otherUser for compatibility
     */
    public User getMatchedUser() {
        return otherUser;
    }
}
