package com.dating.recommendation.service;

import com.dating.recommendation.dto.CandidateProfileDTO;
import com.dating.recommendation.dto.ScoredCandidate;
import com.dating.recommendation.model.User;
import com.dating.recommendation.scorer.CompatibilityScorer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * SCORE AGGREGATOR
 * ============================================================================
 *
 * PURPOSE:
 * Combines scores from all registered scorers into a final compatibility score.
 * This is the heart of the recommendation algorithm.
 *
 * HOW IT WORKS:
 * 1. Spring automatically injects ALL beans that implement CompatibilityScorer
 * 2. For each candidate, calls each scorer to get individual scores
 * 3. Multiplies each score by its weight
 * 4. Sums weighted scores and normalizes to 0.0-1.0 range
 *
 * FORMULA:
 * finalScore = (score1 * weight1 + score2 * weight2 + ...) / totalWeight
 *
 * EXAMPLE:
 * If you have 3 scorers with weights 0.3, 0.4, 0.3:
 * - Age score: 1.0 * 0.3 = 0.30
 * - Location score: 0.8 * 0.4 = 0.32
 * - Interest score: 0.6 * 0.3 = 0.18
 * - Total = 0.80 / 1.0 = 0.80 (80% match)
 *
 * HOW TO MODIFY AGGREGATION:
 * - Weighted average (current): Good for balanced scoring
 * - Minimum score: finalScore = Math.min(scores) - strict matching
 * - Maximum score: finalScore = Math.max(scores) - lenient matching
 * - Geometric mean: Good when all factors must be non-zero
 *
 * HOW TO ADD A NEW SCORER:
 * 1. Create a @Component class that implements CompatibilityScorer
 * 2. It will automatically be picked up by Spring's dependency injection
 * 3. No changes needed to this class!
 *
 * HOW TO REMOVE A SCORER:
 * - Delete the class file, OR
 * - Remove @Component annotation, OR
 * - Set its weight to 0 in application.yml
 *
 * ============================================================================
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScoreAggregator {

    // -------------------------------------------------------------------------
    // INJECTED SCORERS
    // -------------------------------------------------------------------------
    // Spring automatically injects ALL beans that implement CompatibilityScorer.
    // This is the magic that makes the system pluggable!
    //
    // TO ADD A NEW SCORER: Just create a new @Component class.
    // TO REMOVE A SCORER: Delete the class or set its weight to 0.
    // -------------------------------------------------------------------------
    private final List<CompatibilityScorer> scorers;

    /**
     * Calculate the final compatibility score between two users.
     *
     * ALGORITHM:
     * 1. Loop through all registered scorers
     * 2. Call each scorer to get individual score
     * 3. Multiply by weight and add to sum
     * 4. Normalize by total weight
     *
     * PERFORMANCE:
     * - O(n) where n = number of scorers
     * - Each scorer should be O(1) or O(k) where k is small (interests count)
     * - Total time should be <1ms per candidate
     *
     * @param user      The user requesting recommendations
     * @param candidate A potential match candidate
     * @return ScoredCandidate with final score and breakdown
     */
    public ScoredCandidate aggregate(User user, User candidate) {
        // =====================================================================
        // STEP 1: Initialize accumulators
        // =====================================================================
        // Map to store individual scores for transparency/debugging
        Map<String, Double> individualScores = new HashMap<>();

        // Weighted sum accumulator
        double weightedSum = 0.0;

        // Total weight accumulator (for normalization)
        double totalWeight = 0.0;

        // =====================================================================
        // STEP 2: Calculate score from each registered scorer
        // =====================================================================
        for (CompatibilityScorer scorer : scorers) {
            // -----------------------------------------------------------------
            // Skip disabled scorers (weight = 0)
            // This allows disabling scorers via configuration without removing code
            // -----------------------------------------------------------------
            if (scorer.getWeight() <= 0) {
                log.trace("Skipping scorer '{}' (weight = 0)", scorer.getName());
                continue;
            }

            // -----------------------------------------------------------------
            // Calculate individual score from this scorer
            // Score should be between 0.0 and 1.0
            // -----------------------------------------------------------------
            double score = scorer.calculateScore(user, candidate);

            // Validate score is in expected range (defensive programming)
            if (score < 0.0 || score > 1.0) {
                log.warn("Scorer '{}' returned invalid score {} for user {} and candidate {}. " +
                         "Clamping to [0, 1] range.",
                         scorer.getName(), score, user.getId(), candidate.getId());
                score = Math.max(0.0, Math.min(1.0, score));
            }

            double weight = scorer.getWeight();

            // -----------------------------------------------------------------
            // Store for transparency (returned in ScoredCandidate)
            // -----------------------------------------------------------------
            individualScores.put(scorer.getName(), score);

            // -----------------------------------------------------------------
            // Add to weighted sum
            // -----------------------------------------------------------------
            weightedSum += score * weight;
            totalWeight += weight;

            // Debug logging (only enabled at DEBUG level)
            log.debug("Scorer '{}': score={}, weight={}, contribution={}",
                    scorer.getName(),
                    String.format("%.3f", score),
                    String.format("%.2f", weight),
                    String.format("%.3f", score * weight));
        }

        // =====================================================================
        // STEP 3: Normalize to 0.0-1.0 range
        // =====================================================================
        // WHY NORMALIZE: Weights might not sum to 1.0. User can configure
        // any weight values (e.g., 0.1, 0.2, 0.15). We normalize so the
        // final score is always in the 0-1 range.
        //
        // EXAMPLE:
        // - Total weighted sum: 0.65
        // - Total weight: 0.8 (weights didn't sum to 1.0)
        // - Normalized score: 0.65 / 0.8 = 0.8125
        // =====================================================================
        double finalScore = totalWeight > 0 ? weightedSum / totalWeight : 0.0;

        // =====================================================================
        // STEP 4: Log summary and return result
        // =====================================================================
        log.debug("Final score for candidate {}: {} (from {} active scorers)",
                candidate.getId(),
                String.format("%.3f", finalScore),
                individualScores.size());

        // Return result with full breakdown for transparency
        // Convert User to CandidateProfileDTO to avoid exposing sensitive internal fields
        return new ScoredCandidate(CandidateProfileDTO.fromUser(candidate), finalScore, individualScores);
    }

    /**
     * Get list of all registered scorers.
     * Useful for debugging and admin endpoints.
     *
     * @return List of scorer names and weights
     */
    public Map<String, Double> getScorerWeights() {
        Map<String, Double> weights = new HashMap<>();
        for (CompatibilityScorer scorer : scorers) {
            weights.put(scorer.getName(), scorer.getWeight());
        }
        return weights;
    }

    /**
     * Get count of active scorers (weight > 0).
     *
     * @return Number of active scorers
     */
    public int getActiveScorerCount() {
        return (int) scorers.stream()
                .filter(s -> s.getWeight() > 0)
                .count();
    }
}
