package com.dating.recommendation.scorer;

import com.dating.recommendation.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * AGE COMPATIBILITY SCORER
 * ============================================================================
 *
 * PURPOSE:
 * Calculates how well two users match based on their age preferences.
 * This is a critical scorer because age preferences are usually hard filters.
 *
 * HOW IT WORKS:
 * 1. Check if candidate's age falls within user's preferred age range
 * 2. Check if user's age falls within candidate's preferred age range
 * 3. Score based on mutual acceptance
 *
 * SCORING LOGIC:
 * - Both accept each other: 1.0 (perfect match)
 * - Only one accepts: 0.5 (partial match)
 * - Neither accepts: 0.0 (no match)
 *
 * WHY THIS MATTERS:
 * - Age preferences are typically non-negotiable for users
 * - A 25-year-old looking for 25-30 won't want to see 50-year-olds
 * - Respecting preferences builds trust in the recommendation system
 *
 * HOW TO MODIFY:
 * - To make age less important: reduce weight in application.yml
 * - To add age gap penalty: penalize large gaps even within range
 * - To use continuous scoring: use Gaussian distribution around preferred age
 * - To ignore age: set weight to 0 in application.yml
 *
 * CONFIGURATION:
 * application.yml:
 *   recommendation:
 *     scorers:
 *       age:
 *         weight: 0.2  # 20% of total score
 *
 * EXAMPLE CALCULATIONS:
 * - User (age 25, wants 23-30) + Candidate (age 27, wants 24-28) → 1.0 (mutual)
 * - User (age 25, wants 23-30) + Candidate (age 27, wants 26-35) → 0.5 (one-way)
 * - User (age 25, wants 23-30) + Candidate (age 35, wants 30-40) → 0.0 (neither)
 *
 * ============================================================================
 */
@Component
@Slf4j
public class AgeCompatibilityScorer implements CompatibilityScorer {

    // -------------------------------------------------------------------------
    // CONFIGURATION
    // -------------------------------------------------------------------------
    // Weight determines how much this factor affects the final score.
    // Range: 0.0 to 1.0 (can be higher for emphasis)
    // Default: 0.2 (age is 20% of the total compatibility score)
    //
    // TO ADJUST: Change 'recommendation.scorers.age.weight' in application.yml
    // TO DISABLE: Set weight to 0
    // -------------------------------------------------------------------------
    @Value("${recommendation.scorers.age.weight:0.2}")
    private double weight;

    /**
     * Calculate age compatibility score between two users.
     *
     * ALGORITHM:
     * 1. Get candidate's age and check against user's preferred range
     * 2. Get user's age and check against candidate's preferred range
     * 3. Return score based on mutual acceptance
     *
     * @param user      The user requesting recommendations
     * @param candidate A potential match candidate
     * @return Score: 1.0 (mutual), 0.5 (one-way), 0.0 (neither)
     */
    @Override
    public double calculateScore(User user, User candidate) {
        // =====================================================================
        // STEP 1: Get ages
        // =====================================================================
        // Note: getAge() calculates from dateOfBirth using Period.between()
        int userAge = user.getAge();
        int candidateAge = candidate.getAge();

        // Handle edge case: missing date of birth
        if (userAge <= 0 || candidateAge <= 0) {
            log.warn("Missing age data: user age={}, candidate age={}. Returning neutral score.",
                    userAge, candidateAge);
            return 0.5; // Neutral score for missing data
        }

        // =====================================================================
        // STEP 2: Check if candidate's age is within user's preferred range
        // =====================================================================
        // User's preferences define who they want to see
        int userMinPref = user.getMinAgePreference() != null ? user.getMinAgePreference() : 18;
        int userMaxPref = user.getMaxAgePreference() != null ? user.getMaxAgePreference() : 100;

        boolean userAcceptsCandidate =
                candidateAge >= userMinPref &&
                candidateAge <= userMaxPref;

        log.trace("User {} (age {}) prefers {}-{}, candidate age {}: {}",
                user.getId(), userAge, userMinPref, userMaxPref,
                candidateAge, userAcceptsCandidate ? "ACCEPTED" : "REJECTED");

        // =====================================================================
        // STEP 3: Check if user's age is within candidate's preferred range
        // =====================================================================
        // Candidate's preferences define who they want to match with
        int candMinPref = candidate.getMinAgePreference() != null ? candidate.getMinAgePreference() : 18;
        int candMaxPref = candidate.getMaxAgePreference() != null ? candidate.getMaxAgePreference() : 100;

        boolean candidateAcceptsUser =
                userAge >= candMinPref &&
                userAge <= candMaxPref;

        log.trace("Candidate {} (age {}) prefers {}-{}, user age {}: {}",
                candidate.getId(), candidateAge, candMinPref, candMaxPref,
                userAge, candidateAcceptsUser ? "ACCEPTED" : "REJECTED");

        // =====================================================================
        // STEP 4: Calculate score based on mutual acceptance
        // =====================================================================
        // SCORING LOGIC (modify this to change behavior):
        // - Both accept each other: 1.0 (perfect match)
        // - Only one accepts: 0.5 (partial match - still show but lower ranked)
        // - Neither accepts: 0.0 (no match - might filter out entirely)
        //
        // WHY 0.5 FOR ONE-WAY:
        // We still want to show these candidates but ranked lower.
        // The user might change their mind, or the candidate might.
        //
        // TO MODIFY:
        // - For strict matching: return 0.0 for one-way matches
        // - For lenient matching: return 0.75 for one-way matches
        // - For asymmetric: prioritize user's preferences (return 0.7 if user accepts)
        // =====================================================================
        if (userAcceptsCandidate && candidateAcceptsUser) {
            log.trace("Mutual age acceptance between {} and {}", user.getId(), candidate.getId());
            return 1.0; // Mutual acceptance - highest score
        } else if (userAcceptsCandidate || candidateAcceptsUser) {
            log.trace("One-way age acceptance between {} and {}", user.getId(), candidate.getId());
            return 0.5; // One-way acceptance - show but rank lower
        } else {
            log.trace("No age acceptance between {} and {}", user.getId(), candidate.getId());
            return 0.0; // No acceptance - consider filtering out
        }
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public String getName() {
        return "age-compatibility";
    }
}
