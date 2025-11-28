package com.dating.recommendation.scorer;

import com.dating.recommendation.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * ============================================================================
 * INTEREST SCORER
 * ============================================================================
 *
 * PURPOSE:
 * Scores candidates based on shared interests with the user.
 * More shared interests = higher compatibility score.
 *
 * HOW IT WORKS:
 * 1. Get both users' interest sets
 * 2. Calculate Jaccard similarity coefficient
 * 3. Return similarity as score
 *
 * JACCARD SIMILARITY:
 * Formula: |A ∩ B| / |A ∪ B|
 * - Intersection: interests both users share
 * - Union: all unique interests from both users
 * - Result: 0.0 (no overlap) to 1.0 (identical interests)
 *
 * WHY JACCARD:
 * - Standard similarity metric in set theory
 * - Handles different set sizes fairly
 * - Easy to understand and explain
 * - 0-1 range matches our scoring system
 *
 * EXAMPLE:
 * User interests: [hiking, cooking, reading]
 * Candidate interests: [hiking, cooking, movies]
 * Intersection: [hiking, cooking] (2)
 * Union: [hiking, cooking, reading, movies] (4)
 * Jaccard = 2/4 = 0.5
 *
 * WHY THIS MATTERS:
 * - Shared interests provide conversation starters
 * - Common activities to do together
 * - Similar lifestyles suggest compatibility
 *
 * HOW TO MODIFY:
 * - Weighted interests: Give some interests higher importance
 * - Minimum threshold: Return 0 if shared < 3 interests
 * - Category matching: Match by category (sports, arts) not exact
 * - NLP matching: Use word embeddings to match "running" with "jogging"
 *
 * CONFIGURATION:
 * application.yml:
 *   recommendation:
 *     scorers:
 *       interests:
 *         weight: 0.25            # 25% of total score
 *         minimum-shared: 0       # Minimum shared interests required
 *
 * ============================================================================
 */
@Component
@Slf4j
public class InterestScorer implements CompatibilityScorer {

    // -------------------------------------------------------------------------
    // CONFIGURATION
    // -------------------------------------------------------------------------

    /**
     * Weight of this scorer in the final calculation.
     * Default: 0.25 (interests are 25% of total score)
     *
     * TO ADJUST: Change 'recommendation.scorers.interests.weight' in application.yml
     */
    @Value("${recommendation.scorers.interests.weight:0.25}")
    private double weight;

    /**
     * Minimum shared interests required for a non-zero score.
     * Default: 0 (any overlap counts)
     *
     * TO USE:
     * Set this to require a minimum number of shared interests.
     * If shared < minimum, score will be 0.
     */
    @Value("${recommendation.scorers.interests.minimum-shared:0}")
    private int minimumShared;

    /**
     * Calculate interest-based compatibility score.
     *
     * ALGORITHM:
     * 1. Get interest sets for both users
     * 2. Handle edge cases (empty sets)
     * 3. Calculate Jaccard similarity
     * 4. Apply minimum threshold if configured
     *
     * @param user      The user requesting recommendations
     * @param candidate A potential match candidate
     * @return Score: 0.0 (no shared interests) to 1.0 (identical interests)
     */
    @Override
    public double calculateScore(User user, User candidate) {
        // =====================================================================
        // STEP 1: Get interest sets
        // =====================================================================
        Set<String> userInterests = user.getInterests();
        Set<String> candidateInterests = candidate.getInterests();

        // =====================================================================
        // STEP 2: Handle edge cases
        // =====================================================================
        // If either user has no interests, return neutral score
        // We don't want to penalize users who haven't filled in interests yet
        //
        // WHY 0.5: Neutral score means this factor won't push ranking up or down
        // ALTERNATIVE: Return 0.0 to penalize incomplete profiles
        // =====================================================================
        if (userInterests == null || userInterests.isEmpty() ||
            candidateInterests == null || candidateInterests.isEmpty()) {
            log.trace("Missing interests: user has {}, candidate has {}. Returning neutral score.",
                    userInterests != null ? userInterests.size() : 0,
                    candidateInterests != null ? candidateInterests.size() : 0);
            return 0.5; // Neutral score for missing data
        }

        // =====================================================================
        // STEP 3: Calculate intersection (shared interests)
        // =====================================================================
        // Create new set to avoid modifying the original
        Set<String> intersection = new HashSet<>(userInterests);
        intersection.retainAll(candidateInterests);

        int sharedCount = intersection.size();

        // =====================================================================
        // STEP 4: Check minimum threshold
        // =====================================================================
        // If configured, require minimum number of shared interests
        if (sharedCount < minimumShared) {
            log.trace("Not enough shared interests: {} < {}. Returning 0.",
                    sharedCount, minimumShared);
            return 0.0;
        }

        // =====================================================================
        // STEP 5: Calculate union (all unique interests)
        // =====================================================================
        Set<String> union = new HashSet<>(userInterests);
        union.addAll(candidateInterests);

        int unionCount = union.size();

        // =====================================================================
        // STEP 6: Calculate Jaccard similarity
        // =====================================================================
        // FORMULA: |intersection| / |union|
        //
        // PROPERTIES:
        // - Always between 0 and 1
        // - 0 = no overlap
        // - 1 = identical sets
        // - Symmetric: J(A,B) = J(B,A)
        // - Penalizes very large sets with few matches
        //
        // ALTERNATIVE FORMULAS:
        //
        // Sørensen-Dice coefficient (more weight on matches):
        // double dice = 2.0 * sharedCount / (userInterests.size() + candidateInterests.size());
        //
        // Overlap coefficient (lenient for different sizes):
        // double overlap = (double) sharedCount / Math.min(userInterests.size(), candidateInterests.size());
        //
        // Simple ratio:
        // double ratio = (double) sharedCount / userInterests.size(); // Fraction of user's interests matched
        // =====================================================================
        double jaccardSimilarity = (double) sharedCount / unionCount;

        log.trace("Interest score: {} shared out of {} union = {}. Shared: {}",
                sharedCount, unionCount, String.format("%.3f", jaccardSimilarity), intersection);

        return jaccardSimilarity;
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public String getName() {
        return "interest-overlap";
    }
}
