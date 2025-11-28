package com.dating.recommendation.scorer;

import com.dating.recommendation.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================================
 * AGE COMPATIBILITY SCORER TESTS
 * ============================================================================
 *
 * Tests for the AgeCompatibilityScorer component.
 * Verifies scoring logic for age preference matching.
 *
 * TEST CASES:
 * - Mutual acceptance (both in range) → 1.0
 * - One-way acceptance → 0.5
 * - No acceptance → 0.0
 * - Edge cases (missing data, edge of range)
 *
 * ============================================================================
 */
class AgeCompatibilityScorerTest {

    private AgeCompatibilityScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new AgeCompatibilityScorer();
        // Set weight via reflection (normally injected by Spring)
        ReflectionTestUtils.setField(scorer, "weight", 0.2);
    }

    // =========================================================================
    // BASIC SCORING TESTS
    // =========================================================================

    @Test
    @DisplayName("Mutual acceptance should return 1.0")
    void testMutualAcceptance() {
        // User: 25 years old, wants 20-30
        User user = createUser(25, 20, 30);
        // Candidate: 27 years old, wants 23-32
        User candidate = createUser(27, 23, 32);

        double score = scorer.calculateScore(user, candidate);

        assertEquals(1.0, score, "Mutual acceptance should return 1.0");
    }

    @Test
    @DisplayName("One-way acceptance (user accepts candidate) should return 0.5")
    void testOneWayAcceptanceUserAccepts() {
        // User: 25 years old, wants 20-30
        User user = createUser(25, 20, 30);
        // Candidate: 27 years old, wants 28-35 (doesn't accept 25)
        User candidate = createUser(27, 28, 35);

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.5, score, "One-way acceptance should return 0.5");
    }

    @Test
    @DisplayName("One-way acceptance (candidate accepts user) should return 0.5")
    void testOneWayAcceptanceCandidateAccepts() {
        // User: 25 years old, wants 30-40 (doesn't accept 27)
        User user = createUser(25, 30, 40);
        // Candidate: 27 years old, wants 20-30
        User candidate = createUser(27, 20, 30);

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.5, score, "One-way acceptance should return 0.5");
    }

    @Test
    @DisplayName("No acceptance should return 0.0")
    void testNoAcceptance() {
        // User: 25 years old, wants 30-40
        User user = createUser(25, 30, 40);
        // Candidate: 50 years old, wants 45-55
        User candidate = createUser(50, 45, 55);

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.0, score, "No acceptance should return 0.0");
    }

    // =========================================================================
    // EDGE CASE TESTS
    // =========================================================================

    @Test
    @DisplayName("Exactly at minimum of range should be accepted")
    void testExactlyAtMinimum() {
        // User: 25 years old, wants 20-30
        User user = createUser(25, 20, 30);
        // Candidate: 20 years old, wants 20-30
        User candidate = createUser(20, 20, 30);

        double score = scorer.calculateScore(user, candidate);

        assertEquals(1.0, score, "Exactly at minimum should be accepted");
    }

    @Test
    @DisplayName("Exactly at maximum of range should be accepted")
    void testExactlyAtMaximum() {
        // User: 25 years old, wants 20-30
        User user = createUser(25, 20, 30);
        // Candidate: 30 years old, wants 20-30
        User candidate = createUser(30, 20, 30);

        double score = scorer.calculateScore(user, candidate);

        assertEquals(1.0, score, "Exactly at maximum should be accepted");
    }

    @Test
    @DisplayName("Just outside range should not be accepted")
    void testJustOutsideRange() {
        // User: 25 years old, wants 20-30
        User user = createUser(25, 20, 30);
        // Candidate: 31 years old (just outside), wants 20-35
        User candidate = createUser(31, 20, 35);

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.5, score, "Just outside range = one-way acceptance");
    }

    @Test
    @DisplayName("Missing date of birth should return neutral score")
    void testMissingDateOfBirth() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .dateOfBirth(null)  // Missing
                .minAgePreference(20)
                .maxAgePreference(30)
                .build();

        User candidate = createUser(25, 20, 30);

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.5, score, "Missing data should return neutral 0.5");
    }

    @Test
    @DisplayName("Default preferences (18-100) should accept most users")
    void testDefaultPreferences() {
        // User with default preferences
        User user = User.builder()
                .id(UUID.randomUUID())
                .dateOfBirth(LocalDate.now().minusYears(25))
                .minAgePreference(18)
                .maxAgePreference(100)
                .build();

        // Candidate with default preferences
        User candidate = User.builder()
                .id(UUID.randomUUID())
                .dateOfBirth(LocalDate.now().minusYears(40))
                .minAgePreference(18)
                .maxAgePreference(100)
                .build();

        double score = scorer.calculateScore(user, candidate);

        assertEquals(1.0, score, "Default preferences should accept most users");
    }

    // =========================================================================
    // WEIGHT TEST
    // =========================================================================

    @Test
    @DisplayName("getWeight should return configured weight")
    void testGetWeight() {
        assertEquals(0.2, scorer.getWeight());
    }

    @Test
    @DisplayName("getName should return 'age-compatibility'")
    void testGetName() {
        assertEquals("age-compatibility", scorer.getName());
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Create a user with specified age and preferences.
     */
    private User createUser(int age, int minPref, int maxPref) {
        return User.builder()
                .id(UUID.randomUUID())
                .dateOfBirth(LocalDate.now().minusYears(age))
                .minAgePreference(minPref)
                .maxAgePreference(maxPref)
                .build();
    }
}
