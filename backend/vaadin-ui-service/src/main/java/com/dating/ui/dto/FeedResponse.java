package com.dating.ui.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for feed of potential matches.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedResponse {
    private List<User> profiles;
    private long total;
    private boolean hasMore;
}
