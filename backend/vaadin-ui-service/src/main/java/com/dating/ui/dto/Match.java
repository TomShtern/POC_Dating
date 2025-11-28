package com.dating.ui.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotBlank(message = "Match ID is required")
    private String id;

    @NotBlank(message = "User 1 ID is required")
    private String user1Id;

    @NotBlank(message = "User 2 ID is required")
    private String user2Id;

    @NotNull(message = "Other user information is required")
    @Valid
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
