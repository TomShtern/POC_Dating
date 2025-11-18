package com.dating.recommendation.dto;

import com.dating.recommendation.model.User;

import java.util.Map;

/**
 * ============================================================================
 * SCORED CANDIDATE - RECOMMENDATION RESULT
 * ============================================================================
 *
 * PURPOSE:
 * Holds a candidate user along with their compatibility score and breakdown.
 * This record provides transparency into WHY a user was recommended.
 *
 * WHY TRANSPARENCY MATTERS:
 * - Users trust recommendations more when they understand the reasoning
 * - Developers can debug and tune the algorithm more easily
 * - Score breakdown can be shown in UI ("87% match - similar interests!")
 *
 * FIELDS:
 * - candidate: The potential match user
 * - finalScore: Overall compatibility score (0.0 to 1.0)
 * - scoreBreakdown: Individual scores from each scorer
 *
 * EXAMPLE BREAKDOWN:
 * {
 *   "age-compatibility": 1.0,      // Both in preferred age range
 *   "location-distance": 0.8,      // 10km apart (max 50km)
 *   "interest-overlap": 0.6,       // 3 shared interests out of 5
 *   "activity-level": 0.9,         // Active within last 3 days
 *   "gender-preference": 1.0       // Mutual gender preference match
 * }
 *
 * HOW TO USE IN UI:
 * - Show finalScore as "87% Match"
 * - Highlight top scoring factors: "You both like hiking and cooking!"
 * - Show weak factors as potential concerns (if desired)
 *
 * HOW TO MODIFY:
 * - Add more fields like "rank", "reason", "boosts"
 * - Add method to get top N scoring factors
 * - Add method to format score as percentage
 *
 * ============================================================================
 */
public record ScoredCandidate(
        /**
         * The candidate user being recommended.
         * Contains full user profile for display in UI.
         */
        User candidate,

        /**
         * Final compatibility score after aggregating all scorers.
         * Range: 0.0 (worst) to 1.0 (best)
         *
         * INTERPRETATION:
         * - 0.8-1.0: Excellent match (show at top of feed)
         * - 0.6-0.8: Good match (show in feed)
         * - 0.4-0.6: Moderate match (show if feed is low)
         * - 0.0-0.4: Poor match (usually filtered out)
         */
        double finalScore,

        /**
         * Individual scores from each scorer.
         * Map key: scorer name (e.g., "age-compatibility")
         * Map value: individual score (0.0 to 1.0)
         *
         * USES:
         * - Debugging: Why did this user score low?
         * - UI: "You both like hiking!" (high interest score)
         * - Analytics: Which factors matter most?
         */
        Map<String, Double> scoreBreakdown

) implements Comparable<ScoredCandidate> {

    /**
     * Compare by final score for sorting (highest first).
     *
     * This allows using Collections.sort() or Stream.sorted() directly.
     * Results are sorted in descending order (best matches first).
     *
     * EXAMPLE:
     * List<ScoredCandidate> recommendations = ...;
     * Collections.sort(recommendations); // Highest score first
     */
    @Override
    public int compareTo(ScoredCandidate other) {
        // Descending order: higher scores first
        return Double.compare(other.finalScore, this.finalScore);
    }

    /**
     * Get the final score as a percentage (0-100).
     * Useful for UI display.
     *
     * @return Score as percentage, e.g., 85 for 85%
     */
    public int getScorePercentage() {
        return (int) Math.round(finalScore * 100);
    }

    /**
     * Check if this candidate meets minimum score threshold.
     *
     * @param threshold Minimum score required (typically 0.3-0.5)
     * @return true if finalScore >= threshold
     */
    public boolean meetsThreshold(double threshold) {
        return finalScore >= threshold;
    }
}
