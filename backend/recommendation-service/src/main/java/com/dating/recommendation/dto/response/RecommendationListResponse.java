package com.dating.recommendation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * DTO for a list of recommendations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * List of recommendations.
     */
    private List<RecommendationResponse> recommendations;

    /**
     * Total number of available recommendations.
     */
    private long total;

    /**
     * Page size.
     */
    private int limit;

    /**
     * Current offset.
     */
    private int offset;

    /**
     * Whether there are more recommendations available.
     */
    private boolean hasMore;

    /**
     * Timestamp when recommendations were generated.
     */
    private Instant generatedAt;
}
