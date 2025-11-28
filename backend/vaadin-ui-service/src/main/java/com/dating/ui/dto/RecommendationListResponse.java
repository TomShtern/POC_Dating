package com.dating.ui.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO for a list of recommendations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationListResponse {
    private List<RecommendationResponse> recommendations;
    private long total;
    private boolean hasMore;
    private Instant generatedAt;
}
