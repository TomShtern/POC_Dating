package com.dating.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for a list of conversations.
 * Wraps the conversations array with metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationsListResponse {

    /**
     * List of conversations.
     */
    private List<ConversationResponse> conversations;

    /**
     * Total number of conversations returned.
     */
    private int total;
}
