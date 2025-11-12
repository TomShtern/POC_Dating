package com.dating.chat.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for marking messages as read
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkAsReadRequest {

    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;

    @NotEmpty(message = "At least one message ID is required")
    private List<UUID> messageIds;
}
