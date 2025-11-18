package com.dating.recommendation.scorer;

import com.dating.recommendation.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * ============================================================================
 * GENDER PREFERENCE SCORER
 * ============================================================================
 *
 * PURPOSE:
 * Scores candidates based on mutual gender preference matching.
 * This is typically a hard filter - if genders don't match, score is 0.
 *
 * HOW IT WORKS:
 * 1. Check if candidate's gender is in user's preferences
 * 2. Check if user's gender is in candidate's preferences
 * 3. Return 1.0 for mutual match, 0.0 otherwise
 *
 * WHY BINARY SCORING:
 * Unlike age or distance, gender preference is usually non-negotiable.
 * A straight man doesn't want to see other men, regardless of other factors.
 *
 * GENDER VALUES:
 * - MALE
 * - FEMALE
 * - OTHER (non-binary, etc.)
 *
 * PREFERENCE VALUES:
 * - Single gender: {"MALE"} or {"FEMALE"}
 * - Multiple: {"MALE", "FEMALE"} (bisexual)
 * - All: {"MALE", "FEMALE", "OTHER"}
 *
 * WHY THIS MATTERS:
 * - Fundamental to dating app experience
 * - Wrong matches waste everyone's time
 * - Users expect this to be respected perfectly
 *
 * HOW TO MODIFY:
 * - Strict mode (current): Both must match, binary score
 * - User-priority: Only check user's preference (candidate sees everyone)
 * - Soft matching: 0.5 for one-way match (show but rank lower)
 *
 * CONFIGURATION:
 * application.yml:
 *   recommendation:
 *     scorers:
 *       gender:
 *         weight: 0.1  # 10% of total score (but effectively a filter)
 *
 * IMPORTANT NOTE ON WEIGHT:
 * Even with low weight (0.1), a score of 0.0 will significantly drag down
 * the final score. This effectively makes it a filter rather than a factor.
 *
 * To make it a true filter, consider:
 * - Pre-filtering in repository query (more efficient)
 * - Higher weight (0.5+) to ensure 0 score = low total
 * - Separate filtering step before scoring
 *
 * ============================================================================
 */
@Component
@Slf4j
public class GenderPreferenceScorer implements CompatibilityScorer {

    // -------------------------------------------------------------------------
    // CONFIGURATION
    // -------------------------------------------------------------------------

    /**
     * Weight of this scorer in the final calculation.
     * Default: 0.1 (10% of total score)
     *
     * NOTE: Despite low weight, returning 0.0 effectively filters out candidates
     * because it brings down the weighted average significantly.
     *
     * TO MAKE IT A STRONGER FILTER: Increase weight to 0.3 or higher
     * TO ADJUST: Change 'recommendation.scorers.gender.weight' in application.yml
     */
    @Value("${recommendation.scorers.gender.weight:0.1}")
    private double weight;

    /**
     * Calculate gender preference compatibility score.
     *
     * ALGORITHM:
     * 1. Check if candidate's gender is in user's preferences
     * 2. Check if user's gender is in candidate's preferences
     * 3. Return 1.0 if mutual match, 0.0 otherwise
     *
     * @param user      The user requesting recommendations
     * @param candidate A potential match candidate
     * @return Score: 1.0 (mutual match) or 0.0 (no match)
     */
    @Override
    public double calculateScore(User user, User candidate) {
        // =====================================================================
        // STEP 1: Get gender information
        // =====================================================================
        String userGender = user.getGender();
        String candidateGender = candidate.getGender();
        Set<String> userPreferences = user.getGenderPreferences();
        Set<String> candidatePreferences = candidate.getGenderPreferences();

        // =====================================================================
        // STEP 2: Handle missing data
        // =====================================================================
        // If gender preferences aren't set, return neutral score
        // This is common for new users who haven't completed their profile
        //
        // ALTERNATIVE: Return 0.0 to enforce profile completion
        // =====================================================================
        if (userGender == null || candidateGender == null) {
            log.warn("Missing gender data: user gender={}, candidate gender={}. " +
                     "Returning neutral score.",
                    userGender, candidateGender);
            return 0.5;
        }

        // If preferences aren't set, assume open to all
        if (userPreferences == null || userPreferences.isEmpty()) {
            log.trace("User {} has no gender preferences set. Assuming open to all.",
                    user.getId());
            userPreferences = Set.of("MALE", "FEMALE", "OTHER");
        }

        if (candidatePreferences == null || candidatePreferences.isEmpty()) {
            log.trace("Candidate {} has no gender preferences set. Assuming open to all.",
                    candidate.getId());
            candidatePreferences = Set.of("MALE", "FEMALE", "OTHER");
        }

        // =====================================================================
        // STEP 3: Check mutual gender preference match
        // =====================================================================
        // User must want candidate's gender AND candidate must want user's gender

        boolean userAcceptsCandidate = userPreferences.contains(candidateGender);
        boolean candidateAcceptsUser = candidatePreferences.contains(userGender);

        log.trace("User {} (gender={}, wants={}) + Candidate {} (gender={}, wants={}): " +
                  "userAccepts={}, candidateAccepts={}",
                user.getId(), userGender, userPreferences,
                candidate.getId(), candidateGender, candidatePreferences,
                userAcceptsCandidate, candidateAcceptsUser);

        // =====================================================================
        // STEP 4: Calculate score
        // =====================================================================
        // SCORING LOGIC (binary):
        // - Both accept each other: 1.0 (show this candidate)
        // - One or both reject: 0.0 (don't show)
        //
        // WHY BINARY:
        // Gender preference is usually non-negotiable. There's no "partial match"
        // for gender the way there is for age or distance.
        //
        // TO MODIFY FOR SOFT MATCHING:
        // if (userAcceptsCandidate && candidateAcceptsUser) return 1.0;
        // if (userAcceptsCandidate || candidateAcceptsUser) return 0.5;
        // return 0.0;
        //
        // TO MODIFY FOR USER-PRIORITY:
        // Only check if user wants candidate (don't check reverse)
        // return userAcceptsCandidate ? 1.0 : 0.0;
        // =====================================================================
        if (userAcceptsCandidate && candidateAcceptsUser) {
            log.trace("Mutual gender match between {} and {}", user.getId(), candidate.getId());
            return 1.0;
        } else {
            log.trace("Gender mismatch between {} and {}. " +
                      "User accepts={}, Candidate accepts={}",
                    user.getId(), candidate.getId(),
                    userAcceptsCandidate, candidateAcceptsUser);
            return 0.0;
        }
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public String getName() {
        return "gender-preference";
    }
}
