package com.dating.recommendation.algorithm;

import com.dating.recommendation.dto.UserProfileDto;
import com.dating.recommendation.dto.response.ScoreFactors;

/**
 * Strategy interface for scoring algorithms.
 * Allows different algorithm implementations to be swapped.
 */
public interface ScoringAlgorithm {

    /**
     * Calculate compatibility score between two users.
     *
     * @param sourceUser The user requesting recommendations
     * @param targetUser The candidate user being scored
     * @return Compatibility score (0-100)
     */
    int calculateScore(UserProfileDto sourceUser, UserProfileDto targetUser);

    /**
     * Calculate detailed score factors between two users.
     *
     * @param sourceUser The user requesting recommendations
     * @param targetUser The candidate user being scored
     * @return Detailed breakdown of score factors
     */
    ScoreFactors calculateFactors(UserProfileDto sourceUser, UserProfileDto targetUser);

    /**
     * Get the algorithm version identifier.
     *
     * @return Algorithm version (e.g., "v1", "v2")
     */
    String getVersion();

    /**
     * Get a human-readable description of the algorithm.
     *
     * @return Algorithm description
     */
    String getDescription();
}
