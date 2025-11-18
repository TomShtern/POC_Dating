package com.dating.recommendation.service;

import com.dating.recommendation.dto.ScoredCandidate;
import com.dating.recommendation.model.User;
import com.dating.recommendation.scorer.CompatibilityScorer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================================
 * SCORE AGGREGATOR TESTS
 * ============================================================================
 *
 * Tests for the ScoreAggregator component.
 * Verifies weighted score aggregation logic.
 *
 * TEST CASES:
 * - Single scorer
 * - Multiple scorers with equal weights
 * - Multiple scorers with different weights
 * - Disabled scorers (weight = 0)
 * - Score normalization
 *
 * ============================================================================
 */
class ScoreAggregatorTest {

    private User user;
    private User candidate;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
        candidate = User.builder().id(UUID.randomUUID()).build();
    }

    // =========================================================================
    // BASIC AGGREGATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Single scorer should return its score")
    void testSingleScorer() {
        CompatibilityScorer scorer = new TestScorer("test", 0.5, 1.0);
        ScoreAggregator aggregator = new ScoreAggregator(List.of(scorer));

        ScoredCandidate result = aggregator.aggregate(user, candidate);

        assertEquals(1.0, result.finalScore(), 0.001,
                "Single scorer should return its score");
        assertEquals(1, result.scoreBreakdown().size());
        assertEquals(1.0, result.scoreBreakdown().get("test"));
    }

    @Test
    @DisplayName("Multiple scorers with equal weights should average")
    void testEqualWeights() {
        // Two scorers with weight 0.5, scores 1.0 and 0.5
        // Average = (1.0 + 0.5) / 2 = 0.75
        List<CompatibilityScorer> scorers = List.of(
                new TestScorer("scorer1", 0.5, 1.0),
                new TestScorer("scorer2", 0.5, 0.5)
        );
        ScoreAggregator aggregator = new ScoreAggregator(scorers);

        ScoredCandidate result = aggregator.aggregate(user, candidate);

        assertEquals(0.75, result.finalScore(), 0.001,
                "Equal weights should average scores");
    }

    @Test
    @DisplayName("Different weights should compute weighted average")
    void testDifferentWeights() {
        // Scorer1: weight 0.3, score 1.0 → contribution 0.3
        // Scorer2: weight 0.7, score 0.5 → contribution 0.35
        // Total = 0.65, normalized by 1.0 = 0.65
        List<CompatibilityScorer> scorers = List.of(
                new TestScorer("scorer1", 0.3, 1.0),
                new TestScorer("scorer2", 0.7, 0.5)
        );
        ScoreAggregator aggregator = new ScoreAggregator(scorers);

        ScoredCandidate result = aggregator.aggregate(user, candidate);

        assertEquals(0.65, result.finalScore(), 0.001,
                "Should compute weighted average");
    }

    // =========================================================================
    // NORMALIZATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Weights not summing to 1 should be normalized")
    void testNormalization() {
        // Weights sum to 0.5, should still work
        // Scorer1: weight 0.2, score 1.0 → contribution 0.2
        // Scorer2: weight 0.3, score 0.5 → contribution 0.15
        // Total = 0.35, normalized by 0.5 = 0.7
        List<CompatibilityScorer> scorers = List.of(
                new TestScorer("scorer1", 0.2, 1.0),
                new TestScorer("scorer2", 0.3, 0.5)
        );
        ScoreAggregator aggregator = new ScoreAggregator(scorers);

        ScoredCandidate result = aggregator.aggregate(user, candidate);

        assertEquals(0.7, result.finalScore(), 0.001,
                "Should normalize weights not summing to 1");
    }

    @Test
    @DisplayName("All zeros should return 0.0")
    void testAllZeros() {
        List<CompatibilityScorer> scorers = List.of(
                new TestScorer("scorer1", 0.5, 0.0),
                new TestScorer("scorer2", 0.5, 0.0)
        );
        ScoreAggregator aggregator = new ScoreAggregator(scorers);

        ScoredCandidate result = aggregator.aggregate(user, candidate);

        assertEquals(0.0, result.finalScore(), 0.001,
                "All zeros should return 0.0");
    }

    @Test
    @DisplayName("All ones should return 1.0")
    void testAllOnes() {
        List<CompatibilityScorer> scorers = List.of(
                new TestScorer("scorer1", 0.3, 1.0),
                new TestScorer("scorer2", 0.7, 1.0)
        );
        ScoreAggregator aggregator = new ScoreAggregator(scorers);

        ScoredCandidate result = aggregator.aggregate(user, candidate);

        assertEquals(1.0, result.finalScore(), 0.001,
                "All ones should return 1.0");
    }

    // =========================================================================
    // DISABLED SCORER TESTS
    // =========================================================================

    @Test
    @DisplayName("Zero weight scorer should be skipped")
    void testZeroWeightScorer() {
        List<CompatibilityScorer> scorers = List.of(
                new TestScorer("enabled", 1.0, 0.8),
                new TestScorer("disabled", 0.0, 0.0)  // Should be skipped
        );
        ScoreAggregator aggregator = new ScoreAggregator(scorers);

        ScoredCandidate result = aggregator.aggregate(user, candidate);

        assertEquals(0.8, result.finalScore(), 0.001,
                "Zero weight should be skipped");
        // Breakdown should only have enabled scorer
        assertEquals(1, result.scoreBreakdown().size());
        assertTrue(result.scoreBreakdown().containsKey("enabled"));
    }

    @Test
    @DisplayName("Negative weight scorer should be skipped")
    void testNegativeWeightScorer() {
        List<CompatibilityScorer> scorers = List.of(
                new TestScorer("enabled", 1.0, 0.8),
                new TestScorer("negative", -0.5, 1.0)  // Should be skipped
        );
        ScoreAggregator aggregator = new ScoreAggregator(scorers);

        ScoredCandidate result = aggregator.aggregate(user, candidate);

        assertEquals(0.8, result.finalScore(), 0.001);
        assertEquals(1, result.scoreBreakdown().size());
    }

    // =========================================================================
    // BREAKDOWN TESTS
    // =========================================================================

    @Test
    @DisplayName("Breakdown should contain all active scorers")
    void testBreakdownContainsAllScorers() {
        List<CompatibilityScorer> scorers = List.of(
                new TestScorer("age", 0.2, 1.0),
                new TestScorer("location", 0.3, 0.8),
                new TestScorer("interests", 0.25, 0.6)
        );
        ScoreAggregator aggregator = new ScoreAggregator(scorers);

        ScoredCandidate result = aggregator.aggregate(user, candidate);

        assertEquals(3, result.scoreBreakdown().size());
        assertEquals(1.0, result.scoreBreakdown().get("age"));
        assertEquals(0.8, result.scoreBreakdown().get("location"));
        assertEquals(0.6, result.scoreBreakdown().get("interests"));
    }

    // =========================================================================
    // EDGE CASE TESTS
    // =========================================================================

    @Test
    @DisplayName("Empty scorer list should return 0.0")
    void testEmptyScorerList() {
        ScoreAggregator aggregator = new ScoreAggregator(List.of());

        ScoredCandidate result = aggregator.aggregate(user, candidate);

        assertEquals(0.0, result.finalScore(), 0.001,
                "Empty list should return 0.0");
        assertTrue(result.scoreBreakdown().isEmpty());
    }

    @Test
    @DisplayName("All scorers disabled should return 0.0")
    void testAllScorersDisabled() {
        List<CompatibilityScorer> scorers = List.of(
                new TestScorer("disabled1", 0.0, 1.0),
                new TestScorer("disabled2", 0.0, 1.0)
        );
        ScoreAggregator aggregator = new ScoreAggregator(scorers);

        ScoredCandidate result = aggregator.aggregate(user, candidate);

        assertEquals(0.0, result.finalScore(), 0.001);
        assertTrue(result.scoreBreakdown().isEmpty());
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    @Test
    @DisplayName("getScorerWeights should return all weights")
    void testGetScorerWeights() {
        List<CompatibilityScorer> scorers = List.of(
                new TestScorer("age", 0.2, 1.0),
                new TestScorer("location", 0.3, 0.8)
        );
        ScoreAggregator aggregator = new ScoreAggregator(scorers);

        var weights = aggregator.getScorerWeights();

        assertEquals(2, weights.size());
        assertEquals(0.2, weights.get("age"));
        assertEquals(0.3, weights.get("location"));
    }

    @Test
    @DisplayName("getActiveScorerCount should exclude disabled")
    void testGetActiveScorerCount() {
        List<CompatibilityScorer> scorers = List.of(
                new TestScorer("enabled1", 0.2, 1.0),
                new TestScorer("enabled2", 0.3, 0.8),
                new TestScorer("disabled", 0.0, 1.0)
        );
        ScoreAggregator aggregator = new ScoreAggregator(scorers);

        assertEquals(2, aggregator.getActiveScorerCount());
    }

    // =========================================================================
    // TEST SCORER IMPLEMENTATION
    // =========================================================================

    /**
     * Simple scorer for testing that returns a fixed score.
     */
    private static class TestScorer implements CompatibilityScorer {
        private final String name;
        private final double weight;
        private final double score;

        TestScorer(String name, double weight, double score) {
            this.name = name;
            this.weight = weight;
            this.score = score;
        }

        @Override
        public double calculateScore(User user, User candidate) {
            return score;
        }

        @Override
        public double getWeight() {
            return weight;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
