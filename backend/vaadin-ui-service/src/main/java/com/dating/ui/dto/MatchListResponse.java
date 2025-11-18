package com.dating.ui.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for paginated list of matches.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchListResponse {
    private List<Match> matches;
    private long total;
    private boolean hasMore;
}
