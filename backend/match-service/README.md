# Match Service

## Overview

Microservice responsible for handling swipes and calculating mutual matches between users.

## Port
**8082** (internal, accessed via API Gateway)

## Responsibilities

### Swipe Recording
- Record user swipes (like/pass)
- Validate swipe eligibility
- Prevent duplicate swipes
- Track swipe history

### Match Calculation
- Detect mutual matches (both users swiped right on each other)
- Calculate match compatibility scores
- Generate match records

### Feed Generation
- Generate personalized feed of potential matches for users
- Apply user preference filters (age, distance, interests)
- Rank candidates by compatibility score
- Cache feed in Redis for quick access

## Database Schema

```
swipes
├── id (UUID, PK)
├── user_id (FK → users.id)
├── target_user_id (FK → users.id)
├── action (LIKE, PASS, SUPER_LIKE)
├── created_at
└── UNIQUE(user_id, target_user_id)

matches
├── id (UUID, PK)
├── user1_id (FK → users.id)
├── user2_id (FK → users.id)
├── matched_at (TIMESTAMP)
├── ended_at (nullable)
└── UNIQUE(user1_id, user2_id)

match_scores
├── id (UUID, PK)
├── match_id (FK)
├── score (0-100)
├── factors (JSON - interest match %, age match %, etc)
└── calculated_at
```

## API Endpoints

**Base URL:** http://localhost:8080/api/matches

### Swipe Actions
```
POST   /swipes              → Record a swipe (like/pass/super-like)
GET    /swipes/{userId}     → Get swipe history
GET    /feed/{userId}       → Get personalized feed of matches
```

### Match Management
```
GET    /matches/{userId}    → Get all matches for user
GET    /{matchId}           → Get match details
DELETE /{matchId}           → End match (unmatch)
```

### Match Quality
```
GET    /{matchId}/score     → Get compatibility score
GET    /{matchId}/factors   → Get score breakdown
```

## Events Published

### Via RabbitMQ
```
match:created
├── Payload: MatchId, UserId1, UserId2, MatchedAt
├── Purpose: Notify both users of match
└── Consumed by: Chat Service, Notification Service

match:ended
├── Payload: MatchId, UnmatchedAt
├── Purpose: Cleanup match-related data
└── Consumed by: Chat Service

swipe:recorded
├── Payload: UserId, TargetId, Action, Timestamp
├── Purpose: Track engagement metrics
└── Consumed by: Analytics Service
```

## Events Consumed

```
user:preferences-updated
├── Source: User Service
├── Action: Regenerate user's feed with new preferences
└── Cache invalidation: Clear Redis cache for that user

user:deleted
├── Source: User Service
├── Action: Delete all swipes and matches involving user
└── Cleanup: Remove from cache
```

## Matching Algorithm

### Feed Generation Process

1. **Fetch Candidates**
   - Get all active users excluding already-swiped

2. **Apply Preferences**
   - Age range filter
   - Distance filter
   - Interest overlap

3. **Score Calculation**
   - Interest match percentage (0-40 points)
   - Age compatibility (0-30 points)
   - Preference alignment (0-30 points)
   - Total: 0-100

4. **Rank & Cache**
   - Sort by score descending
   - Cache top 100 in Redis (24-hour TTL)
   - Serve paginated results

### Swipe Validation

```
ALLOWED IF:
✓ User not swiping self
✓ Target is active/not deleted
✓ No duplicate swipe exists
✓ Within preference range (age, distance)

BLOCKED IF:
✗ Blocked by target user
✗ Target blocked this user
✗ User deleted/suspended
✗ Swipe rate limit exceeded
```

## Caching Strategy

```
Redis Keys:
├── feed:{userId}:v2        → Cached feed (TTL: 24h)
├── swipes:{userId}         → Swipe count (TTL: 1h)
├── match:score:{matchId}   → Compatibility score (TTL: 30 days)
└── matches:{userId}        → User's matches (TTL: 1h)
```

## Performance Considerations

### Problem: N+1 Queries
**Solution:** Batch loading of user profiles when generating feed

### Problem: Feed Generation is Slow
**Solution:** Pre-generate and cache top feeds, update hourly

### Problem: Swipe Rate Limiting
**Solution:** Use Redis atomic counters with 5-minute windows

### Problem: Large Match Tables
**Solution:** Index on (user_id, created_at) and (target_user_id, created_at)

## Error Handling

```
400 Bad Request      → Invalid swipe action or user
401 Unauthorized     → Not authenticated
403 Forbidden        → User suspended
404 Not Found        → User/match not found
409 Conflict         → Duplicate swipe
429 Too Many Requests→ Swipe rate limit
500 Internal Server  → Matching algorithm error
```

## Testing

### Unit Tests
- Swipe validation logic
- Match calculation algorithm
- Score computation
- Feed ranking

### Integration Tests
- Database swipe recording
- Match detection
- Feed generation with test data
- Event publishing

### Load Testing
- High-volume swipe processing
- Feed generation performance
- Cache hit rates
- Database query optimization

## Future Enhancements

- Elo-based rating system
- Machine learning recommendations
- Trending/featured matches
- Location-based feeds
- Super Like features
- Unlimited swipes (premium)
- Undo swipe (premium)
- See who liked you (premium)
