package com.dating.recommendation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for batch scoring response (internal API for match-service).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchScoreResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Map of candidate user ID to their score.
     */
    private Map<UUID, Integer> scores;

    /**
     * Map of candidate user ID to score factors.
     */
    private Map<UUID, ScoreFactors> factors;
}
