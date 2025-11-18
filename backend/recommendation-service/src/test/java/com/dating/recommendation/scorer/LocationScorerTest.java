package com.dating.recommendation.scorer;

import com.dating.recommendation.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================================
 * LOCATION SCORER TESTS
 * ============================================================================
 *
 * Tests for the LocationScorer component.
 * Verifies distance calculation and scoring logic.
 *
 * TEST CASES:
 * - Same location → 1.0
 * - Halfway to max → 0.5
 * - At max distance → 0.0
 * - Beyond max distance → 0.0
 * - Known distances (verified with external calculator)
 *
 * REFERENCE LOCATIONS:
 * - New York: 40.7128°N, 74.0060°W
 * - Los Angeles: 34.0522°N, 118.2437°W
 * - London: 51.5074°N, 0.1278°W
 *
 * ============================================================================
 */
class LocationScorerTest {

    private LocationScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new LocationScorer();
        ReflectionTestUtils.setField(scorer, "weight", 0.3);
        ReflectionTestUtils.setField(scorer, "defaultMaxDistanceKm", 50.0);
    }

    // =========================================================================
    // BASIC SCORING TESTS
    // =========================================================================

    @Test
    @DisplayName("Same location should return 1.0")
    void testSameLocation() {
        User user = createUserWithLocation(40.7128, -74.0060, 50);
        User candidate = createUserWithLocation(40.7128, -74.0060, 50);

        double score = scorer.calculateScore(user, candidate);

        assertEquals(1.0, score, 0.001, "Same location should return 1.0");
    }

    @Test
    @DisplayName("Location at max distance should return ~0.0")
    void testAtMaxDistance() {
        // User in Manhattan
        User user = createUserWithLocation(40.7580, -73.9855, 50);
        // Candidate ~50km away (Newark, NJ is about 15km)
        // Using calculated point ~50km south
        User candidate = createUserWithLocation(40.3080, -73.9855, 50);

        double score = scorer.calculateScore(user, candidate);

        // Should be close to 0 (exact depends on Haversine calculation)
        assertTrue(score <= 0.1, "At max distance should return close to 0");
    }

    @Test
    @DisplayName("Location halfway should return ~0.5")
    void testHalfwayDistance() {
        // User at origin point
        User user = createUserWithLocation(40.7128, -74.0060, 50);
        // Candidate ~25km away
        User candidate = createUserWithLocation(40.9378, -74.0060, 50);

        double score = scorer.calculateScore(user, candidate);

        // Should be around 0.5 (25km out of 50km)
        assertTrue(score > 0.3 && score < 0.7,
                "Halfway distance should return around 0.5, got: " + score);
    }

    // =========================================================================
    // KNOWN DISTANCE TESTS
    // =========================================================================

    @Test
    @DisplayName("New York to nearby location should score high")
    void testNearbyLocations() {
        // Times Square, NYC
        User user = createUserWithLocation(40.7580, -73.9855, 50);
        // Central Park (about 2km away)
        User candidate = createUserWithLocation(40.7829, -73.9654, 50);

        double score = scorer.calculateScore(user, candidate);

        // 2km out of 50km = 0.96
        assertTrue(score > 0.9, "Very close locations should score > 0.9, got: " + score);
    }

    @Test
    @DisplayName("Far locations should return 0.0")
    void testFarLocations() {
        // New York
        User user = createUserWithLocation(40.7128, -74.0060, 50);
        // Los Angeles (~3,940 km away)
        User candidate = createUserWithLocation(34.0522, -118.2437, 50);

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.0, score, "Far locations should return 0.0");
    }

    // =========================================================================
    // USER PREFERENCE TESTS
    // =========================================================================

    @Test
    @DisplayName("User's max distance preference should be used")
    void testUserMaxDistancePreference() {
        // User with 100km max distance
        User user = createUserWithLocation(40.7128, -74.0060, 100);
        // Candidate 50km away
        User candidate = createUserWithLocation(40.2628, -74.0060, 50);

        double score = scorer.calculateScore(user, candidate);

        // 50km out of 100km = 0.5
        assertTrue(score > 0.4 && score < 0.6,
                "50km with 100km max should be around 0.5, got: " + score);
    }

    @Test
    @DisplayName("Null max distance preference should use default")
    void testNullMaxDistanceUsesDefault() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .latitude(40.7128)
                .longitude(-74.0060)
                .maxDistancePreference(null)  // Will use default 50km
                .build();

        User candidate = createUserWithLocation(40.7128, -74.0060, 50);

        double score = scorer.calculateScore(user, candidate);

        assertEquals(1.0, score, 0.001, "Same location should still be 1.0");
    }

    // =========================================================================
    // EDGE CASE TESTS
    // =========================================================================

    @Test
    @DisplayName("Missing user location should return neutral 0.5")
    void testMissingUserLocation() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .latitude(null)
                .longitude(null)
                .maxDistancePreference(50)
                .build();

        User candidate = createUserWithLocation(40.7128, -74.0060, 50);

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.5, score, "Missing location should return neutral 0.5");
    }

    @Test
    @DisplayName("Missing candidate location should return neutral 0.5")
    void testMissingCandidateLocation() {
        User user = createUserWithLocation(40.7128, -74.0060, 50);

        User candidate = User.builder()
                .id(UUID.randomUUID())
                .latitude(null)
                .longitude(null)
                .maxDistancePreference(50)
                .build();

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.5, score, "Missing location should return neutral 0.5");
    }

    @Test
    @DisplayName("Antipodal points should return 0.0")
    void testAntipodalPoints() {
        // North Pole
        User user = createUserWithLocation(90.0, 0.0, 50);
        // South Pole
        User candidate = createUserWithLocation(-90.0, 0.0, 50);

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.0, score, "Opposite sides of Earth should return 0.0");
    }

    // =========================================================================
    // WEIGHT AND NAME TESTS
    // =========================================================================

    @Test
    @DisplayName("getWeight should return configured weight")
    void testGetWeight() {
        assertEquals(0.3, scorer.getWeight());
    }

    @Test
    @DisplayName("getName should return 'location-distance'")
    void testGetName() {
        assertEquals("location-distance", scorer.getName());
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private User createUserWithLocation(double lat, double lon, int maxDistance) {
        return User.builder()
                .id(UUID.randomUUID())
                .latitude(lat)
                .longitude(lon)
                .maxDistancePreference(maxDistance)
                .build();
    }
}
