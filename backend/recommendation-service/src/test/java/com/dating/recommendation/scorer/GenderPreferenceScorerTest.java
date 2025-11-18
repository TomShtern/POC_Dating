package com.dating.recommendation.scorer;

import com.dating.recommendation.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================================
 * GENDER PREFERENCE SCORER TESTS
 * ============================================================================
 *
 * Tests for the GenderPreferenceScorer component.
 * Verifies binary gender preference matching.
 *
 * TEST CASES:
 * - Mutual match → 1.0
 * - No match → 0.0
 * - Edge cases (missing data, multiple preferences)
 *
 * ============================================================================
 */
class GenderPreferenceScorerTest {

    private GenderPreferenceScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new GenderPreferenceScorer();
        ReflectionTestUtils.setField(scorer, "weight", 0.1);
    }

    // =========================================================================
    // BASIC MATCHING TESTS
    // =========================================================================

    @Test
    @DisplayName("Mutual straight match should return 1.0")
    void testMutualStraightMatch() {
        // Man interested in women
        User user = createUser("MALE", Set.of("FEMALE"));
        // Woman interested in men
        User candidate = createUser("FEMALE", Set.of("MALE"));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(1.0, score, "Mutual straight match should return 1.0");
    }

    @Test
    @DisplayName("Mutual gay match should return 1.0")
    void testMutualGayMatch() {
        // Man interested in men
        User user = createUser("MALE", Set.of("MALE"));
        // Man interested in men
        User candidate = createUser("MALE", Set.of("MALE"));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(1.0, score, "Mutual gay match should return 1.0");
    }

    @Test
    @DisplayName("One-way preference should return 0.0")
    void testOneWayPreference() {
        // Man interested in women
        User user = createUser("MALE", Set.of("FEMALE"));
        // Woman interested in women (not men)
        User candidate = createUser("FEMALE", Set.of("FEMALE"));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.0, score, "One-way preference should return 0.0");
    }

    @Test
    @DisplayName("No matching preferences should return 0.0")
    void testNoMatchingPreferences() {
        // Man interested in women
        User user = createUser("MALE", Set.of("FEMALE"));
        // Man interested in women (both want women, neither wants each other)
        User candidate = createUser("MALE", Set.of("FEMALE"));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.0, score, "No matching preferences should return 0.0");
    }

    // =========================================================================
    // BISEXUAL PREFERENCE TESTS
    // =========================================================================

    @Test
    @DisplayName("Bisexual user should match with any gender")
    void testBisexualUserMatches() {
        // Man interested in both
        User user = createUser("MALE", Set.of("MALE", "FEMALE"));
        // Woman interested in men
        User candidate = createUser("FEMALE", Set.of("MALE"));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(1.0, score, "Bisexual user should match");
    }

    @Test
    @DisplayName("Both bisexual should match")
    void testBothBisexual() {
        // Person interested in both
        User user = createUser("FEMALE", Set.of("MALE", "FEMALE"));
        // Person interested in both
        User candidate = createUser("OTHER", Set.of("MALE", "FEMALE", "OTHER"));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(1.0, score, "Both bisexual should match");
    }

    // =========================================================================
    // NON-BINARY GENDER TESTS
    // =========================================================================

    @Test
    @DisplayName("Non-binary preference matching should work")
    void testNonBinaryMatching() {
        // Non-binary interested in non-binary
        User user = createUser("OTHER", Set.of("OTHER"));
        // Non-binary interested in other
        User candidate = createUser("OTHER", Set.of("OTHER"));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(1.0, score, "Non-binary mutual match should return 1.0");
    }

    @Test
    @DisplayName("All genders preference should match everyone")
    void testAllGendersPreference() {
        // User interested in everyone
        User user = createUser("MALE", Set.of("MALE", "FEMALE", "OTHER"));
        // Non-binary interested in males
        User candidate = createUser("OTHER", Set.of("MALE"));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(1.0, score, "All genders should match");
    }

    // =========================================================================
    // EDGE CASE TESTS
    // =========================================================================

    @Test
    @DisplayName("Null user gender should return 0.5")
    void testNullUserGender() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .gender(null)
                .genderPreferences(Set.of("FEMALE"))
                .build();

        User candidate = createUser("FEMALE", Set.of("MALE"));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.5, score, "Null gender should return neutral 0.5");
    }

    @Test
    @DisplayName("Null candidate gender should return 0.5")
    void testNullCandidateGender() {
        User user = createUser("MALE", Set.of("FEMALE"));

        User candidate = User.builder()
                .id(UUID.randomUUID())
                .gender(null)
                .genderPreferences(Set.of("MALE"))
                .build();

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.5, score, "Null gender should return neutral 0.5");
    }

    @Test
    @DisplayName("Empty preferences should assume open to all")
    void testEmptyPreferences() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .gender("MALE")
                .genderPreferences(Set.of())  // Empty
                .build();

        User candidate = createUser("FEMALE", Set.of("MALE", "FEMALE", "OTHER"));

        double score = scorer.calculateScore(user, candidate);

        // Empty preferences = assume open to all = should match
        assertEquals(1.0, score, "Empty preferences should be open to all");
    }

    @Test
    @DisplayName("Null preferences should assume open to all")
    void testNullPreferences() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .gender("MALE")
                .genderPreferences(null)  // Null
                .build();

        User candidate = createUser("FEMALE", Set.of("MALE", "FEMALE", "OTHER"));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(1.0, score, "Null preferences should be open to all");
    }

    // =========================================================================
    // WEIGHT AND NAME TESTS
    // =========================================================================

    @Test
    @DisplayName("getWeight should return configured weight")
    void testGetWeight() {
        assertEquals(0.1, scorer.getWeight());
    }

    @Test
    @DisplayName("getName should return 'gender-preference'")
    void testGetName() {
        assertEquals("gender-preference", scorer.getName());
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private User createUser(String gender, Set<String> preferences) {
        return User.builder()
                .id(UUID.randomUUID())
                .gender(gender)
                .genderPreferences(preferences)
                .build();
    }
}
