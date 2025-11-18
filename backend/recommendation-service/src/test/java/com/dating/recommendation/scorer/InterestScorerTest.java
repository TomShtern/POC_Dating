package com.dating.recommendation.scorer;

import com.dating.recommendation.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================================
 * INTEREST SCORER TESTS
 * ============================================================================
 *
 * Tests for the InterestScorer component.
 * Verifies Jaccard similarity calculation.
 *
 * JACCARD FORMULA: |intersection| / |union|
 *
 * TEST CASES:
 * - Identical interests → 1.0
 * - No overlap → 0.0
 * - Partial overlap → proportion
 * - Empty interests → 0.5 (neutral)
 *
 * ============================================================================
 */
class InterestScorerTest {

    private InterestScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new InterestScorer();
        ReflectionTestUtils.setField(scorer, "weight", 0.25);
        ReflectionTestUtils.setField(scorer, "minimumShared", 0);
    }

    // =========================================================================
    // BASIC SCORING TESTS
    // =========================================================================

    @Test
    @DisplayName("Identical interests should return 1.0")
    void testIdenticalInterests() {
        Set<String> interests = Set.of("hiking", "cooking", "reading");

        User user = createUserWithInterests(interests);
        User candidate = createUserWithInterests(interests);

        double score = scorer.calculateScore(user, candidate);

        assertEquals(1.0, score, 0.001, "Identical interests should return 1.0");
    }

    @Test
    @DisplayName("No shared interests should return 0.0")
    void testNoSharedInterests() {
        User user = createUserWithInterests(Set.of("hiking", "cooking"));
        User candidate = createUserWithInterests(Set.of("gaming", "movies"));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.0, score, 0.001, "No overlap should return 0.0");
    }

    @Test
    @DisplayName("50% overlap should return 0.5")
    void testHalfOverlap() {
        // User: hiking, cooking
        // Candidate: hiking, movies
        // Intersection: {hiking} = 1
        // Union: {hiking, cooking, movies} = 3
        // Jaccard = 1/3 ≈ 0.333
        User user = createUserWithInterests(Set.of("hiking", "cooking"));
        User candidate = createUserWithInterests(Set.of("hiking", "movies"));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.333, score, 0.01, "1/3 overlap should return ~0.333");
    }

    @Test
    @DisplayName("Calculate Jaccard correctly for different set sizes")
    void testDifferentSetSizes() {
        // User: hiking, cooking, reading (3)
        // Candidate: hiking (1)
        // Intersection: {hiking} = 1
        // Union: {hiking, cooking, reading} = 3
        // Jaccard = 1/3
        User user = createUserWithInterests(Set.of("hiking", "cooking", "reading"));
        User candidate = createUserWithInterests(Set.of("hiking"));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.333, score, 0.01, "1/3 Jaccard should return ~0.333");
    }

    // =========================================================================
    // EDGE CASE TESTS
    // =========================================================================

    @Test
    @DisplayName("Empty user interests should return neutral 0.5")
    void testEmptyUserInterests() {
        User user = createUserWithInterests(new HashSet<>());
        User candidate = createUserWithInterests(Set.of("hiking", "cooking"));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.5, score, "Empty user interests should return 0.5");
    }

    @Test
    @DisplayName("Empty candidate interests should return neutral 0.5")
    void testEmptyCandidateInterests() {
        User user = createUserWithInterests(Set.of("hiking", "cooking"));
        User candidate = createUserWithInterests(new HashSet<>());

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.5, score, "Empty candidate interests should return 0.5");
    }

    @Test
    @DisplayName("Null interests should return neutral 0.5")
    void testNullInterests() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .interests(null)
                .build();

        User candidate = createUserWithInterests(Set.of("hiking"));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.5, score, "Null interests should return 0.5");
    }

    @Test
    @DisplayName("Single shared interest with many total should score low")
    void testSingleSharedManyTotal() {
        // User: 5 interests
        // Candidate: 5 different interests + 1 shared
        // Intersection: 1
        // Union: 10
        // Jaccard = 0.1
        User user = createUserWithInterests(
                Set.of("hiking", "cooking", "reading", "music", "art"));
        User candidate = createUserWithInterests(
                Set.of("hiking", "gaming", "movies", "sports", "travel"));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.111, score, 0.01, "1/9 overlap should return ~0.111");
    }

    // =========================================================================
    // MINIMUM SHARED THRESHOLD TESTS
    // =========================================================================

    @Test
    @DisplayName("Below minimum shared should return 0.0")
    void testBelowMinimumShared() {
        // Set minimum to 2
        ReflectionTestUtils.setField(scorer, "minimumShared", 2);

        User user = createUserWithInterests(Set.of("hiking", "cooking", "reading"));
        User candidate = createUserWithInterests(Set.of("hiking", "gaming")); // Only 1 shared

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.0, score, "Below minimum shared should return 0.0");
    }

    @Test
    @DisplayName("At minimum shared should return normal score")
    void testAtMinimumShared() {
        // Set minimum to 2
        ReflectionTestUtils.setField(scorer, "minimumShared", 2);

        User user = createUserWithInterests(Set.of("hiking", "cooking", "reading"));
        User candidate = createUserWithInterests(Set.of("hiking", "cooking", "gaming")); // 2 shared

        double score = scorer.calculateScore(user, candidate);

        // Jaccard = 2/4 = 0.5
        assertEquals(0.5, score, 0.001, "At minimum should return normal score");
    }

    // =========================================================================
    // SPECIFIC EXAMPLES
    // =========================================================================

    @Test
    @DisplayName("Example from documentation")
    void testDocumentationExample() {
        // From the documentation example:
        // User interests: [hiking, cooking, reading]
        // Candidate interests: [hiking, cooking, movies]
        // Intersection: [hiking, cooking] (2)
        // Union: [hiking, cooking, reading, movies] (4)
        // Jaccard = 2/4 = 0.5
        User user = createUserWithInterests(Set.of("hiking", "cooking", "reading"));
        User candidate = createUserWithInterests(Set.of("hiking", "cooking", "movies"));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.5, score, 0.001, "Documentation example should return 0.5");
    }

    // =========================================================================
    // WEIGHT AND NAME TESTS
    // =========================================================================

    @Test
    @DisplayName("getWeight should return configured weight")
    void testGetWeight() {
        assertEquals(0.25, scorer.getWeight());
    }

    @Test
    @DisplayName("getName should return 'interest-overlap'")
    void testGetName() {
        assertEquals("interest-overlap", scorer.getName());
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private User createUserWithInterests(Set<String> interests) {
        return User.builder()
                .id(UUID.randomUUID())
                .interests(new HashSet<>(interests))
                .build();
    }
}
