# Recommendation Algorithm Implementation Prompt

## Context
You are implementing a **fully modular, deeply documented** recommendation/matching algorithm for a POC Dating application. The user needs to easily understand, tweak, and completely replace any part of this system. You have **full internet access** to research recommendation algorithms, scoring strategies, and matching systems.

**Scale:** 100-10K users (small scale - simplicity over performance)

**Critical Requirement:** Every line of code must be documented. Every decision must be explained. This is a learning/experimental system that will be heavily modified.

## ⚠️ CRITICAL: Code Quality Requirements

**WRITE HYPER-DOCUMENTED, FULLY MODULAR CODE.**

This is non-negotiable. Every component must be:
- **MODULAR** - Each scoring factor is a separate, swappable component
- **CONFIGURABLE** - All weights and thresholds in application.yml, not hardcoded
- **DOCUMENTED** - Inline comments explaining WHAT, WHY, and HOW TO MODIFY
- **TESTABLE** - Each component can be tested independently
- **REPLACEABLE** - Can swap entire algorithm with one line change

**Documentation Rules:**
```java
/**
 * ============================================================================
 * AGE COMPATIBILITY SCORER
 * ============================================================================
 *
 * PURPOSE:
 * Calculates how well two users match based on age preferences.
 *
 * HOW IT WORKS:
 * 1. Checks if each user falls within the other's preferred age range
 * 2. Returns 1.0 for mutual match, 0.5 for one-way, 0.0 for no match
 *
 * HOW TO MODIFY:
 * - To make age less important: reduce weight in application.yml
 * - To add age gap penalty: modify calculateScore() to penalize large gaps
 * - To ignore age entirely: set weight to 0 or remove from scorer list
 *
 * CONFIGURATION:
 * recommendation.scorers.age.weight: 0.2  (20% of total score)
 *
 * ============================================================================
 */
@Component
public class AgeCompatibilityScorer implements CompatibilityScorer {

    // -------------------------------------------------------------------------
    // Weight determines how much this factor affects the final score.
    // Range: 0.0 to 1.0
    // Default: 0.2 (age is 20% of the total compatibility score)
    //
    // TO ADJUST: Change 'recommendation.scorers.age.weight' in application.yml
    // -------------------------------------------------------------------------
    @Value("${recommendation.scorers.age.weight:0.2}")
    private double weight;

    @Override
    public double calculateScore(User user, User candidate) {
        // =====================================================================
        // STEP 1: Check if candidate's age is within user's preferred range
        // =====================================================================
        int candidateAge = candidate.getAge();
        boolean userAcceptsCandidate =
            candidateAge >= user.getMinAgePreference() &&
            candidateAge <= user.getMaxAgePreference();

        // =====================================================================
        // STEP 2: Check if user's age is within candidate's preferred range
        // =====================================================================
        int userAge = user.getAge();
        boolean candidateAcceptsUser =
            userAge >= candidate.getMinAgePreference() &&
            userAge <= candidate.getMaxAgePreference();

        // =====================================================================
        // STEP 3: Calculate score based on mutual acceptance
        //
        // SCORING LOGIC (modify this to change behavior):
        // - Both accept each other: 1.0 (perfect match)
        // - Only one accepts: 0.5 (partial match - still show but lower ranked)
        // - Neither accepts: 0.0 (no match - might filter out entirely)
        //
        // TO MODIFY: Change these return values to adjust scoring behavior
        // =====================================================================
        if (userAcceptsCandidate && candidateAcceptsUser) {
            return 1.0;  // Mutual acceptance - highest score
        } else if (userAcceptsCandidate || candidateAcceptsUser) {
            return 0.5;  // One-way acceptance - show but rank lower
        } else {
            return 0.0;  // No acceptance - consider filtering out
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
```

**Why This Matters:** You will definitely want to change how matching works. Modular, documented code lets you experiment without breaking everything.

## Scope
Implement a pluggable recommendation system with these components:

1. **Core interfaces** - Define contracts for scorers
2. **Individual scorers** - One class per scoring factor
3. **Aggregator** - Combines scores from all scorers
4. **Configuration** - All weights in application.yml
5. **Service layer** - Orchestrates recommendation generation

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                 RecommendationService                    │
│  (Orchestrates the recommendation generation process)    │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│              ScoreAggregator                             │
│  (Combines scores from all scorers using weights)        │
└─────────────────┬───────────────────────────────────────┘
                  │
        ┌─────────┼─────────┬─────────┬─────────┐
        ▼         ▼         ▼         ▼         ▼
   ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
   │  Age    │ │Location │ │Interest │ │Activity │ │ Custom  │
   │ Scorer  │ │ Scorer  │ │ Scorer  │ │ Scorer  │ │ Scorer  │
   └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘

Each scorer implements CompatibilityScorer interface.
Add new scorers by creating a new @Component class.
Remove scorers by deleting the class or setting weight to 0.
```

## Implementation Tasks

### Task 1: Core Interfaces

```java
/**
 * ============================================================================
 * COMPATIBILITY SCORER INTERFACE
 * ============================================================================
 *
 * PURPOSE:
 * Contract that all scoring components must implement.
 *
 * HOW TO ADD A NEW SCORER:
 * 1. Create a new class that implements this interface
 * 2. Add @Component annotation
 * 3. Add weight configuration to application.yml
 * 4. The aggregator will automatically pick it up
 *
 * ============================================================================
 */
public interface CompatibilityScorer {

    /**
     * Calculate compatibility score between two users.
     *
     * @param user The user requesting recommendations
     * @param candidate A potential match candidate
     * @return Score between 0.0 (no match) and 1.0 (perfect match)
     */
    double calculateScore(User user, User candidate);

    /**
     * Get the weight of this scorer in the final calculation.
     *
     * @return Weight between 0.0 and 1.0
     */
    double getWeight();

    /**
     * Get the name of this scorer for logging/debugging.
     *
     * @return Human-readable name
     */
    String getName();
}
```

### Task 2: Score Aggregator

```java
/**
 * ============================================================================
 * SCORE AGGREGATOR
 * ============================================================================
 *
 * PURPOSE:
 * Combines scores from all registered scorers into a final score.
 *
 * HOW IT WORKS:
 * 1. Collects all @Component classes that implement CompatibilityScorer
 * 2. Calls each scorer to get individual scores
 * 3. Multiplies each score by its weight
 * 4. Sums weighted scores and normalizes to 0.0-1.0 range
 *
 * FORMULA:
 * finalScore = (score1 * weight1 + score2 * weight2 + ...) / totalWeight
 *
 * HOW TO MODIFY:
 * - To change aggregation method: modify aggregate() method
 * - To add minimum thresholds: add filtering logic
 * - To use max instead of weighted average: change the formula
 *
 * ============================================================================
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScoreAggregator {

    // -------------------------------------------------------------------------
    // Spring automatically injects ALL beans that implement CompatibilityScorer.
    // To add a new scorer, just create a new @Component class.
    // To remove a scorer, delete the class or set its weight to 0.
    // -------------------------------------------------------------------------
    private final List<CompatibilityScorer> scorers;

    /**
     * Calculate the final compatibility score between two users.
     *
     * @param user The user requesting recommendations
     * @param candidate A potential match candidate
     * @return Aggregated score between 0.0 and 1.0
     */
    public ScoredCandidate aggregate(User user, User candidate) {
        // =====================================================================
        // STEP 1: Calculate individual scores from each scorer
        // =====================================================================
        Map<String, Double> individualScores = new HashMap<>();
        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (CompatibilityScorer scorer : scorers) {
            // Skip scorers with zero weight (disabled)
            if (scorer.getWeight() <= 0) {
                continue;
            }

            double score = scorer.calculateScore(user, candidate);
            double weight = scorer.getWeight();

            // Store for debugging/transparency
            individualScores.put(scorer.getName(), score);

            // Add to weighted sum
            weightedSum += score * weight;
            totalWeight += weight;

            log.debug("Scorer '{}': score={}, weight={}",
                scorer.getName(), score, weight);
        }

        // =====================================================================
        // STEP 2: Normalize to 0.0-1.0 range
        //
        // WHY NORMALIZE: Weights might not sum to 1.0 (user can configure any values)
        // =====================================================================
        double finalScore = totalWeight > 0 ? weightedSum / totalWeight : 0.0;

        log.debug("Final score for candidate {}: {} (from {} scorers)",
            candidate.getId(), finalScore, scorers.size());

        // =====================================================================
        // STEP 3: Return result with breakdown for transparency
        // =====================================================================
        return new ScoredCandidate(candidate, finalScore, individualScores);
    }
}

/**
 * Holds a candidate with their score and breakdown.
 * The breakdown helps users understand WHY they got this recommendation.
 */
public record ScoredCandidate(
    User candidate,
    double finalScore,
    Map<String, Double> scoreBreakdown  // Individual scores for transparency
) implements Comparable<ScoredCandidate> {

    @Override
    public int compareTo(ScoredCandidate other) {
        // Higher scores first
        return Double.compare(other.finalScore, this.finalScore);
    }
}
```

### Task 3: Individual Scorers

Create these scorers (one file each):

```java
// =============================================================================
// FILE: AgeCompatibilityScorer.java
// PURPOSE: Score based on age preferences
// CONFIG: recommendation.scorers.age.weight
// =============================================================================

// =============================================================================
// FILE: LocationScorer.java
// PURPOSE: Score based on distance between users
// CONFIG: recommendation.scorers.location.weight
//         recommendation.scorers.location.max-distance-km
// =============================================================================
@Component
public class LocationScorer implements CompatibilityScorer {

    @Value("${recommendation.scorers.location.weight:0.3}")
    private double weight;

    // -------------------------------------------------------------------------
    // Maximum distance in kilometers to consider.
    // Beyond this distance, score is 0.
    // TO ADJUST: Change in application.yml
    // -------------------------------------------------------------------------
    @Value("${recommendation.scorers.location.max-distance-km:50}")
    private double maxDistanceKm;

    @Override
    public double calculateScore(User user, User candidate) {
        // =====================================================================
        // STEP 1: Calculate distance using Haversine formula
        //
        // WHY HAVERSINE: Accounts for Earth's curvature, accurate for distances
        // =====================================================================
        double distance = calculateHaversineDistance(
            user.getLatitude(), user.getLongitude(),
            candidate.getLatitude(), candidate.getLongitude()
        );

        // =====================================================================
        // STEP 2: Convert distance to score
        //
        // SCORING LOGIC:
        // - 0 km = 1.0 (same location, perfect)
        // - maxDistanceKm = 0.0 (too far)
        // - Linear interpolation in between
        //
        // TO MODIFY:
        // - For exponential decay: score = Math.exp(-distance / maxDistanceKm)
        // - For step function: if (distance < 10) return 1.0; else return 0.0;
        // =====================================================================
        if (distance >= maxDistanceKm) {
            return 0.0;
        }

        return 1.0 - (distance / maxDistanceKm);
    }

    private double calculateHaversineDistance(double lat1, double lon1,
                                               double lat2, double lon2) {
        // Haversine formula implementation
        // ... (standard implementation)
    }
}

// =============================================================================
// FILE: InterestScorer.java
// PURPOSE: Score based on shared interests
// CONFIG: recommendation.scorers.interests.weight
// =============================================================================
@Component
public class InterestScorer implements CompatibilityScorer {

    @Value("${recommendation.scorers.interests.weight:0.25}")
    private double weight;

    @Override
    public double calculateScore(User user, User candidate) {
        // =====================================================================
        // STEP 1: Get interests for both users
        // =====================================================================
        Set<String> userInterests = user.getInterests();
        Set<String> candidateInterests = candidate.getInterests();

        if (userInterests.isEmpty() || candidateInterests.isEmpty()) {
            // No interests = neutral score (don't penalize)
            return 0.5;
        }

        // =====================================================================
        // STEP 2: Calculate Jaccard similarity
        //
        // FORMULA: |intersection| / |union|
        // WHY JACCARD: Standard similarity metric, 0-1 range, handles different sizes
        //
        // TO MODIFY:
        // - For weighted interests: multiply by interest importance
        // - For minimum threshold: return 0 if shared < 3
        // =====================================================================
        Set<String> intersection = new HashSet<>(userInterests);
        intersection.retainAll(candidateInterests);

        Set<String> union = new HashSet<>(userInterests);
        union.addAll(candidateInterests);

        return (double) intersection.size() / union.size();
    }
}

// =============================================================================
// FILE: ActivityScorer.java
// PURPOSE: Score based on recent activity (prefer active users)
// CONFIG: recommendation.scorers.activity.weight
//         recommendation.scorers.activity.inactive-days-threshold
// =============================================================================
@Component
public class ActivityScorer implements CompatibilityScorer {

    @Value("${recommendation.scorers.activity.weight:0.15}")
    private double weight;

    @Value("${recommendation.scorers.activity.inactive-days-threshold:30}")
    private int inactiveDaysThreshold;

    @Override
    public double calculateScore(User user, User candidate) {
        // =====================================================================
        // SCORING LOGIC:
        // - Active today: 1.0
        // - Inactive for threshold days: 0.0
        // - Linear decay in between
        //
        // WHY INCLUDE: Users prefer matches who will actually respond
        // =====================================================================
        long daysSinceActive = ChronoUnit.DAYS.between(
            candidate.getLastActiveAt(), LocalDateTime.now()
        );

        if (daysSinceActive >= inactiveDaysThreshold) {
            return 0.0;
        }

        return 1.0 - ((double) daysSinceActive / inactiveDaysThreshold);
    }
}

// =============================================================================
// FILE: GenderPreferenceScorer.java
// PURPOSE: Filter by gender preference (hard filter, not soft score)
// CONFIG: recommendation.scorers.gender.weight
// =============================================================================
@Component
public class GenderPreferenceScorer implements CompatibilityScorer {

    @Value("${recommendation.scorers.gender.weight:0.1}")
    private double weight;

    @Override
    public double calculateScore(User user, User candidate) {
        // =====================================================================
        // HARD FILTER: Gender must match preferences
        //
        // This is binary (1.0 or 0.0) because gender preference is usually strict.
        // =====================================================================
        boolean userAcceptsCandidate = user.getGenderPreferences()
            .contains(candidate.getGender());

        boolean candidateAcceptsUser = candidate.getGenderPreferences()
            .contains(user.getGender());

        if (userAcceptsCandidate && candidateAcceptsUser) {
            return 1.0;
        } else {
            return 0.0;  // Hard filter - will likely be excluded
        }
    }
}
```

### Task 4: Configuration

**application.yml:**
```yaml
# ==============================================================================
# RECOMMENDATION ALGORITHM CONFIGURATION
# ==============================================================================
#
# HOW TO MODIFY:
# - To make a factor more important: increase its weight
# - To disable a factor: set weight to 0
# - Weights don't need to sum to 1.0 (they're normalized automatically)
#
# EXAMPLE PROFILES:
#
# Profile: "Location First" (for users who want nearby matches)
#   location.weight: 0.5
#   age.weight: 0.2
#   interests.weight: 0.2
#   activity.weight: 0.1
#
# Profile: "Interests First" (for users who want compatible matches)
#   interests.weight: 0.5
#   age.weight: 0.2
#   location.weight: 0.2
#   activity.weight: 0.1
#
# ==============================================================================
recommendation:
  scorers:
    age:
      weight: 0.2
    location:
      weight: 0.3
      max-distance-km: 50
    interests:
      weight: 0.25
    activity:
      weight: 0.15
      inactive-days-threshold: 30
    gender:
      weight: 0.1

  # Number of recommendations to generate per request
  batch-size: 20

  # Minimum score to include in results (0.0 to 1.0)
  # Candidates below this score are filtered out
  minimum-score: 0.3

  # How often to refresh recommendations (in hours)
  refresh-interval-hours: 24
```

### Task 5: Recommendation Service

```java
/**
 * ============================================================================
 * RECOMMENDATION SERVICE
 * ============================================================================
 *
 * PURPOSE:
 * Main entry point for generating user recommendations.
 *
 * HOW IT WORKS:
 * 1. Gets candidate users from database (filters out already swiped, blocked)
 * 2. Scores each candidate using the aggregator
 * 3. Filters by minimum score threshold
 * 4. Sorts by score (highest first)
 * 5. Returns top N candidates
 *
 * HOW TO MODIFY:
 * - Change candidate fetching: modify getCandidates()
 * - Change filtering: modify filterCandidates()
 * - Change sorting: modify sortCandidates()
 * - Change batch size: update application.yml
 *
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final UserRepository userRepository;
    private final SwipeRepository swipeRepository;
    private final ScoreAggregator scoreAggregator;

    @Value("${recommendation.batch-size:20}")
    private int batchSize;

    @Value("${recommendation.minimum-score:0.3}")
    private double minimumScore;

    /**
     * Generate recommendations for a user.
     *
     * @param userId The user requesting recommendations
     * @return List of scored candidates, sorted by score (highest first)
     */
    @Cacheable(value = "recommendations", key = "#userId")
    public List<ScoredCandidate> getRecommendations(UUID userId) {
        log.info("Generating recommendations for user: {}", userId);

        // =====================================================================
        // STEP 1: Load the requesting user
        // =====================================================================
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        // =====================================================================
        // STEP 2: Get potential candidates
        //
        // FILTERING:
        // - Exclude already swiped users
        // - Exclude blocked users
        // - Exclude self
        // =====================================================================
        List<User> candidates = getCandidates(user);
        log.debug("Found {} potential candidates", candidates.size());

        // =====================================================================
        // STEP 3: Score each candidate
        // =====================================================================
        List<ScoredCandidate> scored = candidates.stream()
            .map(candidate -> scoreAggregator.aggregate(user, candidate))
            .filter(sc -> sc.finalScore() >= minimumScore)  // Apply threshold
            .sorted()  // Highest score first
            .limit(batchSize)
            .toList();

        log.info("Generated {} recommendations for user {} (from {} candidates)",
            scored.size(), userId, candidates.size());

        return scored;
    }

    private List<User> getCandidates(User user) {
        // Get users not yet swiped
        Set<UUID> swipedUserIds = swipeRepository.findSwipedUserIds(user.getId());
        Set<UUID> blockedUserIds = userRepository.findBlockedUserIds(user.getId());

        Set<UUID> excludeIds = new HashSet<>();
        excludeIds.addAll(swipedUserIds);
        excludeIds.addAll(blockedUserIds);
        excludeIds.add(user.getId());  // Exclude self

        return userRepository.findCandidates(excludeIds);
    }
}
```

## Iteration Loop (Repeat Until Complete)

### Phase 1: Create Interfaces and Aggregator
```bash
# Create and compile
cd backend/recommendation-service
mvn compile
```
- If compilation fails → fix interfaces → rebuild

### Phase 2: Implement Individual Scorers
For each scorer:
```bash
# Add scorer, compile, test
mvn test -Dtest=AgeCompatibilityScorerTest
```
- If scorer logic is wrong → fix → retest

### Phase 3: Configuration
```bash
# Update application.yml with all weights
# Start service and verify config loads
mvn spring-boot:run
curl localhost:8084/actuator/env | grep recommendation
```

### Phase 4: Integration Test
```bash
# Test full recommendation flow
curl -H "Authorization: Bearer <token>" localhost:8084/api/recommendations
```
- Verify scores are calculated correctly
- Verify sorting works
- Verify minimum threshold filters

### Phase 5: Documentation Review
- Read through every comment
- Ensure "HOW TO MODIFY" sections are clear
- Ensure configuration is documented

## Success Criteria
- [ ] All scorers implement CompatibilityScorer interface
- [ ] Each scorer has comprehensive inline documentation
- [ ] All weights are configurable in application.yml
- [ ] Score aggregation works correctly
- [ ] Minimum score filtering works
- [ ] Results are sorted by score (highest first)
- [ ] Each scorer can be tested independently
- [ ] "HOW TO MODIFY" comments are clear and actionable
- [ ] User can add a new scorer by creating one @Component class

## When Stuck
1. **Search internet** for recommendation algorithms, scoring systems
2. **Check:** logs for individual scorer outputs
3. **Test:** scorers individually with unit tests

## DO NOT
- Hardcode weights (must be in application.yml)
- Create monolithic scorer classes (one responsibility per scorer)
- Skip documentation (this is the most critical part)
- Use complex ML algorithms (keep it simple, scorable)
- Forget to explain WHY each scorer exists

## How to Add a New Scorer (Example)
```java
// 1. Create new class
@Component
public class ProfileCompletenessScorer implements CompatibilityScorer {
    @Value("${recommendation.scorers.completeness.weight:0.1}")
    private double weight;

    @Override
    public double calculateScore(User user, User candidate) {
        // Score based on how complete candidate's profile is
        int fields = countFilledFields(candidate);
        return fields / 10.0;  // Assuming 10 possible fields
    }
}

// 2. Add to application.yml
recommendation:
  scorers:
    completeness:
      weight: 0.1

// 3. Done! Aggregator automatically picks it up.
```

---
**Iterate until all scorers are documented and configurable. Use internet access freely to research scoring algorithms.**
