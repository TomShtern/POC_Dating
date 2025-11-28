package com.dating.recommendation.scorer;

import com.dating.recommendation.model.User;

/**
 * ============================================================================
 * COMPATIBILITY SCORER INTERFACE
 * ============================================================================
 *
 * PURPOSE:
 * Contract that all scoring components must implement. This interface enables
 * the pluggable architecture of the recommendation system.
 *
 * HOW THE SYSTEM WORKS:
 * 1. Each scoring factor (age, location, interests, etc.) is a separate class
 * 2. Each class implements this interface
 * 3. The ScoreAggregator automatically collects all implementations
 * 4. Scores are combined using weighted average
 *
 * HOW TO ADD A NEW SCORER:
 * 1. Create a new class that implements this interface
 * 2. Add @Component annotation to register it as a Spring bean
 * 3. Add weight configuration to application.yml
 * 4. The aggregator will automatically discover and use it
 *
 * EXAMPLE:
 * <pre>
 * {@code
 * @Component
 * public class ProfileCompletenessScorer implements CompatibilityScorer {
 *     @Value("${recommendation.scorers.completeness.weight:0.1}")
 *     private double weight;
 *
 *     @Override
 *     public double calculateScore(User user, User candidate) {
 *         int filledFields = countFilledFields(candidate);
 *         return filledFields / 10.0; // Assuming 10 possible fields
 *     }
 *
 *     @Override
 *     public double getWeight() { return weight; }
 *
 *     @Override
 *     public String getName() { return "profile-completeness"; }
 * }
 * }
 * </pre>
 *
 * SCORING PHILOSOPHY:
 * - Scores must be between 0.0 (no compatibility) and 1.0 (perfect compatibility)
 * - 0.5 represents neutral/average compatibility
 * - Weights determine relative importance (configured in application.yml)
 * - Missing data should return 0.5 (neutral), not 0.0
 *
 * ============================================================================
 */
public interface CompatibilityScorer {

    /**
     * Calculate compatibility score between two users.
     *
     * IMPLEMENTATION REQUIREMENTS:
     * - Must return a value between 0.0 and 1.0
     * - Must handle null values gracefully (return 0.5 for neutral)
     * - Must be deterministic (same inputs = same output)
     * - Should be fast (called thousands of times per recommendation batch)
     *
     * @param user      The user requesting recommendations (the "seeker")
     * @param candidate A potential match candidate (being evaluated)
     * @return Score between 0.0 (no match) and 1.0 (perfect match)
     *
     * @example
     * // Age scorer returns 1.0 if both users are in each other's preferred range
     * // Location scorer returns 1.0 if distance is 0km, 0.0 if beyond max distance
     * // Interest scorer returns Jaccard similarity coefficient
     */
    double calculateScore(User user, User candidate);

    /**
     * Get the weight of this scorer in the final calculation.
     *
     * PURPOSE:
     * Weights determine how much this factor affects the final score.
     * Higher weight = more influence on recommendations.
     *
     * CONFIGURATION:
     * Weights are typically loaded from application.yml using @Value annotation.
     * This allows easy tuning without code changes.
     *
     * IMPORTANT NOTES:
     * - Weights don't need to sum to 1.0 (they're normalized by aggregator)
     * - Setting weight to 0 effectively disables this scorer
     * - Typical range: 0.05 (minor factor) to 0.5 (major factor)
     *
     * @return Weight between 0.0 and 1.0 (or higher if you want extreme emphasis)
     *
     * @example
     * // Age compatibility might have weight 0.2 (20% of total score)
     * // Location might have weight 0.3 (30% of total score)
     */
    double getWeight();

    /**
     * Get the name of this scorer for logging and debugging.
     *
     * PURPOSE:
     * - Appears in logs for debugging
     * - Appears in score breakdown returned to users (transparency)
     * - Used as cache key prefix if scorer has its own caching
     *
     * NAMING CONVENTIONS:
     * - Use lowercase with hyphens: "age-compatibility", "location-distance"
     * - Keep it short but descriptive
     * - Must be unique across all scorers
     *
     * @return Human-readable name for this scorer
     */
    String getName();
}
