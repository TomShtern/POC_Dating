package com.dating.ui.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a list of messages with pagination metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageListResponse {
    private UUID conversationId;
    private List<Message> messages;
    private int total;
    private int limit;
    private int offset;
    private boolean hasMore;
}
