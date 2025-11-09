# Performance Monitoring & Optimization

## Table of Contents
- [APM Tools Setup](#apm-tools-setup)
- [Query Optimization](#query-optimization)
- [N+1 Problem Solutions](#n1-problem-solutions)
- [Caching Strategies](#caching-strategies)
- [Performance Profiling](#performance-profiling)
- [Metrics & Logging](#metrics--logging)
- [Database Optimization](#database-optimization)
- [Load Testing](#load-testing)

---

## APM Tools Setup

### DataDog APM Integration

**Why DataDog:**
- Distributed tracing across microservices
- Real-time performance metrics
- Custom metrics and dashboards
- Log aggregation
- Alert management

**Installation:**

```bash
npm install --save dd-trace
```

**Configuration:**

```typescript
// packages/shared/src/config/datadog.ts
import tracer from 'dd-trace';

export function initializeDataDog() {
  if (process.env.NODE_ENV === 'production') {
    tracer.init({
      // Service name
      service: process.env.DD_SERVICE || 'dating-app-api',

      // Environment
      env: process.env.DD_ENV || 'production',

      // Version (from package.json or git commit)
      version: process.env.DD_VERSION || '1.0.0',

      // Logging
      logInjection: true, // Inject trace IDs into logs

      // Profiling
      profiling: true,
      runtimeMetrics: true,

      // Sampling (100% in production for critical app)
      sampleRate: 1.0,

      // APM tags
      tags: {
        team: 'backend',
        region: process.env.AWS_REGION || 'us-east-1',
      },
    });

    console.log('DataDog APM initialized');
  }
}

// packages/api-gateway/src/server.ts
import { initializeDataDog } from '@dating-app/shared/config/datadog';

// IMPORTANT: Initialize DataDog BEFORE importing other modules
initializeDataDog();

import express from 'express';
// ... rest of imports
```

**Custom Metrics:**

```typescript
// packages/shared/src/services/metrics.service.ts
import tracer from 'dd-trace';

const metrics = tracer.dogstatsd;

export class MetricsService {
  /**
   * Track swipe action
   */
  static trackSwipe(action: 'like' | 'dislike' | 'superlike') {
    metrics.increment('swipe.action', 1, [`action:${action}`]);
  }

  /**
   * Track match creation
   */
  static trackMatch(matchType: 'regular' | 'super') {
    metrics.increment('match.created', 1, [`type:${matchType}`]);
  }

  /**
   * Track message sent
   */
  static trackMessage(messageLength: number) {
    metrics.increment('message.sent', 1);
    metrics.histogram('message.length', messageLength);
  }

  /**
   * Track image upload
   */
  static trackImageUpload(fileSize: number, duration: number) {
    metrics.histogram('image.upload.size', fileSize);
    metrics.histogram('image.upload.duration', duration);
  }

  /**
   * Track cache hit/miss
   */
  static trackCacheHit(key: string) {
    metrics.increment('cache.hit', 1, [`key:${key}`]);
  }

  static trackCacheMiss(key: string) {
    metrics.increment('cache.miss', 1, [`key:${key}`]);
  }

  /**
   * Track database query duration
   */
  static trackQueryDuration(operation: string, duration: number) {
    metrics.histogram('db.query.duration', duration, [`operation:${operation}`]);
  }

  /**
   * Track active WebSocket connections
   */
  static setActiveConnections(count: number) {
    metrics.gauge('websocket.connections.active', count);
  }

  /**
   * Track API endpoint response time
   */
  static trackApiLatency(endpoint: string, method: string, statusCode: number, duration: number) {
    metrics.histogram('api.latency', duration, [
      `endpoint:${endpoint}`,
      `method:${method}`,
      `status:${statusCode}`,
    ]);
  }
}
```

**Custom Span Tracing:**

```typescript
// packages/matching/src/services/matching.service.ts
import tracer from 'dd-trace';
import { MetricsService } from '@dating-app/shared/services/metrics.service';

export class MatchingService {
  async findMatches(userId: string): Promise<Match[]> {
    // Create custom span for matching algorithm
    const span = tracer.startSpan('matching.find_matches', {
      resource: userId,
      tags: {
        'user.id': userId,
      },
    });

    try {
      const startTime = Date.now();

      // Step 1: Filter by location
      const locationSpan = tracer.startSpan('matching.filter_location', {
        childOf: span,
      });
      const nearbyUsers = await this.filterByLocation(userId);
      locationSpan.setTag('nearby_users.count', nearbyUsers.length);
      locationSpan.finish();

      // Step 2: Calculate compatibility
      const compatibilitySpan = tracer.startSpan('matching.calculate_compatibility', {
        childOf: span,
      });
      const scoredUsers = await this.calculateCompatibility(userId, nearbyUsers);
      compatibilitySpan.setTag('scored_users.count', scoredUsers.length);
      compatibilitySpan.finish();

      // Step 3: Sort and limit
      const sortedMatches = scoredUsers
        .sort((a, b) => b.score - a.score)
        .slice(0, 50);

      const duration = Date.now() - startTime;
      MetricsService.trackQueryDuration('find_matches', duration);

      span.setTag('matches.count', sortedMatches.length);
      span.setTag('duration_ms', duration);

      return sortedMatches;
    } catch (error) {
      span.setTag('error', true);
      span.setTag('error.message', (error as Error).message);
      throw error;
    } finally {
      span.finish();
    }
  }
}
```

### New Relic Alternative

```typescript
// packages/shared/src/config/newrelic.ts
import newrelic from 'newrelic';

export function trackCustomMetric(name: string, value: number) {
  newrelic.recordMetric(`Custom/${name}`, value);
}

export function trackCustomEvent(eventType: string, attributes: Record<string, any>) {
  newrelic.recordCustomEvent(eventType, attributes);
}

// Usage:
// trackCustomEvent('UserSwipe', { userId: '123', action: 'like', targetUserId: '456' });
```

---

## Query Optimization

### EXPLAIN ANALYZE for PostgreSQL

**Analyzing Query Performance:**

```typescript
// packages/shared/src/utils/query-analyzer.ts
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient({
  log: [
    {
      emit: 'event',
      level: 'query',
    },
  ],
});

// Log all queries with EXPLAIN ANALYZE in development
if (process.env.NODE_ENV === 'development') {
  prisma.$on('query', async (e) => {
    console.log('Query: ' + e.query);
    console.log('Duration: ' + e.duration + 'ms');

    // Run EXPLAIN ANALYZE for slow queries
    if (e.duration > 100) {
      const explained = await prisma.$queryRawUnsafe(
        `EXPLAIN ANALYZE ${e.query}`
      );
      console.log('EXPLAIN ANALYZE:', explained);
    }
  });
}

export { prisma };
```

**Manual Query Analysis:**

```sql
-- Analyze a slow query
EXPLAIN ANALYZE
SELECT u.id, u.first_name, u.location,
       COUNT(p.id) as photo_count
FROM users u
LEFT JOIN photos p ON p.user_id = u.id
WHERE ST_DWithin(
  u.location::geography,
  ST_MakePoint(-73.935242, 40.730610)::geography,
  10000  -- 10km radius
)
AND u.gender = 'female'
AND u.age BETWEEN 25 AND 35
GROUP BY u.id
ORDER BY u.last_active DESC
LIMIT 50;

-- Output will show:
-- 1. Seq Scan vs Index Scan
-- 2. Actual rows vs Estimated rows
-- 3. Execution time per node
-- 4. Total planning + execution time
```

**Optimizing the Query:**

```sql
-- Add indexes to improve performance
CREATE INDEX idx_users_location_gist ON users USING GIST(location);
CREATE INDEX idx_users_gender_age ON users(gender, age);
CREATE INDEX idx_users_last_active ON users(last_active DESC);
CREATE INDEX idx_photos_user_id ON photos(user_id);

-- Update statistics
ANALYZE users;
ANALYZE photos;

-- Re-run EXPLAIN ANALYZE to verify improvement
```

**Query Optimization Tips:**

```typescript
// packages/matching/src/repositories/user.repository.ts
import { prisma } from '@dating-app/shared/utils/query-analyzer';

export class UserRepository {
  /**
   * OPTIMIZED: Find nearby users with single query
   */
  async findNearbyUsers(latitude: number, longitude: number, radiusKm: number) {
    // Use raw query with PostGIS for optimal performance
    return prisma.$queryRaw`
      SELECT
        id,
        first_name,
        age,
        ST_Distance(
          location::geography,
          ST_MakePoint(${longitude}, ${latitude})::geography
        ) as distance_meters
      FROM users
      WHERE
        ST_DWithin(
          location::geography,
          ST_MakePoint(${longitude}, ${latitude})::geography,
          ${radiusKm * 1000}
        )
        AND id != ${userId}
        AND deleted_at IS NULL
      ORDER BY distance_meters ASC
      LIMIT 100
    `;
  }

  /**
   * BAD: Fetching all users then filtering in memory
   */
  async findNearbyUsersBad(userId: string, radiusKm: number) {
    const currentUser = await prisma.user.findUnique({ where: { id: userId } });

    // DON'T DO THIS - Loads all users into memory
    const allUsers = await prisma.user.findMany();

    // DON'T DO THIS - Filtering in application layer
    return allUsers.filter(user => {
      const distance = calculateDistance(currentUser.location, user.location);
      return distance <= radiusKm;
    });
  }
}
```

---

## N+1 Problem Solutions

### Understanding the N+1 Problem

**Example of N+1 Problem:**

```typescript
// BAD: N+1 queries (1 query for users + N queries for photos)
async function getUsersWithPhotos() {
  const users = await prisma.user.findMany(); // 1 query

  // N additional queries (one per user)
  for (const user of users) {
    user.photos = await prisma.photo.findMany({
      where: { userId: user.id },
    }); // N queries
  }

  return users;
}
// Total queries: 1 + N (if 100 users, that's 101 queries!)
```

### Solution 1: Prisma Include (JOIN)

```typescript
// GOOD: 1 query with JOIN
async function getUsersWithPhotos() {
  return prisma.user.findMany({
    include: {
      photos: true, // Single JOIN query
    },
  });
}
// Total queries: 1
```

### Solution 2: Prisma Relation Load Strategy

```typescript
// Use JOIN strategy for optimal performance
async function getUsersWithPhotosOptimized() {
  return prisma.user.findMany({
    include: {
      photos: true,
    },
    relationLoadStrategy: 'join', // Force JOIN instead of separate queries
  });
}
```

### Solution 3: DataLoader Pattern

**Why DataLoader:**
- Batches multiple requests into a single query
- Caches results within a single request
- Perfect for GraphQL resolvers
- Prevents N+1 queries automatically

**Implementation:**

```typescript
// packages/shared/src/dataloaders/photo.dataloader.ts
import DataLoader from 'dataloader';
import { prisma } from '../utils/query-analyzer';

/**
 * Batch load photos by user IDs
 */
export const createPhotoLoader = () => {
  return new DataLoader<string, Photo[]>(async (userIds: readonly string[]) => {
    // Single query to fetch all photos for all users
    const photos = await prisma.photo.findMany({
      where: {
        userId: {
          in: userIds as string[],
        },
      },
      orderBy: {
        order: 'asc',
      },
    });

    // Group photos by userId
    const photosByUserId = new Map<string, Photo[]>();
    for (const photo of photos) {
      if (!photosByUserId.has(photo.userId)) {
        photosByUserId.set(photo.userId, []);
      }
      photosByUserId.get(photo.userId)!.push(photo);
    }

    // Return photos in same order as userIds
    return userIds.map(userId => photosByUserId.get(userId) || []);
  });
};

/**
 * Batch load users by IDs
 */
export const createUserLoader = () => {
  return new DataLoader<string, User | null>(async (userIds: readonly string[]) => {
    const users = await prisma.user.findMany({
      where: {
        id: {
          in: userIds as string[],
        },
      },
    });

    const userMap = new Map(users.map(user => [user.id, user]));

    return userIds.map(id => userMap.get(id) || null);
  });
};

/**
 * Create all dataloaders for a request
 */
export function createDataLoaders() {
  return {
    photoLoader: createPhotoLoader(),
    userLoader: createUserLoader(),
  };
}
```

**Using DataLoader in Express:**

```typescript
// packages/api-gateway/src/middleware/dataloader.middleware.ts
import { Request, Response, NextFunction } from 'express';
import { createDataLoaders } from '@dating-app/shared/dataloaders';

declare global {
  namespace Express {
    interface Request {
      loaders: ReturnType<typeof createDataLoaders>;
    }
  }
}

export function attachDataLoaders(req: Request, res: Response, next: NextFunction) {
  req.loaders = createDataLoaders();
  next();
}

// packages/api-gateway/src/server.ts
import { attachDataLoaders } from './middleware/dataloader.middleware';
app.use(attachDataLoaders);
```

**Using DataLoader in Controllers:**

```typescript
// packages/users/src/controllers/user.controller.ts
import { Request, Response } from 'express';

export class UserController {
  static async getUsers(req: Request, res: Response) {
    const users = await prisma.user.findMany({ take: 50 });

    // Load photos for all users in a single batched query
    const usersWithPhotos = await Promise.all(
      users.map(async user => ({
        ...user,
        photos: await req.loaders.photoLoader.load(user.id), // Batched!
      }))
    );

    res.json(usersWithPhotos);
  }
}
```

### Solution 4: Prisma findUnique Batching

```typescript
// Prisma automatically batches findUnique queries
async function getUsersWithProfiles(userIds: string[]) {
  // These are automatically batched into a single query
  const users = await Promise.all(
    userIds.map(id =>
      prisma.user.findUnique({
        where: { id },
        include: { profile: true },
      })
    )
  );

  return users;
}
```

---

## Caching Strategies

### Multi-Level Caching

```typescript
// packages/shared/src/services/cache.service.ts
import { redisClient } from '../config/redis';
import NodeCache from 'node-cache';

// In-memory cache (L1)
const memoryCache = new NodeCache({
  stdTTL: 60, // 60 seconds default TTL
  checkperiod: 120, // Check for expired keys every 2 minutes
  maxKeys: 1000, // Limit memory usage
});

export class CacheService {
  /**
   * L1: Memory cache -> L2: Redis cache -> L3: Database
   */
  static async get<T>(
    key: string,
    fetchFn: () => Promise<T>,
    ttl: number = 300
  ): Promise<T> {
    // L1: Check memory cache
    const memoryValue = memoryCache.get<T>(key);
    if (memoryValue !== undefined) {
      return memoryValue;
    }

    // L2: Check Redis cache
    const redisValue = await redisClient.get(key);
    if (redisValue) {
      const parsed = JSON.parse(redisValue) as T;

      // Populate L1 cache
      memoryCache.set(key, parsed, ttl);

      return parsed;
    }

    // L3: Fetch from database
    const dbValue = await fetchFn();

    // Populate both caches
    await Promise.all([
      redisClient.setex(key, ttl, JSON.stringify(dbValue)),
      Promise.resolve(memoryCache.set(key, dbValue, ttl)),
    ]);

    return dbValue;
  }

  /**
   * Invalidate cache at all levels
   */
  static async invalidate(key: string): Promise<void> {
    await Promise.all([
      redisClient.del(key),
      Promise.resolve(memoryCache.del(key)),
    ]);
  }

  /**
   * Invalidate by pattern (Redis only)
   */
  static async invalidatePattern(pattern: string): Promise<void> {
    const keys = await redisClient.keys(pattern);
    if (keys.length > 0) {
      await redisClient.del(...keys);
      // Also clear memory cache
      keys.forEach(key => memoryCache.del(key));
    }
  }

  /**
   * Cache-aside pattern for user data
   */
  static async getUser(userId: string) {
    return this.get(
      `user:${userId}`,
      async () => {
        const user = await prisma.user.findUnique({
          where: { id: userId },
          include: { profile: true, photos: true },
        });
        return user;
      },
      300 // 5 minutes TTL
    );
  }

  /**
   * Write-through cache (update cache when writing to DB)
   */
  static async updateUser(userId: string, data: any) {
    const user = await prisma.user.update({
      where: { id: userId },
      data,
      include: { profile: true, photos: true },
    });

    // Update cache immediately
    const cacheKey = `user:${userId}`;
    await Promise.all([
      redisClient.setex(cacheKey, 300, JSON.stringify(user)),
      Promise.resolve(memoryCache.set(cacheKey, user, 300)),
    ]);

    return user;
  }

  /**
   * Cache with stale-while-revalidate pattern
   */
  static async getWithStaleRevalidate<T>(
    key: string,
    fetchFn: () => Promise<T>,
    ttl: number = 300,
    staleTtl: number = 600
  ): Promise<T> {
    const value = await redisClient.get(key);

    if (value) {
      const { data, timestamp } = JSON.parse(value);
      const age = Date.now() - timestamp;

      // If fresh, return immediately
      if (age < ttl * 1000) {
        return data;
      }

      // If stale but within stale window, return stale data
      // and trigger background refresh
      if (age < staleTtl * 1000) {
        // Background refresh (don't await)
        this.refreshInBackground(key, fetchFn, ttl);
        return data;
      }
    }

    // If no cache or expired, fetch fresh data
    return this.refresh(key, fetchFn, ttl);
  }

  private static async refresh<T>(
    key: string,
    fetchFn: () => Promise<T>,
    ttl: number
  ): Promise<T> {
    const data = await fetchFn();
    await redisClient.setex(
      key,
      ttl,
      JSON.stringify({ data, timestamp: Date.now() })
    );
    return data;
  }

  private static refreshInBackground<T>(
    key: string,
    fetchFn: () => Promise<T>,
    ttl: number
  ): void {
    this.refresh(key, fetchFn, ttl).catch(err =>
      console.error('Background refresh failed:', err)
    );
  }
}
```

**Caching Matching Algorithm Results:**

```typescript
// packages/matching/src/services/matching.service.ts
import { CacheService } from '@dating-app/shared/services/cache.service';

export class MatchingService {
  async getMatchCandidates(userId: string): Promise<User[]> {
    const cacheKey = `match_candidates:${userId}`;

    return CacheService.getWithStaleRevalidate(
      cacheKey,
      async () => {
        // Expensive matching algorithm
        return this.calculateMatches(userId);
      },
      300,  // Fresh for 5 minutes
      900   // Serve stale for up to 15 minutes
    );
  }

  async invalidateMatchCache(userId: string): Promise<void> {
    // Invalidate when user updates profile/preferences
    await CacheService.invalidate(`match_candidates:${userId}`);

    // Also invalidate for users who might match with this user
    // (Complex - might need a "potential matches" cache key pattern)
    await CacheService.invalidatePattern(`match_candidates:*`);
  }
}
```

---

## Performance Profiling

### Clinic.js for Node.js

**Installation:**

```bash
npm install -g clinic
```

**Usage:**

```bash
# Profile with Doctor (overall health check)
clinic doctor -- node packages/api-gateway/dist/server.js

# Run load test while profiling
# In another terminal:
# wrk -t12 -c400 -d30s http://localhost:3000/api/users

# Clinic will generate an HTML report when process stops (Ctrl+C)

# Profile CPU usage with Flame
clinic flame -- node packages/api-gateway/dist/server.js

# Profile async operations with Bubbleproof
clinic bubbleproof -- node packages/api-gateway/dist/server.js

# Profile memory with Heap Profiler
clinic heapprofiler -- node packages/api-gateway/dist/server.js
```

### Node.js Built-in Profiler

```typescript
// packages/shared/src/utils/profiler.ts
import { Session } from 'inspector';
import fs from 'fs';
import path from 'path';

export class Profiler {
  private session: Session | null = null;

  /**
   * Start CPU profiling
   */
  startCpuProfile() {
    this.session = new Session();
    this.session.connect();
    this.session.post('Profiler.enable');
    this.session.post('Profiler.start');
    console.log('CPU profiling started');
  }

  /**
   * Stop CPU profiling and save to file
   */
  async stopCpuProfile(filename: string = 'cpu-profile.cpuprofile') {
    if (!this.session) {
      throw new Error('Profiling not started');
    }

    return new Promise<void>((resolve, reject) => {
      this.session!.post('Profiler.stop', (err, { profile }) => {
        if (err) {
          reject(err);
          return;
        }

        const filepath = path.join(process.cwd(), 'profiles', filename);
        fs.mkdirSync(path.dirname(filepath), { recursive: true });
        fs.writeFileSync(filepath, JSON.stringify(profile));

        console.log(`CPU profile saved to ${filepath}`);
        console.log('Open in Chrome DevTools: chrome://inspect -> "Open dedicated DevTools for Node"');

        this.session!.disconnect();
        resolve();
      });
    });
  }

  /**
   * Start heap profiling
   */
  async takeHeapSnapshot(filename: string = 'heap-snapshot.heapsnapshot') {
    const session = new Session();
    session.connect();

    return new Promise<void>((resolve, reject) => {
      session.post('HeapProfiler.takeHeapSnapshot', null, (err, result) => {
        if (err) {
          reject(err);
          return;
        }

        const filepath = path.join(process.cwd(), 'profiles', filename);
        fs.mkdirSync(path.dirname(filepath), { recursive: true });

        const writeStream = fs.createWriteStream(filepath);

        session.on('HeapProfiler.addHeapSnapshotChunk', (m) => {
          writeStream.write(m.params.chunk);
        });

        session.post('HeapProfiler.stopSampling', () => {
          writeStream.end();
          session.disconnect();
          console.log(`Heap snapshot saved to ${filepath}`);
          resolve();
        });
      });
    });
  }
}

// Usage in API endpoint (admin only)
// router.post('/admin/profile/cpu/start', requireRole(Role.ADMIN), (req, res) => {
//   profiler.startCpuProfile();
//   res.json({ message: 'CPU profiling started' });
// });
// router.post('/admin/profile/cpu/stop', requireRole(Role.ADMIN), async (req, res) => {
//   await profiler.stopCpuProfile();
//   res.json({ message: 'CPU profile saved' });
// });
```

---

## Metrics & Logging

### Prometheus Metrics with Express

```typescript
// packages/api-gateway/src/middleware/prometheus.middleware.ts
import promClient from 'prom-client';
import { Request, Response, NextFunction } from 'express';

// Create a Registry
const register = new promClient.Registry();

// Add default metrics (CPU, memory, event loop, etc.)
promClient.collectDefaultMetrics({ register });

// Custom metrics
const httpRequestDuration = new promClient.Histogram({
  name: 'http_request_duration_seconds',
  help: 'Duration of HTTP requests in seconds',
  labelNames: ['method', 'route', 'status_code'],
  buckets: [0.01, 0.05, 0.1, 0.5, 1, 2, 5], // Buckets in seconds
  registers: [register],
});

const httpRequestTotal = new promClient.Counter({
  name: 'http_requests_total',
  help: 'Total number of HTTP requests',
  labelNames: ['method', 'route', 'status_code'],
  registers: [register],
});

const activeConnections = new promClient.Gauge({
  name: 'active_connections',
  help: 'Number of active connections',
  registers: [register],
});

const databaseQueryDuration = new promClient.Histogram({
  name: 'database_query_duration_seconds',
  help: 'Duration of database queries in seconds',
  labelNames: ['operation', 'table'],
  buckets: [0.001, 0.01, 0.05, 0.1, 0.5, 1],
  registers: [register],
});

/**
 * Middleware to track HTTP metrics
 */
export function metricsMiddleware() {
  return (req: Request, res: Response, next: NextFunction) => {
    const start = Date.now();

    // Track when response finishes
    res.on('finish', () => {
      const duration = (Date.now() - start) / 1000; // Convert to seconds

      const route = req.route?.path || req.path;
      const labels = {
        method: req.method,
        route,
        status_code: res.statusCode.toString(),
      };

      httpRequestDuration.observe(labels, duration);
      httpRequestTotal.inc(labels);
    });

    next();
  };
}

/**
 * Expose /metrics endpoint
 */
export async function metricsEndpoint(req: Request, res: Response) {
  res.set('Content-Type', register.contentType);
  res.end(await register.metrics());
}

export { register, activeConnections, databaseQueryDuration };
```

**Using Metrics:**

```typescript
// packages/api-gateway/src/server.ts
import express from 'express';
import { metricsMiddleware, metricsEndpoint, activeConnections } from './middleware/prometheus.middleware';

const app = express();

// Apply metrics middleware
app.use(metricsMiddleware());

// Expose metrics endpoint
app.get('/metrics', metricsEndpoint);

// Track active connections
const server = app.listen(3000, () => {
  console.log('Server started');
});

server.on('connection', (socket) => {
  activeConnections.inc();
  socket.on('close', () => {
    activeConnections.dec();
  });
});
```

**Instrumenting Database Queries:**

```typescript
// packages/shared/src/utils/prisma-metrics.ts
import { PrismaClient } from '@prisma/client';
import { databaseQueryDuration } from '../middleware/prometheus.middleware';

const prisma = new PrismaClient();

// Extend Prisma to track query duration
prisma.$use(async (params, next) => {
  const start = Date.now();
  const result = await next(params);
  const duration = (Date.now() - start) / 1000;

  databaseQueryDuration.observe(
    {
      operation: params.action,
      table: params.model || 'unknown',
    },
    duration
  );

  return result;
});

export { prisma };
```

---

## Database Optimization

### Index Strategy

```sql
-- Users table indexes
CREATE INDEX idx_users_location_gist ON users USING GIST(location); -- Geospatial queries
CREATE INDEX idx_users_age_gender ON users(age, gender); -- Filtering
CREATE INDEX idx_users_last_active ON users(last_active DESC); -- Sorting
CREATE INDEX idx_users_email_unique ON users(email) WHERE deleted_at IS NULL; -- Partial unique index

-- Matches table indexes
CREATE INDEX idx_matches_user_ids ON matches(user_id_1, user_id_2); -- Match lookups
CREATE INDEX idx_matches_created_at ON matches(created_at DESC); -- Recent matches

-- Messages table indexes
CREATE INDEX idx_messages_match_id_created ON messages(match_id, created_at DESC); -- Message history
CREATE INDEX idx_messages_sender_id ON messages(sender_id); -- User's sent messages
CREATE INDEX idx_messages_unread ON messages(recipient_id, read_at) WHERE read_at IS NULL; -- Unread messages

-- Swipes table indexes
CREATE INDEX idx_swipes_user_target ON swipes(user_id, target_user_id); -- Lookup swipes
CREATE INDEX idx_swipes_target_user ON swipes(target_user_id, action, created_at); -- Who liked a user

-- Composite index for complex queries
CREATE INDEX idx_users_search ON users(gender, age, location) WHERE deleted_at IS NULL;
```

### Monitoring Index Usage

```sql
-- Check which indexes are used
SELECT
  schemaname,
  tablename,
  indexname,
  idx_scan as scans,
  idx_tup_read as tuples_read,
  idx_tup_fetch as tuples_fetched,
  pg_size_pretty(pg_relation_size(indexrelid)) as size
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC;

-- Find unused indexes (candidates for removal)
SELECT
  schemaname,
  tablename,
  indexname,
  pg_size_pretty(pg_relation_size(indexrelid)) as size
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexrelname NOT LIKE 'pg_toast_%'
ORDER BY pg_relation_size(indexrelid) DESC;

-- Check table statistics (for ANALYZE)
SELECT
  schemaname,
  tablename,
  last_vacuum,
  last_autovacuum,
  last_analyze,
  last_autoanalyze,
  n_live_tup as live_rows,
  n_dead_tup as dead_rows
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC;
```

### Connection Pooling

```typescript
// packages/shared/src/config/database.ts
import { PrismaClient } from '@prisma/client';

const DATABASE_URL = process.env.DATABASE_URL!;

// Configure connection pool
const prisma = new PrismaClient({
  datasources: {
    db: {
      url: DATABASE_URL,
    },
  },
  // Connection pool configuration
  // connection_limit=20 in DATABASE_URL
  // or via PgBouncer for connection pooling
});

// Graceful shutdown
process.on('SIGINT', async () => {
  await prisma.$disconnect();
  process.exit(0);
});

export { prisma };
```

**PgBouncer Configuration (external connection pooler):**

```ini
; /etc/pgbouncer/pgbouncer.ini
[databases]
dating_app = host=localhost port=5432 dbname=dating_app

[pgbouncer]
listen_addr = 127.0.0.1
listen_port = 6432
auth_type = md5
auth_file = /etc/pgbouncer/userlist.txt

; Connection pool settings
pool_mode = transaction
max_client_conn = 1000
default_pool_size = 25
reserve_pool_size = 5
reserve_pool_timeout = 3

; Server connection settings
server_reset_query = DISCARD ALL
server_check_delay = 10
```

---

## Load Testing

### k6 Load Testing

**Installation:**

```bash
# macOS
brew install k6

# Linux
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

**Load Test Script:**

```javascript
// load-tests/api-load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '30s', target: 20 },   // Ramp up to 20 users
    { duration: '1m', target: 50 },    // Ramp up to 50 users
    { duration: '2m', target: 100 },   // Ramp up to 100 users
    { duration: '2m', target: 100 },   // Stay at 100 users
    { duration: '30s', target: 0 },    // Ramp down
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500'], // 95% of requests should be below 500ms
    'http_req_failed': ['rate<0.01'],   // Error rate should be below 1%
    'errors': ['rate<0.1'],              // Custom error rate below 10%
  },
};

const BASE_URL = 'http://localhost:3000';

export function setup() {
  // Login and get token
  const loginRes = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
    email: 'test@example.com',
    password: 'Test123!@#',
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  const token = loginRes.json('accessToken');
  return { token };
}

export default function(data) {
  const headers = {
    'Authorization': `Bearer ${data.token}`,
    'Content-Type': 'application/json',
  };

  // Test 1: Get match candidates
  const candidatesRes = http.get(`${BASE_URL}/api/matches/candidates`, { headers });
  check(candidatesRes, {
    'candidates status is 200': (r) => r.status === 200,
    'candidates response time < 500ms': (r) => r.timings.duration < 500,
  }) || errorRate.add(1);

  sleep(1);

  // Test 2: Swipe action
  const swipeRes = http.post(`${BASE_URL}/api/swipes`, JSON.stringify({
    targetUserId: '123e4567-e89b-12d3-a456-426614174000',
    action: 'like',
  }), { headers });
  check(swipeRes, {
    'swipe status is 200 or 201': (r) => r.status === 200 || r.status === 201,
    'swipe response time < 200ms': (r) => r.timings.duration < 200,
  }) || errorRate.add(1);

  sleep(2);

  // Test 3: Get messages
  const messagesRes = http.get(`${BASE_URL}/api/messages?matchId=uuid`, { headers });
  check(messagesRes, {
    'messages status is 200': (r) => r.status === 200,
  }) || errorRate.add(1);

  sleep(1);
}

export function teardown(data) {
  // Cleanup if needed
}
```

**Running Load Tests:**

```bash
# Run load test
k6 run load-tests/api-load-test.js

# Run with custom VUs and duration
k6 run --vus 100 --duration 5m load-tests/api-load-test.js

# Run with cloud output
k6 run --out cloud load-tests/api-load-test.js

# Run with InfluxDB output (for Grafana visualization)
k6 run --out influxdb=http://localhost:8086/k6 load-tests/api-load-test.js
```

---

## Performance Checklist

### Backend Optimization

- ✅ Enable query logging and analyze slow queries with EXPLAIN ANALYZE
- ✅ Add indexes for frequently queried columns
- ✅ Use connection pooling (PgBouncer or Prisma pool)
- ✅ Implement multi-level caching (memory + Redis)
- ✅ Solve N+1 queries with joins, includes, or DataLoader
- ✅ Use bulk operations instead of loops
- ✅ Implement pagination for large result sets
- ✅ Profile CPU and memory usage with Clinic.js
- ✅ Monitor with APM tools (DataDog or New Relic)
- ✅ Set up Prometheus metrics
- ✅ Run load tests regularly with k6
- ✅ Optimize JSON serialization (fast-json-stringify)
- ✅ Use worker threads for CPU-intensive tasks
- ✅ Implement response compression (gzip/brotli)
- ✅ Use CDN for static assets

### Database Optimization

- ✅ Regular VACUUM and ANALYZE
- ✅ Monitor index usage and remove unused indexes
- ✅ Use materialized views for complex aggregations
- ✅ Implement database replicas for read scaling
- ✅ Partition large tables (e.g., messages by date)
- ✅ Use prepared statements
- ✅ Set appropriate work_mem and shared_buffers
- ✅ Monitor connection count and pool usage

### Caching Optimization

- ✅ Cache matching algorithm results (5-15 min TTL)
- ✅ Cache user profiles (5 min TTL)
- ✅ Cache geolocation queries (10 min TTL)
- ✅ Implement cache warming for popular data
- ✅ Use stale-while-revalidate for better UX
- ✅ Monitor cache hit/miss ratios
- ✅ Set appropriate eviction policies

### Monitoring & Alerts

- ✅ Alert on response time > 1 second
- ✅ Alert on error rate > 1%
- ✅ Alert on CPU usage > 80%
- ✅ Alert on memory usage > 85%
- ✅ Alert on database connection pool exhaustion
- ✅ Alert on cache miss rate > 30%
- ✅ Track and alert on WebSocket connection failures
