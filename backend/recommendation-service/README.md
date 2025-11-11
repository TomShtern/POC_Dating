# Recommendation Service

## Overview

Microservice for generating personalized recommendations and improving matching algorithms through data analysis.

## Port
**8084** (internal, accessed via API Gateway)

## Responsibilities

### Recommendation Generation
- Analyze user preferences and behavior
- Generate personalized suggestions
- Calculate compatibility metrics
- Rank potential matches

### Algorithm Evolution
- A/B test different recommendation algorithms
- Track algorithm performance
- Learn from user interactions
- Improve over time

## Current Algorithms (POC)

### 1. Rule-Based Matching
```
Score = (interest_overlap * 0.4) + (age_compatibility * 0.3) +
         (preference_alignment * 0.3)

Where:
- interest_overlap: Shared interests / total interests
- age_compatibility: Inverse distance from age preference
- preference_alignment: Match with stated gender preference
```

### 2. Collaborative Filtering (Future)
```
Find users similar to you, recommend who they matched with
Uses item-based similarity (shared match patterns)
```

### 3. Deep Learning (Future)
```
Learn complex patterns from user behavior
Requires more historical data (6+ months)
```

## Database Schema

```
recommendations
├── id (UUID, PK)
├── user_id (FK)
├── recommended_user_id (FK)
├── score (0-100)
├── algorithm_version (v1, v2, etc)
├── factors (JSON)
├── created_at
├── expires_at
└── INDEX(user_id, created_at)

interaction_history
├── id (UUID, PK)
├── user_id (FK)
├── action (view, like, pass, match, message)
├── target_id (FK)
├── timestamp
└── INDEX(user_id, timestamp)

algorithm_performance
├── id (UUID, PK)
├── algorithm_version
├── metric (click_through_rate, match_rate, engagement)
├── value (percentage)
├── date
└── time_period (daily, weekly, monthly)
```

## API Endpoints

**Base URL:** http://localhost:8080/api/recommendations

### Get Recommendations
```
GET    /users/{userId}           → Get top 10 recommendations
GET    /users/{userId}?limit=20  → Get N recommendations
GET    /users/{userId}?algorithm=v2 → Get with specific algorithm
```

### Recommendation Details
```
GET    /{recommendationId}       → Get single recommendation with score breakdown
GET    /users/{userId}/{targetId}/score → Get score between two users
```

### Feedback (for Learning)
```
POST   /feedback/{recommendationId}/accepted  → User likes this recommendation
POST   /feedback/{recommendationId}/rejected  → User dislikes
```

## Events Published

### Via RabbitMQ
```
recommendation:generated
├── Payload: UserId, Recommendations[], Timestamp
├── Purpose: Track generation for analytics
└── Consumed by: Analytics/Logging

recommendation:accepted
├── Payload: UserId, RecommendationId
├── Purpose: Learn user preferences
└── Feedback loop for algorithm improvement

recommendation:rejected
├── Payload: UserId, RecommendationId
├── Purpose: Negative feedback signal
└── Avoid recommending similar users
```

## Events Consumed

```
user:preferences-updated
├── Source: User Service
├── Action: Regenerate recommendations with new preferences
└── Cache invalidation: Clear old recommendations

swipe:recorded
├── Source: Match Service
├── Action: Learn from swipe behavior
└── Update collaborative filtering data

match:created
├── Source: Match Service
├── Action: Track successful matches
└── Improve algorithm scoring
```

## Caching Strategy

```
Redis Keys:
├── rec:{userId}:v1          → Cached recommendations (TTL: 24h)
├── rec:{userId}:v2          → Alternative algorithm recs (TTL: 24h)
├── sim:{userId1}:{userId2}  → Precomputed similarity (TTL: 7d)
├── perf:{algo}:{date}       → Algorithm performance (TTL: 90d)
└── learning:{userId}        → Behavior patterns (TTL: 30d)
```

## Performance Considerations

### Computation Time
- Initial recommendations: ~500ms (first time, uses algorithm)
- Cached recommendations: < 50ms
- Score calculation: ~10ms per pair

### Optimization Strategies
1. **Pre-compute recommendations** in background job (nightly)
2. **Cache aggressively** with 24-hour TTL
3. **Batch score calculations** (compute top 100, not all users)
4. **Paginate results** (don't send 10k recommendations)

### Scalability
- Current: Single instance handles ~100k users
- Scaling: Add cron job for periodic recomputation
- Future: Distributed computation (Spark, Hadoop)

## Algorithm Metrics

### Track These
```
- Click-through rate: % of recommendations user interacts with
- Match rate: % of recommendations that result in matches
- Average score: Mean score of successful matches
- Algorithm A vs B: Which performs better
- Engagement time: How long users view recommendations
```

## Error Handling

```
400 Bad Request      → Invalid user ID or parameters
401 Unauthorized     → Not authenticated
404 Not Found        → User not found
500 Internal Server  → Algorithm computation error
```

## Testing

### Unit Tests
- Score calculation formulas
- Interest overlap computation
- Age compatibility calculation
- Ranking logic

### Integration Tests
- Event consumption
- Recommendation generation with test data
- Cache invalidation
- Performance benchmarks

### A/B Testing
- Test different algorithms
- Track metrics for each
- Statistical significance testing

## Future Enhancements

- Machine learning integration
- Location-based recommendations
- Trend detection ("hot profiles")
- Smart timing (suggest when user most active)
- Re-ranking based on real-time engagement
- Cross-service recommendation (app features)
- Diversity in recommendations (avoid echo chamber)
- Cold-start problem solving (new users)
- Serendipity factor (occasional random suggestions)
