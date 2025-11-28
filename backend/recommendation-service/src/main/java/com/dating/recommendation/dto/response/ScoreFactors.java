package com.dating.recommendation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO representing the breakdown of a compatibility score.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreFactors implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Profile completeness score (0-10).
     */
    private double profileCompleteness;

    /**
     * Preference match score (0-40).
     */
    private double preferenceMatch;

    /**
     * Activity score (0-20).
     */
    private double activity;

    /**
     * ML prediction score (0-30).
     */
    private double mlPrediction;

    /**
     * Interest match description (e.g., "8/10").
     */
    private String interestMatch;

    /**
     * Age compatibility description (e.g., "Perfect", "Good").
     */
    private String ageCompatibility;

    /**
     * Preference alignment description (e.g., "High", "Medium").
     */
    private String preferenceAlignment;

    /**
     * Calculate total score from all factors.
     */
    public double getTotalScore() {
        return profileCompleteness + preferenceMatch + activity + mlPrediction;
    }
}
