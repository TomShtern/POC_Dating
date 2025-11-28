package com.dating.recommendation.service;

import com.dating.recommendation.algorithm.CollaborativeFilteringAlgorithm;
import com.dating.recommendation.algorithm.RuleBasedScoringAlgorithm;
import com.dating.recommendation.algorithm.ScoringAlgorithm;
import com.dating.recommendation.dto.UserProfileDto;
import com.dating.recommendation.dto.response.BatchScoreResponse;
import com.dating.recommendation.dto.response.ScoreFactors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for scoring user compatibility.
 * Uses Strategy pattern to support multiple algorithms.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScoringService {

    private final RuleBasedScoringAlgorithm ruleBasedAlgorithm;
    private final CollaborativeFilteringAlgorithm collaborativeAlgorithm;

    /**
     * Calculate compatibility score between two users.
     *
     * @param sourceUser Source user profile
     * @param targetUser Target user profile
     * @param algorithmVersion Algorithm version to use
     * @return Compatibility score (0-100)
     */
    public int calculateScore(UserProfileDto sourceUser, UserProfileDto targetUser, String algorithmVersion) {
        ScoringAlgorithm algorithm = getAlgorithm(algorithmVersion);
        int score = algorithm.calculateScore(sourceUser, targetUser);

        log.debug("Calculated score {} between users {} and {} using algorithm {}",
                score, sourceUser.getId(), targetUser.getId(), algorithmVersion);

        return score;
    }

    /**
     * Calculate detailed score factors between two users.
     *
     * @param sourceUser Source user profile
     * @param targetUser Target user profile
     * @param algorithmVersion Algorithm version to use
     * @return Detailed score factors
     */
    public ScoreFactors calculateFactors(UserProfileDto sourceUser, UserProfileDto targetUser, String algorithmVersion) {
        ScoringAlgorithm algorithm = getAlgorithm(algorithmVersion);
        return algorithm.calculateFactors(sourceUser, targetUser);
    }

    /**
     * Score multiple candidates for a user.
     *
     * @param sourceUser Source user profile
     * @param candidates List of candidate profiles
     * @param algorithmVersion Algorithm version to use
     * @return Batch score response with scores and factors
     */
    public BatchScoreResponse scoreMultiple(
            UserProfileDto sourceUser,
            List<UserProfileDto> candidates,
            String algorithmVersion) {

        ScoringAlgorithm algorithm = getAlgorithm(algorithmVersion);

        Map<UUID, Integer> scores = new HashMap<>();
        Map<UUID, ScoreFactors> factors = new HashMap<>();

        for (UserProfileDto candidate : candidates) {
            int score = algorithm.calculateScore(sourceUser, candidate);
            ScoreFactors factorBreakdown = algorithm.calculateFactors(sourceUser, candidate);

            scores.put(candidate.getId(), score);
            factors.put(candidate.getId(), factorBreakdown);
        }

        log.debug("Scored {} candidates for user {} using algorithm {}",
                candidates.size(), sourceUser.getId(), algorithmVersion);

        return BatchScoreResponse.builder()
                .scores(scores)
                .factors(factors)
                .build();
    }

    /**
     * Get the appropriate scoring algorithm based on version.
     *
     * @param version Algorithm version (default: v1)
     * @return Scoring algorithm implementation
     */
    private ScoringAlgorithm getAlgorithm(String version) {
        if (version == null) {
            return ruleBasedAlgorithm;
        }

        return switch (version.toLowerCase()) {
            case "v2", "collaborative" -> collaborativeAlgorithm;
            default -> ruleBasedAlgorithm;
        };
    }

    /**
     * Generate a human-readable reason for a recommendation.
     *
     * @param factors Score factors
     * @param sourceUser Source user
     * @param targetUser Target user
     * @return Recommendation reason string
     */
    public String generateReason(ScoreFactors factors, UserProfileDto sourceUser, UserProfileDto targetUser) {
        if (sourceUser.getInterests() != null && targetUser.getInterests() != null) {
            long sharedCount = sourceUser.getInterests().stream()
                    .filter(interest -> targetUser.getInterests().contains(interest))
                    .count();

            if (sharedCount > 0) {
                return String.format("Shares %d interests with you", sharedCount);
            }
        }

        if ("Perfect".equals(factors.getAgeCompatibility())) {
            return "Perfect age match";
        }

        if ("High".equals(factors.getPreferenceAlignment())) {
            return "Great preference alignment";
        }

        return "Recommended based on your preferences";
    }
}
