package com.dating.recommendation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for a single recommendation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Recommendation ID.
     */
    private UUID id;

    /**
     * Recommended user's basic profile.
     */
    private RecommendedUserResponse recommendedUser;

    /**
     * Compatibility score (0-100).
     */
    private int score;

    /**
     * Breakdown of score factors.
     */
    private ScoreFactors scoreFactors;

    /**
     * Human-readable reason for the recommendation.
     */
    private String reason;
}
