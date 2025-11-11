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
public class Conversation {
    private String id;
    private String matchId;
    private User otherUser;
    private Message lastMessage;
    private int unreadCount;
    private LocalDateTime createdAt;
}
