package com.dating.recommendation.scorer;

import com.dating.recommendation.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================================
 * ACTIVITY SCORER TESTS
 * ============================================================================
 *
 * Tests for the ActivityScorer component.
 * Verifies scoring based on user activity recency.
 *
 * TEST CASES:
 * - Active today → 1.0
 * - Active at threshold → 0.0
 * - Inactive beyond threshold → 0.0
 * - Linear decay between
 *
 * ============================================================================
 */
class ActivityScorerTest {

    private ActivityScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new ActivityScorer();
        ReflectionTestUtils.setField(scorer, "weight", 0.15);
        ReflectionTestUtils.setField(scorer, "inactiveDaysThreshold", 30);
    }

    // =========================================================================
    // BASIC SCORING TESTS
    // =========================================================================

    @Test
    @DisplayName("Active just now should return 1.0")
    void testActiveNow() {
        User user = createUser();
        User candidate = createCandidateActiveAt(LocalDateTime.now());

        double score = scorer.calculateScore(user, candidate);

        assertEquals(1.0, score, 0.001, "Active now should return 1.0");
    }

    @Test
    @DisplayName("Active 15 days ago should return ~0.5")
    void testActiveHalfwayThreshold() {
        User user = createUser();
        User candidate = createCandidateActiveAt(LocalDateTime.now().minusDays(15));

        double score = scorer.calculateScore(user, candidate);

        // 15/30 = 0.5 away from 1.0 = 0.5
        assertEquals(0.5, score, 0.01, "15 days ago should return ~0.5");
    }

    @Test
    @DisplayName("Active exactly at threshold should return 0.0")
    void testActiveAtThreshold() {
        User user = createUser();
        User candidate = createCandidateActiveAt(LocalDateTime.now().minusDays(30));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.0, score, 0.001, "At threshold should return 0.0");
    }

    @Test
    @DisplayName("Active beyond threshold should return 0.0")
    void testActiveBeyondThreshold() {
        User user = createUser();
        User candidate = createCandidateActiveAt(LocalDateTime.now().minusDays(60));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.0, score, "Beyond threshold should return 0.0");
    }

    // =========================================================================
    // LINEAR DECAY TESTS
    // =========================================================================

    @Test
    @DisplayName("Active 7 days ago should return ~0.77")
    void testActive7DaysAgo() {
        User user = createUser();
        User candidate = createCandidateActiveAt(LocalDateTime.now().minusDays(7));

        double score = scorer.calculateScore(user, candidate);

        // 1 - 7/30 = 1 - 0.233 = 0.767
        assertEquals(0.767, score, 0.02, "7 days ago should return ~0.77");
    }

    @Test
    @DisplayName("Active 1 day ago should return ~0.97")
    void testActive1DayAgo() {
        User user = createUser();
        User candidate = createCandidateActiveAt(LocalDateTime.now().minusDays(1));

        double score = scorer.calculateScore(user, candidate);

        // 1 - 1/30 = 0.967
        assertEquals(0.967, score, 0.02, "1 day ago should return ~0.97");
    }

    @Test
    @DisplayName("Active 29 days ago should return ~0.03")
    void testActive29DaysAgo() {
        User user = createUser();
        User candidate = createCandidateActiveAt(LocalDateTime.now().minusDays(29));

        double score = scorer.calculateScore(user, candidate);

        // 1 - 29/30 = 0.033
        assertEquals(0.033, score, 0.02, "29 days ago should return ~0.03");
    }

    // =========================================================================
    // EDGE CASE TESTS
    // =========================================================================

    @Test
    @DisplayName("Null lastActiveAt should return neutral 0.5")
    void testNullLastActiveAt() {
        User user = createUser();
        User candidate = User.builder()
                .id(UUID.randomUUID())
                .lastActiveAt(null)
                .build();

        double score = scorer.calculateScore(user, candidate);

        assertEquals(0.5, score, "Null lastActiveAt should return 0.5");
    }

    @Test
    @DisplayName("Future lastActiveAt should return 1.0")
    void testFutureLastActiveAt() {
        // This shouldn't happen in production, but test defensive behavior
        User user = createUser();
        User candidate = createCandidateActiveAt(LocalDateTime.now().plusDays(1));

        double score = scorer.calculateScore(user, candidate);

        assertEquals(1.0, score, "Future timestamp should return 1.0");
    }

    @Test
    @DisplayName("Active just before midnight should calculate correctly")
    void testActiveJustBeforeMidnight() {
        User user = createUser();
        User candidate = createCandidateActiveAt(
                LocalDateTime.now().minusDays(10).withHour(23).withMinute(59));

        double score = scorer.calculateScore(user, candidate);

        // Should be close to 0.67 (10/30 = 0.33 away)
        assertTrue(score > 0.6 && score < 0.7,
                "10 days ago should be around 0.67, got: " + score);
    }

    // =========================================================================
    // DIFFERENT THRESHOLD TESTS
    // =========================================================================

    @Test
    @DisplayName("With 7-day threshold, 3.5 days should return 0.5")
    void testDifferentThreshold() {
        // Change threshold to 7 days
        ReflectionTestUtils.setField(scorer, "inactiveDaysThreshold", 7);

        User user = createUser();
        User candidate = createCandidateActiveAt(LocalDateTime.now().minusDays(3));

        double score = scorer.calculateScore(user, candidate);

        // 1 - 3/7 = 0.571
        assertTrue(score > 0.5 && score < 0.7,
                "3/7 days should return ~0.57, got: " + score);
    }

    // =========================================================================
    // WEIGHT AND NAME TESTS
    // =========================================================================

    @Test
    @DisplayName("getWeight should return configured weight")
    void testGetWeight() {
        assertEquals(0.15, scorer.getWeight());
    }

    @Test
    @DisplayName("getName should return 'activity-level'")
    void testGetName() {
        assertEquals("activity-level", scorer.getName());
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private User createUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .build();
    }

    private User createCandidateActiveAt(LocalDateTime lastActive) {
        return User.builder()
                .id(UUID.randomUUID())
                .lastActiveAt(lastActive)
                .build();
    }
}
