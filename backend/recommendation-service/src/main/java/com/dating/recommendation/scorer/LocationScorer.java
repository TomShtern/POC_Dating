package com.dating.recommendation.scorer;

import com.dating.recommendation.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * LOCATION SCORER
 * ============================================================================
 *
 * PURPOSE:
 * Scores candidates based on geographic distance from the user.
 * Closer candidates get higher scores.
 *
 * HOW IT WORKS:
 * 1. Calculate distance between user and candidate using Haversine formula
 * 2. Compare to user's maximum distance preference
 * 3. Return linear score (closer = higher)
 *
 * HAVERSINE FORMULA:
 * Calculates great-circle distance between two points on a sphere.
 * Accurate for any distance on Earth (accounts for curvature).
 *
 * SCORING LOGIC:
 * - 0 km: 1.0 (same location, perfect)
 * - max distance / 2: 0.5 (halfway)
 * - max distance: 0.0 (too far)
 *
 * WHY THIS MATTERS:
 * - Most users want to meet in person
 * - Long distance relationships are less likely to succeed
 * - Closer = more likely to actually meet
 *
 * HOW TO MODIFY:
 * - Linear decay (current): Simple, predictable
 * - Exponential decay: score = Math.exp(-distance / maxDistance)
 * - Step function: if (distance < 10) return 1.0; else return 0.0;
 * - City-based: Use city matching instead of exact distance
 *
 * CONFIGURATION:
 * application.yml:
 *   recommendation:
 *     scorers:
 *       location:
 *         weight: 0.3         # 30% of total score
 *         max-distance-km: 50 # Maximum distance to consider
 *
 * EXAMPLE CALCULATIONS:
 * - 0 km away → 1.0
 * - 10 km away (max 50) → 0.8
 * - 25 km away (max 50) → 0.5
 * - 50 km away (max 50) → 0.0
 *
 * ============================================================================
 */
@Component
@Slf4j
public class LocationScorer implements CompatibilityScorer {

    // -------------------------------------------------------------------------
    // CONFIGURATION
    // -------------------------------------------------------------------------

    /**
     * Weight of this scorer in the final calculation.
     * Default: 0.3 (location is 30% of total score)
     *
     * TO ADJUST: Change 'recommendation.scorers.location.weight' in application.yml
     */
    @Value("${recommendation.scorers.location.weight:0.3}")
    private double weight;

    /**
     * Default maximum distance in kilometers.
     * Used as fallback if user hasn't set a preference.
     *
     * TO ADJUST: Change 'recommendation.scorers.location.max-distance-km'
     */
    @Value("${recommendation.scorers.location.max-distance-km:50}")
    private double defaultMaxDistanceKm;

    // -------------------------------------------------------------------------
    // CONSTANTS
    // -------------------------------------------------------------------------

    /**
     * Earth's radius in kilometers.
     * Used in Haversine formula for distance calculation.
     *
     * ACCURACY NOTE:
     * Earth is not a perfect sphere (it's an oblate spheroid).
     * Using mean radius 6371 km gives <0.5% error for most distances.
     * For extreme precision, use Vincenty's formulae instead.
     */
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculate location-based compatibility score.
     *
     * ALGORITHM:
     * 1. Calculate distance using Haversine formula
     * 2. Get user's max distance preference (or use default)
     * 3. Return linear decay score
     *
     * @param user      The user requesting recommendations
     * @param candidate A potential match candidate
     * @return Score: 1.0 (same location) to 0.0 (beyond max distance)
     */
    @Override
    public double calculateScore(User user, User candidate) {
        // =====================================================================
        // STEP 1: Check if both users have location data
        // =====================================================================
        if (!user.hasLocation() || !candidate.hasLocation()) {
            log.warn("Missing location data: user has location={}, candidate has location={}. " +
                     "Returning neutral score.",
                    user.hasLocation(), candidate.hasLocation());
            return 0.5; // Neutral score for missing data
        }

        // =====================================================================
        // STEP 2: Calculate distance using Haversine formula
        // =====================================================================
        double distance = calculateHaversineDistance(
                user.getLatitude(), user.getLongitude(),
                candidate.getLatitude(), candidate.getLongitude()
        );

        log.trace("Distance between user {} and candidate {}: {} km",
                user.getId(), candidate.getId(), String.format("%.2f", distance));

        // =====================================================================
        // STEP 3: Get user's maximum distance preference
        // =====================================================================
        // Use user's preference if set, otherwise use default from config
        double maxDistance = user.getMaxDistancePreference() != null
                ? user.getMaxDistancePreference()
                : defaultMaxDistanceKm;

        // =====================================================================
        // STEP 4: Calculate score based on distance
        // =====================================================================
        // SCORING LOGIC (linear decay):
        // - distance = 0: score = 1.0 (perfect)
        // - distance = max: score = 0.0 (too far)
        // - Linear interpolation in between
        //
        // FORMULA: score = 1.0 - (distance / maxDistance)
        //
        // TO MODIFY:
        // - Exponential decay: return Math.exp(-distance / maxDistance);
        //   (gentler decay, still shows far candidates with low score)
        //
        // - Quadratic decay: return Math.pow(1.0 - distance/maxDistance, 2);
        //   (more emphasis on very close candidates)
        //
        // - Step function: return distance < threshold ? 1.0 : 0.0;
        //   (binary: either close enough or not)
        //
        // - Tiered scoring:
        //   if (distance < 5) return 1.0;      // Walking distance
        //   if (distance < 20) return 0.8;     // Short drive
        //   if (distance < 50) return 0.5;     // Reasonable drive
        //   return 0.0;                        // Too far
        // =====================================================================
        if (distance >= maxDistance) {
            log.trace("Candidate {} is beyond max distance ({} >= {})",
                    candidate.getId(), String.format("%.1f", distance), String.format("%.1f", maxDistance));
            return 0.0;
        }

        double score = 1.0 - (distance / maxDistance);

        log.trace("Location score for candidate {}: {} (distance={}km, max={}km)",
                candidate.getId(), String.format("%.3f", score), String.format("%.1f", distance), String.format("%.1f", maxDistance));

        return score;
    }

    /**
     * Calculate distance between two points using Haversine formula.
     *
     * HAVERSINE FORMULA:
     * a = sin²(Δlat/2) + cos(lat1) * cos(lat2) * sin²(Δlon/2)
     * c = 2 * atan2(√a, √(1-a))
     * d = R * c
     *
     * WHERE:
     * - lat1, lon1: First point coordinates (in radians)
     * - lat2, lon2: Second point coordinates (in radians)
     * - R: Earth's radius (6371 km)
     * - d: Distance between points
     *
     * WHY HAVERSINE:
     * - Accounts for Earth's curvature
     * - Accurate for any distance (unlike flat-Earth approximation)
     * - Fast to compute (compared to Vincenty's formulae)
     *
     * ACCURACY:
     * - Error < 0.5% for most distances
     * - Good enough for dating app purposes
     *
     * @param lat1 Latitude of first point (degrees)
     * @param lon1 Longitude of first point (degrees)
     * @param lat2 Latitude of second point (degrees)
     * @param lon2 Longitude of second point (degrees)
     * @return Distance in kilometers
     */
    private double calculateHaversineDistance(double lat1, double lon1,
                                              double lat2, double lon2) {
        // =====================================================================
        // STEP 1: Convert degrees to radians
        // =====================================================================
        // Math.toRadians() converts degrees to radians (multiply by π/180)
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        // =====================================================================
        // STEP 2: Apply Haversine formula
        // =====================================================================
        // a = sin²(Δlat/2) + cos(lat1) * cos(lat2) * sin²(Δlon/2)
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        // c = 2 * atan2(√a, √(1-a))
        // atan2 is more numerically stable than asin for this purpose
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // =====================================================================
        // STEP 3: Calculate distance
        // =====================================================================
        // d = R * c
        return EARTH_RADIUS_KM * c;
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public String getName() {
        return "location-distance";
    }
}
