package com.dating.recommendation.scorer;

import com.dating.recommendation.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * ============================================================================
 * ACTIVITY SCORER
 * ============================================================================
 *
 * PURPOSE:
 * Scores candidates based on how recently they were active on the platform.
 * Active users are more likely to respond, so we prefer them.
 *
 * HOW IT WORKS:
 * 1. Get candidate's last active timestamp
 * 2. Calculate days since last activity
 * 3. Score using linear decay from threshold
 *
 * SCORING LOGIC:
 * - Active today: 1.0 (highest priority)
 * - Active at threshold: 0.0 (consider inactive)
 * - Linear decay between
 *
 * WHY THIS MATTERS:
 * - Recommending inactive users leads to frustration
 * - Users who don't log in won't respond to matches
 * - Activity suggests engagement with the platform
 *
 * ACTIVITY DEFINITION:
 * "Active" typically means any platform interaction:
 * - Login
 * - Swipe (like/pass)
 * - Send message
 * - Update profile
 * - View matches
 *
 * HOW TO MODIFY:
 * - Linear decay (current): Simple, predictable
 * - Exponential decay: Faster drop-off for inactive users
 * - Step function: Binary active/inactive
 * - Multiple factors: Consider activity TYPE (messages > swipes > views)
 *
 * CONFIGURATION:
 * application.yml:
 *   recommendation:
 *     scorers:
 *       activity:
 *         weight: 0.15                    # 15% of total score
 *         inactive-days-threshold: 30     # Days until considered inactive
 *
 * EXAMPLE CALCULATIONS:
 * - Active today (0 days): 1.0
 * - Active 7 days ago (threshold 30): 1 - 7/30 = 0.77
 * - Active 15 days ago (threshold 30): 1 - 15/30 = 0.5
 * - Active 30+ days ago: 0.0
 *
 * ============================================================================
 */
@Component
@Slf4j
public class ActivityScorer implements CompatibilityScorer {

    // -------------------------------------------------------------------------
    // CONFIGURATION
    // -------------------------------------------------------------------------

    /**
     * Weight of this scorer in the final calculation.
     * Default: 0.15 (activity is 15% of total score)
     *
     * TO ADJUST: Change 'recommendation.scorers.activity.weight' in application.yml
     */
    @Value("${recommendation.scorers.activity.weight:0.15}")
    private double weight;

    /**
     * Number of days after which a user is considered inactive.
     * Default: 30 days
     *
     * CONSIDERATIONS:
     * - Too short (7 days): Penalizes users on vacation
     * - Too long (90 days): Shows many ghost profiles
     * - 30 days: Reasonable balance
     *
     * TO ADJUST: Change 'recommendation.scorers.activity.inactive-days-threshold'
     */
    @Value("${recommendation.scorers.activity.inactive-days-threshold:30}")
    private int inactiveDaysThreshold;

    /**
     * Calculate activity-based compatibility score.
     *
     * ALGORITHM:
     * 1. Get candidate's last active timestamp
     * 2. Calculate days since last activity
     * 3. Apply linear decay from threshold
     *
     * @param user      The user requesting recommendations
     * @param candidate A potential match candidate
     * @return Score: 1.0 (active today) to 0.0 (inactive for threshold days)
     */
    @Override
    public double calculateScore(User user, User candidate) {
        // =====================================================================
        // STEP 1: Get candidate's last active timestamp
        // =====================================================================
        LocalDateTime lastActiveAt = candidate.getLastActiveAt();

        // Handle missing data
        if (lastActiveAt == null) {
            log.trace("Candidate {} has no lastActiveAt. Returning neutral score.",
                    candidate.getId());
            return 0.5; // Neutral - don't penalize missing data
        }

        // =====================================================================
        // STEP 2: Calculate days since last activity
        // =====================================================================
        LocalDateTime now = LocalDateTime.now();
        long daysSinceActive = ChronoUnit.DAYS.between(lastActiveAt, now);

        log.trace("Candidate {} was last active {} days ago (lastActiveAt: {})",
                candidate.getId(), daysSinceActive, lastActiveAt);

        // =====================================================================
        // STEP 3: Handle edge case - future timestamp
        // =====================================================================
        // This shouldn't happen, but handle defensively
        if (daysSinceActive < 0) {
            log.warn("Candidate {} has future lastActiveAt: {}. Treating as active.",
                    candidate.getId(), lastActiveAt);
            return 1.0;
        }

        // =====================================================================
        // STEP 4: Calculate score using linear decay
        // =====================================================================
        // FORMULA: score = 1.0 - (daysSinceActive / threshold)
        //
        // SCORING LOGIC:
        // - Active today (0 days): score = 1.0
        // - Halfway to threshold: score = 0.5
        // - At threshold: score = 0.0
        // - Beyond threshold: score = 0.0 (clamped)
        //
        // ALTERNATIVE FORMULAS:
        //
        // Exponential decay (gentler, long tail):
        // double score = Math.exp(-daysSinceActive / (double) inactiveDaysThreshold);
        //
        // Quadratic decay (harsher on medium-inactive):
        // double ratio = (double) daysSinceActive / inactiveDaysThreshold;
        // double score = ratio >= 1.0 ? 0.0 : Math.pow(1.0 - ratio, 2);
        //
        // Step function (binary):
        // double score = daysSinceActive <= 7 ? 1.0 : 0.0;
        //
        // Tiered scoring:
        // if (daysSinceActive <= 1) return 1.0;   // Active today/yesterday
        // if (daysSinceActive <= 7) return 0.8;   // Active this week
        // if (daysSinceActive <= 14) return 0.5;  // Active this fortnight
        // if (daysSinceActive <= 30) return 0.3;  // Active this month
        // return 0.0;                              // Inactive
        // =====================================================================
        if (daysSinceActive >= inactiveDaysThreshold) {
            log.trace("Candidate {} is inactive ({} days >= {} threshold). Score: 0",
                    candidate.getId(), daysSinceActive, inactiveDaysThreshold);
            return 0.0;
        }

        double score = 1.0 - ((double) daysSinceActive / inactiveDaysThreshold);

        log.trace("Activity score for candidate {}: {:.3f} ({} days since active, threshold {})",
                candidate.getId(), score, daysSinceActive, inactiveDaysThreshold);

        return score;
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public String getName() {
        return "activity-level";
    }
}
