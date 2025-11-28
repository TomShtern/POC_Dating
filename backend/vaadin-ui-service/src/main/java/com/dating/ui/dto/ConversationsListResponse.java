package com.dating.ui.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for a list of conversations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationsListResponse {
    private List<Conversation> conversations;
    private int total;
    private int limit;
    private int offset;
    private boolean hasMore;
}
