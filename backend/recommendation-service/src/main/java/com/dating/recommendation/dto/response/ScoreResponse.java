package com.dating.recommendation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * DTO for compatibility score between two users.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Overall compatibility score (0-100).
     */
    private int score;

    /**
     * Breakdown of score factors.
     */
    private Map<String, Integer> factors;

    /**
     * Timestamp when score was calculated.
     */
    private Instant calculatedAt;
}
