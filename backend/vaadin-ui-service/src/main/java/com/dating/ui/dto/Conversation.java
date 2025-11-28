package com.dating.ui.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
public class Conversation {
    @NotBlank(message = "Conversation ID is required")
    private String id;

    @NotBlank(message = "Match ID is required")
    private String matchId;

    @NotNull(message = "Other user information is required")
    @Valid
    private User otherUser;

    @Valid
    private Message lastMessage;

    @Min(value = 0, message = "Unread count cannot be negative")
    private int unreadCount;

    private LocalDateTime createdAt;
}
