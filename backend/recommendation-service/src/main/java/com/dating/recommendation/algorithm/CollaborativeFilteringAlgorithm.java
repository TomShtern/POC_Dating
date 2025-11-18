package com.dating.recommendation.algorithm;

import com.dating.recommendation.dto.UserProfileDto;
import com.dating.recommendation.dto.response.ScoreFactors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Collaborative filtering scoring algorithm (v2) - STUB.
 * Future implementation for "users like you also liked..." recommendations.
 */
@Component
@Slf4j
public class CollaborativeFilteringAlgorithm implements ScoringAlgorithm {

    private static final String VERSION = "v2";
    private static final String DESCRIPTION = "Collaborative filtering (stub)";

    @Override
    public int calculateScore(UserProfileDto sourceUser, UserProfileDto targetUser) {
        // Stub implementation - returns a neutral score
        log.debug("Collaborative filtering not yet implemented, returning neutral score");
        return 50;
    }

    @Override
    public ScoreFactors calculateFactors(UserProfileDto sourceUser, UserProfileDto targetUser) {
        // Stub implementation - returns neutral factors
        return ScoreFactors.builder()
                .profileCompleteness(5.0)
                .preferenceMatch(20.0)
                .activity(10.0)
                .mlPrediction(15.0)
                .interestMatch("N/A")
                .ageCompatibility("N/A")
                .preferenceAlignment("N/A")
                .build();
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
