package com.dating.recommendation.service;

import com.dating.recommendation.dto.ScoredCandidate;
import com.dating.recommendation.exception.UserNotFoundException;
import com.dating.recommendation.model.User;
import com.dating.recommendation.repository.SwipeRepository;
import com.dating.recommendation.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ============================================================================
 * RECOMMENDATION SERVICE
 * ============================================================================
 *
 * PURPOSE:
 * Main entry point for generating user recommendations.
 * Orchestrates the recommendation generation process.
 *
 * HOW IT WORKS:
 * 1. Load the requesting user from database
 * 2. Get candidate users (exclude already swiped, blocked, self)
 * 3. Score each candidate using ScoreAggregator
 * 4. Filter by minimum score threshold
 * 5. Sort by score (highest first)
 * 6. Return top N candidates
 *
 * CACHING:
 * Recommendations are cached in Redis for 24 hours.
 * Cache is invalidated when:
 * - User updates preferences
 * - User swipes (to exclude swiped users)
 * - Time-based expiry
 *
 * PERFORMANCE CONSIDERATIONS:
 * - Cache hit: <50ms
 * - Cache miss: ~500ms (depending on user count)
 * - For 10K users: Score ~1000 candidates in ~100ms
 *
 * HOW TO MODIFY:
 * - Change candidate fetching: Modify getCandidates()
 * - Change filtering: Modify filter step in getRecommendations()
 * - Change sorting: Modify sort logic or add diversity
 * - Change batch size: Update application.yml
 *
 * CONFIGURATION:
 * application.yml:
 *   recommendation:
 *     batch-size: 20              # Number of recommendations to return
 *     minimum-score: 0.3          # Minimum score threshold
 *     refresh-interval-hours: 24  # Cache TTL
 *
 * ============================================================================
 */
@Service
@Slf4j
public class RecommendationService {

    // -------------------------------------------------------------------------
    // DEPENDENCIES
    // -------------------------------------------------------------------------

    /**
     * Repository for accessing user data.
     */
    private final UserRepository userRepository;

    /**
     * Repository for accessing swipe history.
     */
    private final SwipeRepository swipeRepository;

    /**
     * Aggregator that combines scores from all registered scorers.
     */
    private final ScoreAggregator scoreAggregator;

    // -------------------------------------------------------------------------
    // METRICS
    // -------------------------------------------------------------------------

    /**
     * MeterRegistry for custom metrics.
     */
    private final MeterRegistry meterRegistry;

    /**
     * Counter for tracking total recommendations generated.
     */
    private final Counter recommendationsGenerated;

    /**
     * Timer for tracking recommendation generation time.
     */
    private final Timer recommendationTimer;

    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public RecommendationService(UserRepository userRepository,
                                  SwipeRepository swipeRepository,
                                  ScoreAggregator scoreAggregator,
                                  MeterRegistry meterRegistry) {
        this.userRepository = userRepository;
        this.swipeRepository = swipeRepository;
        this.scoreAggregator = scoreAggregator;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.recommendationsGenerated = Counter.builder("recommendations.generated.total")
            .description("Total number of recommendation batches generated")
            .register(meterRegistry);

        this.recommendationTimer = Timer.builder("recommendations.generation.time")
            .description("Time taken to generate recommendations")
            .register(meterRegistry);
    }

    // -------------------------------------------------------------------------
    // CONFIGURATION
    // -------------------------------------------------------------------------

    /**
     * Number of recommendations to return per request.
     * Default: 20
     *
     * CONSIDERATIONS:
     * - Too few: User runs out of profiles quickly
     * - Too many: UI performance, unnecessary computation
     * - 20: Good balance for typical session
     */
    @Value("${recommendation.batch-size:20}")
    private int batchSize;

    /**
     * Minimum score to include in results.
     * Candidates below this score are filtered out.
     * Default: 0.3 (30%)
     *
     * CONSIDERATIONS:
     * - Too low (0.1): Shows poor matches, wastes user time
     * - Too high (0.8): May not have enough candidates
     * - 0.3: Shows reasonable matches, filters clear mismatches
     */
    @Value("${recommendation.minimum-score:0.3}")
    private double minimumScore;

    // -------------------------------------------------------------------------
    // PUBLIC METHODS
    // -------------------------------------------------------------------------

    /**
     * Generate recommendations for a user.
     *
     * ALGORITHM:
     * 1. Load user
     * 2. Get candidate users (exclude swiped, blocked, self)
     * 3. Score each candidate
     * 4. Filter by minimum score
     * 5. Sort by score (highest first)
     * 6. Return top N
     *
     * CACHING:
     * Results are cached in Redis with key "recommendations:{userId}"
     * TTL: 24 hours (configured in Redis config)
     *
     * @param userId The user requesting recommendations
     * @return List of scored candidates, sorted by score (highest first)
     * @throws UserNotFoundException if user doesn't exist
     */
    @Cacheable(value = "recommendations", key = "#userId")
    public List<ScoredCandidate> getRecommendations(UUID userId) {
        log.info("Generating recommendations for user: {}", userId);
        Timer.Sample sample = Timer.start(meterRegistry);

        // =====================================================================
        // STEP 1: Load the requesting user
        // =====================================================================
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        log.debug("Loaded user: {} ({})", user.getUsername(), user.getId());

        // =====================================================================
        // STEP 2: Get candidate users
        // =====================================================================
        // Exclude:
        // - Already swiped users
        // - Self
        // - (Future: blocked users)
        List<User> candidates = getCandidates(user);
        log.debug("Found {} potential candidates for user {}", candidates.size(), userId);

        // =====================================================================
        // STEP 3: Score each candidate
        // =====================================================================
        // This is where the magic happens!
        // ScoreAggregator calls each registered CompatibilityScorer
        // and combines their weighted scores
        List<ScoredCandidate> scored = candidates.stream()
                .map(candidate -> scoreAggregator.aggregate(user, candidate))
                // =====================================================================
                // STEP 4: Filter by minimum score
                // =====================================================================
                // Remove candidates below threshold
                // This prevents showing clearly incompatible matches
                .filter(sc -> sc.finalScore() >= minimumScore)
                // =====================================================================
                // STEP 5: Sort by score (highest first)
                // =====================================================================
                // ScoredCandidate implements Comparable with descending order
                .sorted()
                // =====================================================================
                // STEP 6: Limit to batch size
                // =====================================================================
                .limit(batchSize)
                .toList();

        // Record metrics
        long elapsedMs = sample.stop(recommendationTimer) / 1_000_000; // Convert nanoseconds to ms
        recommendationsGenerated.increment();

        log.info("Generated {} recommendations for user {} in {}ms (from {} candidates)",
                scored.size(), userId, elapsedMs, candidates.size());

        // Log score statistics for monitoring
        if (!scored.isEmpty()) {
            double avgScore = scored.stream()
                    .mapToDouble(ScoredCandidate::finalScore)
                    .average()
                    .orElse(0);
            log.debug("Score stats: avg={}, min={}, max={}",
                    String.format("%.3f", avgScore),
                    String.format("%.3f", scored.get(scored.size() - 1).finalScore()),
                    String.format("%.3f", scored.get(0).finalScore()));
        }

        return scored;
    }

    /**
     * Calculate compatibility score between two specific users.
     *
     * PURPOSE:
     * - Debug endpoint to understand scoring
     * - Used by Match Service to display "87% match"
     * - Analytics for algorithm tuning
     *
     * @param userId User 1 ID
     * @param targetId User 2 ID
     * @return ScoredCandidate with breakdown
     */
    public ScoredCandidate getCompatibilityScore(UUID userId, UUID targetId) {
        log.info("Calculating compatibility between {} and {}", userId, targetId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new UserNotFoundException(targetId));

        ScoredCandidate result = scoreAggregator.aggregate(user, target);

        log.info("Compatibility score: {} ({} breakdown factors)",
                result.finalScore(), result.scoreBreakdown().size());

        return result;
    }

    /**
     * Invalidate cached recommendations for a user.
     *
     * CALL THIS WHEN:
     * - User updates preferences
     * - User swipes (to exclude swiped user)
     * - User blocks someone
     *
     * @param userId User whose cache should be invalidated
     */
    @CacheEvict(value = "recommendations", key = "#userId")
    public void invalidateRecommendations(UUID userId) {
        log.info("Invalidated recommendations cache for user: {}", userId);
    }

    /**
     * Get algorithm configuration info.
     *
     * PURPOSE:
     * - Debug endpoint
     * - Admin dashboard
     * - Transparency about how recommendations work
     *
     * @return Map of configuration values
     */
    public Map<String, Object> getAlgorithmInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("batchSize", batchSize);
        info.put("minimumScore", minimumScore);
        info.put("scorers", scoreAggregator.getScorerWeights());
        info.put("activeScorerCount", scoreAggregator.getActiveScorerCount());
        return info;
    }

    // -------------------------------------------------------------------------
    // PRIVATE METHODS
    // -------------------------------------------------------------------------

    /**
     * Get candidate users for recommendation scoring.
     *
     * EXCLUSIONS:
     * - Already swiped users
     * - Self
     * - (Future: blocked users)
     *
     * HOW TO MODIFY:
     * - Add pre-filtering by gender/age at database level
     * - Add pagination for very large user bases
     * - Add geographic pre-filtering using PostGIS
     *
     * @param user The user requesting recommendations
     * @return List of candidate users
     */
    private List<User> getCandidates(User user) {
        // =====================================================================
        // Get users to exclude
        // =====================================================================
        Set<UUID> swipedUserIds = swipeRepository.findSwipedUserIds(user.getId());

        // Build exclude set
        Set<UUID> excludeIds = new HashSet<>(swipedUserIds);
        excludeIds.add(user.getId()); // Exclude self

        log.debug("Excluding {} users ({} swiped + self) from candidates",
                excludeIds.size(), swipedUserIds.size());

        // =====================================================================
        // Query candidates
        // =====================================================================
        // Option 1: Basic query (current)
        List<User> candidates = userRepository.findCandidates(excludeIds);

        // Option 2: Pre-filtered query (more efficient for large user bases)
        // Uncomment to use database-level filtering:
        //
        // List<User> candidates = userRepository.findPreFilteredCandidates(
        //         excludeIds,
        //         user.getGender(),
        //         user.getAge(),
        //         user.getGenderPreferences()
        // );

        return candidates;
    }
}
