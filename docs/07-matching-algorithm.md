# Matching Algorithm Implementation

## Overview

The matching algorithm is the core intelligence of a dating app. It determines which users are shown to each other in the discovery feed.

**Goals:**
- High-quality matches (compatible users)
- Fair distribution (avoid "rich get richer" effect)
- Performance (sub-100ms query time)
- Scalability (handle millions of users)

---

## Table of Contents

1. [Algorithm Architecture](#algorithm-architecture)
2. [Multi-Stage Filtering](#multi-stage-filtering)
3. [Scoring System](#scoring-system)
4. [Implementation](#implementation)
5. [Caching Strategy](#caching-strategy)
6. [Machine Learning Integration](#machine-learning-integration)

---

## Algorithm Architecture

```
┌──────────────────────────────────────────┐
│ 1. FILTERING STAGE                       │
│ - Geographic proximity                    │
│ - Age preferences                         │
│ - Gender preferences                      │
│ - Already swiped (exclude)                │
│ - Blocked users (exclude)                 │
│                                           │
│ Input: All users                          │
│ Output: ~1000 candidates                  │
└──────────────────────────────────────────┘
                   ↓
┌──────────────────────────────────────────┐
│ 2. SCORING STAGE                         │
│ - Calculate compatibility score           │
│ - Profile completeness                    │
│ - Activity level                          │
│ - Response rate                           │
│ - Shared interests                        │
│                                           │
│ Input: ~1000 candidates                   │
│ Output: Scored candidates                 │
└──────────────────────────────────────────┘
                   ↓
┌──────────────────────────────────────────┐
│ 3. RANKING STAGE                         │
│ - Sort by score                           │
│ - Apply diversity                         │
│ - Inject new users (fairness)            │
│ - Add randomness                          │
│                                           │
│ Input: Scored candidates                  │
│ Output: Top 50-100 matches                │
└──────────────────────────────────────────┘
                   ↓
┌──────────────────────────────────────────┐
│ 4. PRESENTATION                          │
│ - Fetch full profiles                     │
│ - Add distance info                       │
│ - Return to client                        │
└──────────────────────────────────────────┘
```

---

## Multi-Stage Filtering

### Stage 1: Geographic Filtering

Use Redis geospatial queries for fast proximity search.

```typescript
// src/services/match/utils/geoFilter.ts
import { redis } from '../../../cache/client';

export async function filterByLocation(
  userId: string,
  maxDistanceKm: number,
  limit: number = 1000
): Promise<Array<{ userId: string; distance: number }>> {
  // Get user's location from Redis
  const userLocation = await redis.geopos('users:locations', userId);

  if (!userLocation || !userLocation[0]) {
    throw new Error('User location not set');
  }

  const [lng, lat] = userLocation[0];

  // Find nearby users
  const nearby = await redis.georadius(
    'users:locations',
    parseFloat(lng),
    parseFloat(lat),
    maxDistanceKm,
    'km',
    'WITHDIST',
    'COUNT',
    limit,
    'ASC'
  );

  return nearby
    .filter(([uid]) => uid !== userId)
    .map(([userId, distance]) => ({
      userId,
      distance: parseFloat(distance),
    }));
}
```

---

### Stage 2: Preference Filtering

Apply age, gender, and other preference filters.

```typescript
// src/services/match/utils/preferenceFilter.ts
import { db } from '../../../db/client';
import { Prisma } from '@prisma/client';

interface FilterCriteria {
  userId: string;
  candidateIds: string[];
  preferences: {
    minAge: number;
    maxAge: number;
    interestedIn: string;
  };
}

export async function filterByPreferences({
  userId,
  candidateIds,
  preferences,
}: FilterCriteria): Promise<string[]> {
  const currentYear = new Date().getFullYear();

  // Calculate date of birth range
  const minDob = new Date(currentYear - preferences.maxAge, 0, 1);
  const maxDob = new Date(currentYear - preferences.minAge, 11, 31);

  // Query users matching criteria
  const users = await db.user.findMany({
    where: {
      id: { in: candidateIds },
      gender: preferences.interestedIn,
      dateOfBirth: {
        gte: minDob,
        lte: maxDob,
      },
      active: true,
      verified: true, // Only show verified users
      deletedAt: null,
    },
    select: { id: true },
  });

  return users.map(u => u.id);
}
```

---

### Stage 3: Exclusion Filtering

Exclude users already swiped on and blocked users.

```typescript
// src/services/match/utils/exclusionFilter.ts
import { db } from '../../../db/client';

export async function filterExclusions(
  userId: string,
  candidateIds: string[]
): Promise<string[]> {
  // Get already swiped users
  const swipedUsers = await db.swipe.findMany({
    where: { userId },
    select: { targetUserId: true },
  });

  const swipedIds = new Set(swipedUsers.map(s => s.targetUserId));

  // Get blocked users (both directions)
  const [blockedByUser, blockedUser] = await Promise.all([
    db.block.findMany({
      where: { userId },
      select: { blockedUserId: true },
    }),
    db.block.findMany({
      where: { blockedUserId: userId },
      select: { userId: true },
    }),
  ]);

  const blockedIds = new Set([
    ...blockedByUser.map(b => b.blockedUserId),
    ...blockedUser.map(b => b.userId),
  ]);

  // Filter out excluded users
  return candidateIds.filter(
    id => !swipedIds.has(id) && !blockedIds.has(id)
  );
}
```

---

## Scoring System

### Compatibility Score Calculation

Multi-factor scoring algorithm:

```typescript
// src/services/match/utils/scoring.ts
import { db } from '../../../db/client';

export interface UserProfile {
  id: string;
  dateOfBirth: Date;
  bio: string | null;
  photos: Array<{ id: string }>;
  preferences: {
    minAge: number;
    maxAge: number;
    interestedIn: string;
  } | null;
  lastSeen: Date | null;
  createdAt: Date;
}

export async function calculateCompatibilityScore(
  user: UserProfile,
  candidate: UserProfile
): Promise<number> {
  let score = 0;

  // 1. Profile Completeness (15 points max)
  score += calculateProfileCompletenessScore(candidate);

  // 2. Activity Level (20 points max)
  score += calculateActivityScore(candidate);

  // 3. Mutual Interest (25 points max)
  score += await calculateMutualInterestScore(user, candidate);

  // 4. Response Rate (15 points max)
  score += await calculateResponseRateScore(candidate.id);

  // 5. Freshness Boost (10 points max)
  score += calculateFreshnessBoost(candidate);

  // 6. Distance Penalty (up to -10 points)
  // Handled separately after we have distance from geo query

  return Math.min(Math.max(score, 0), 100);
}

// Profile completeness: more complete = higher score
function calculateProfileCompletenessScore(candidate: UserProfile): number {
  let score = 0;

  // Has bio
  if (candidate.bio && candidate.bio.length > 50) {
    score += 5;
  }

  // Has multiple photos
  const photoCount = candidate.photos.length;
  if (photoCount >= 1) score += 2;
  if (photoCount >= 3) score += 3;
  if (photoCount >= 5) score += 5;

  return Math.min(score, 15);
}

// Activity level: recent activity = higher score
function calculateActivityScore(candidate: UserProfile): number {
  if (!candidate.lastSeen) return 0;

  const hoursSinceActive = (Date.now() - candidate.lastSeen.getTime()) / (1000 * 60 * 60);

  if (hoursSinceActive < 1) return 20;      // Active within 1 hour
  if (hoursSinceActive < 24) return 15;     // Active today
  if (hoursSinceActive < 72) return 10;     // Active this week
  if (hoursSinceActive < 168) return 5;     // Active this month

  return 0;
}

// Mutual interest: users with similar swipe patterns
async function calculateMutualInterestScore(
  user: UserProfile,
  candidate: UserProfile
): Promise<number> {
  // Get users that both user and candidate liked
  const [userLikes, candidateLikes] = await Promise.all([
    db.swipe.findMany({
      where: { userId: user.id, action: 'like' },
      select: { targetUserId: true },
    }),
    db.swipe.findMany({
      where: { userId: candidate.id, action: 'like' },
      select: { targetUserId: true },
    }),
  ]);

  const userLikeSet = new Set(userLikes.map(s => s.targetUserId));
  const candidateLikeSet = new Set(candidateLikes.map(s => s.targetUserId));

  // Calculate Jaccard similarity
  const intersection = [...userLikeSet].filter(id => candidateLikeSet.has(id)).length;
  const union = new Set([...userLikeSet, ...candidateLikeSet]).size;

  if (union === 0) return 0;

  const similarity = intersection / union;

  return similarity * 25;
}

// Response rate: high response rate = more engaged user
async function calculateResponseRateScore(candidateId: string): Promise<number> {
  // This would query MongoDB for message statistics
  // Simplified implementation:

  const stats = await getMessageStats(candidateId);

  if (stats.messagesReceived === 0) return 10; // Benefit of the doubt for new users

  const responseRate = stats.messagesReplied / stats.messagesReceived;

  if (responseRate > 0.8) return 15;
  if (responseRate > 0.6) return 12;
  if (responseRate > 0.4) return 8;
  if (responseRate > 0.2) return 4;

  return 0;
}

// Freshness boost: give new users more visibility
function calculateFreshnessBoost(candidate: UserProfile): number {
  const daysSinceJoined = (Date.now() - candidate.createdAt.getTime()) / (1000 * 60 * 60 * 24);

  if (daysSinceJoined < 3) return 10;   // New user
  if (daysSinceJoined < 7) return 7;
  if (daysSinceJoined < 14) return 5;
  if (daysSinceJoined < 30) return 3;

  return 0;
}

// Message stats helper
async function getMessageStats(userId: string): Promise<{
  messagesReceived: number;
  messagesReplied: number;
}> {
  // Query MongoDB for stats (cached in Redis)
  const cached = await redis.get(`stats:messages:${userId}`);
  if (cached) return JSON.parse(cached);

  // Compute stats from MongoDB
  // This is a simplified version - real implementation would be more complex
  const stats = {
    messagesReceived: 0,
    messagesReplied: 0,
  };

  // Cache for 1 hour
  await redis.setex(`stats:messages:${userId}`, 3600, JSON.stringify(stats));

  return stats;
}
```

---

### Distance Score Adjustment

```typescript
export function adjustScoreForDistance(score: number, distanceKm: number): number {
  // Penalty increases with distance
  let penalty = 0;

  if (distanceKm > 50) penalty = 10;
  else if (distanceKm > 30) penalty = 7;
  else if (distanceKm > 20) penalty = 5;
  else if (distanceKm > 10) penalty = 3;
  else if (distanceKm > 5) penalty = 1;

  return Math.max(score - penalty, 0);
}
```

---

## Implementation

### Complete Match Service

```typescript
// src/services/match/controllers/matchController.ts
import { Request, Response } from 'express';
import { db } from '../../../db/client';
import { redis } from '../../../cache/client';
import { filterByLocation } from '../utils/geoFilter';
import { filterByPreferences } from '../utils/preferenceFilter';
import { filterExclusions } from '../utils/exclusionFilter';
import {
  calculateCompatibilityScore,
  adjustScoreForDistance,
} from '../utils/scoring';

export async function getMatchCandidates(req: Request, res: Response) {
  const userId = req.user.id;
  const limit = parseInt(req.query.limit as string) || 50;

  try {
    // Check cache
    const cacheKey = `match:candidates:${userId}`;
    const cached = await redis.get(cacheKey);
    if (cached) {
      return res.json(JSON.parse(cached));
    }

    // Get user with preferences
    const user = await db.user.findUnique({
      where: { id: userId },
      include: {
        preferences: true,
        photos: true,
      },
    });

    if (!user || !user.preferences) {
      return res.status(400).json({ error: 'Please complete your profile' });
    }

    console.time('geo-filter');
    // Stage 1: Geographic filtering
    const nearbyUsers = await filterByLocation(
      userId,
      user.preferences.maxDistance,
      1000
    );
    console.timeEnd('geo-filter');

    if (nearbyUsers.length === 0) {
      return res.json({ candidates: [], hasMore: false });
    }

    console.time('preference-filter');
    // Stage 2: Preference filtering
    const candidateIds = nearbyUsers.map(u => u.userId);
    const preferenceFiltered = await filterByPreferences({
      userId,
      candidateIds,
      preferences: user.preferences,
    });
    console.timeEnd('preference-filter');

    console.time('exclusion-filter');
    // Stage 3: Exclusion filtering
    const finalCandidateIds = await filterExclusions(userId, preferenceFiltered);
    console.timeEnd('exclusion-filter');

    if (finalCandidateIds.length === 0) {
      return res.json({ candidates: [], hasMore: false });
    }

    console.time('fetch-candidates');
    // Fetch candidate profiles
    const candidates = await db.user.findMany({
      where: { id: { in: finalCandidateIds } },
      include: {
        photos: {
          orderBy: { order: 'asc' },
          take: 5,
        },
        preferences: true,
      },
    });
    console.timeEnd('fetch-candidates');

    console.time('scoring');
    // Stage 4: Scoring
    const scoredCandidates = await Promise.all(
      candidates.map(async (candidate) => {
        const nearbyUser = nearbyUsers.find(u => u.userId === candidate.id);
        const distance = nearbyUser?.distance || 0;

        let score = await calculateCompatibilityScore(user, candidate);
        score = adjustScoreForDistance(score, distance);

        return {
          id: candidate.id,
          firstName: candidate.firstName,
          age: calculateAge(candidate.dateOfBirth),
          bio: candidate.bio,
          photos: candidate.photos.map(p => ({
            url: p.url,
            order: p.order,
          })),
          distance: Math.round(distance * 10) / 10,
          score,
        };
      })
    );
    console.timeEnd('scoring');

    // Stage 5: Ranking with diversity
    const rankedCandidates = rankWithDiversity(scoredCandidates, user);

    // Take top N
    const results = rankedCandidates.slice(0, limit);

    const response = {
      candidates: results,
      hasMore: rankedCandidates.length > limit,
    };

    // Cache for 5 minutes
    await redis.setex(cacheKey, 300, JSON.stringify(response));

    res.json(response);
  } catch (error) {
    console.error('Error getting match candidates:', error);
    res.status(500).json({ error: 'Failed to get matches' });
  }
}

// Calculate age from date of birth
function calculateAge(dateOfBirth: Date): number {
  const today = new Date();
  const birthDate = new Date(dateOfBirth);
  let age = today.getFullYear() - birthDate.getFullYear();
  const monthDiff = today.getMonth() - birthDate.getMonth();

  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
    age--;
  }

  return age;
}
```

---

### Ranking with Diversity

Prevent showing only high-scoring users (diversity in results).

```typescript
// src/services/match/utils/ranking.ts
interface ScoredCandidate {
  id: string;
  firstName: string;
  age: number;
  bio: string | null;
  photos: Array<{ url: string; order: number }>;
  distance: number;
  score: number;
}

export function rankWithDiversity(
  candidates: ScoredCandidate[],
  user: any
): ScoredCandidate[] {
  // Sort by score
  candidates.sort((a, b) => b.score - a.score);

  // Add slight randomness to avoid showing exact same order
  // Take top 20% and shuffle them slightly
  const topTier = Math.ceil(candidates.length * 0.2);

  if (candidates.length > topTier) {
    const top = candidates.slice(0, topTier);
    const rest = candidates.slice(topTier);

    // Add small random factor to top tier
    top.forEach(candidate => {
      candidate.score += Math.random() * 5 - 2.5; // ±2.5 points
    });

    top.sort((a, b) => b.score - a.score);

    return [...top, ...rest];
  }

  return candidates;
}
```

---

## Caching Strategy

### Multi-Level Caching

```typescript
// src/services/match/utils/cache.ts
import { redis } from '../../../cache/client';

export async function getCachedCandidates(userId: string): Promise<any | null> {
  const cacheKey = `match:candidates:${userId}`;
  const cached = await redis.get(cacheKey);

  if (cached) {
    return JSON.parse(cached);
  }

  return null;
}

export async function cacheCandidates(userId: string, candidates: any, ttl: number = 300) {
  const cacheKey = `match:candidates:${userId}`;
  await redis.setex(cacheKey, ttl, JSON.stringify(candidates));
}

export async function invalidateUserCache(userId: string) {
  // Invalidate user's own cache
  await redis.del(`match:candidates:${userId}`);

  // Also invalidate profile cache
  await redis.del(`user:profile:${userId}`);
}
```

---

## Machine Learning Integration

### ML-Enhanced Scoring (Advanced)

For production systems, integrate ML models to improve matching:

```typescript
// src/services/match/ml/predictor.ts
import axios from 'axios';

interface MLFeatures {
  userAge: number;
  candidateAge: number;
  distance: number;
  profileCompleteness: number;
  activityLevel: number;
  mutualInterestScore: number;
  responseRate: number;
  daysSinceJoined: number;
}

export async function predictCompatibility(features: MLFeatures): Promise<number> {
  try {
    // Call ML service (Python FastAPI)
    const response = await axios.post(
      `${process.env.ML_SERVICE_URL}/predict`,
      { features },
      { timeout: 100 } // 100ms timeout
    );

    return response.data.score;
  } catch (error) {
    console.error('ML prediction failed, falling back to rule-based:', error);
    // Fallback to rule-based scoring
    return 0;
  }
}
```

### ML Service (Python)

```python
# ml-service/app.py
from fastapi import FastAPI
from pydantic import BaseModel
import numpy as np
import joblib

app = FastAPI()

# Load trained model
model = joblib.load('models/compatibility_model.pkl')

class Features(BaseModel):
    userAge: int
    candidateAge: int
    distance: float
    profileCompleteness: float
    activityLevel: float
    mutualInterestScore: float
    responseRate: float
    daysSinceJoined: int

@app.post("/predict")
async def predict(features: Features):
    # Convert to numpy array
    X = np.array([[
        features.userAge,
        features.candidateAge,
        features.distance,
        features.profileCompleteness,
        features.activityLevel,
        features.mutualInterestScore,
        features.responseRate,
        features.daysSinceJoined,
    ]])

    # Predict
    score = model.predict(X)[0]

    return {"score": float(score)}
```

---

## Performance Optimization

### Batch Processing

```typescript
// Process scoring in batches to avoid overwhelming the database
async function scoreCandidatesInBatches(
  user: UserProfile,
  candidates: UserProfile[],
  batchSize: number = 50
): Promise<Array<{ id: string; score: number }>> {
  const results: Array<{ id: string; score: number }> = [];

  for (let i = 0; i < candidates.length; i += batchSize) {
    const batch = candidates.slice(i, i + batchSize);

    const scores = await Promise.all(
      batch.map(async (candidate) => ({
        id: candidate.id,
        score: await calculateCompatibilityScore(user, candidate),
      }))
    );

    results.push(...scores);
  }

  return results;
}
```

---

### Precomputation

Precompute candidate pools during off-peak hours:

```typescript
// src/services/match/jobs/precomputeCandidates.ts
import { db } from '../../../db/client';
import { redis } from '../../../cache/client';

export async function precomputeCandidatesForAllUsers() {
  // Get all active users
  const users = await db.user.findMany({
    where: { active: true },
    select: { id: true },
  });

  console.log(`Precomputing candidates for ${users.length} users`);

  for (const user of users) {
    try {
      // Compute candidates (same logic as real-time)
      const candidates = await computeCandidatesForUser(user.id);

      // Cache for 1 hour
      await redis.setex(
        `match:candidates:${user.id}`,
        3600,
        JSON.stringify(candidates)
      );
    } catch (error) {
      console.error(`Failed to precompute for user ${user.id}:`, error);
    }
  }

  console.log('Precomputation complete');
}

// Run this job periodically (e.g., every hour)
```

---

## Testing

### Unit Tests

```typescript
// src/services/match/utils/__tests__/scoring.test.ts
import { calculateCompatibilityScore } from '../scoring';

describe('calculateCompatibilityScore', () => {
  it('should give high score for complete profile', async () => {
    const user = createMockUser({ photoCount: 5, bio: 'Long bio text here' });
    const candidate = createMockUser({ photoCount: 5, bio: 'Long bio text here' });

    const score = await calculateCompatibilityScore(user, candidate);

    expect(score).toBeGreaterThan(50);
  });

  it('should penalize incomplete profiles', async () => {
    const user = createMockUser({ photoCount: 5, bio: 'Long bio' });
    const candidate = createMockUser({ photoCount: 1, bio: null });

    const score = await calculateCompatibilityScore(user, candidate);

    expect(score).toBeLessThan(40);
  });

  it('should boost recently active users', async () => {
    const user = createMockUser();
    const activeCandidate = createMockUser({ lastSeen: new Date() });
    const inactiveCandidate = createMockUser({
      lastSeen: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000),
    });

    const activeScore = await calculateCompatibilityScore(user, activeCandidate);
    const inactiveScore = await calculateCompatibilityScore(user, inactiveCandidate);

    expect(activeScore).toBeGreaterThan(inactiveScore);
  });
});
```

---

## Next Steps

- [Real-time Messaging Implementation](08-realtime-messaging.md)
- [Geolocation Service](09-geolocation-service.md)
- [Infrastructure & Deployment](10-infrastructure.md)
