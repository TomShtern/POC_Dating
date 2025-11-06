# Database Schemas

## Overview

Dating app uses a hybrid database architecture:

- **PostgreSQL**: User profiles, relationships, preferences (ACID compliance)
- **Redis**: Caching, sessions, geolocation, real-time data
- **MongoDB**: Messages, events, activity logs (high write throughput)

---

## Table of Contents

1. [PostgreSQL Schemas](#postgresql-schemas)
2. [MongoDB Collections](#mongodb-collections)
3. [Redis Data Structures](#redis-data-structures)
4. [Prisma Schema](#prisma-schema)

---

## PostgreSQL Schemas

### Users Table

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  email VARCHAR(255) UNIQUE NOT NULL,
  phone VARCHAR(20) UNIQUE,
  password_hash VARCHAR(255),

  -- OAuth fields
  google_id VARCHAR(255) UNIQUE,
  apple_id VARCHAR(255) UNIQUE,
  facebook_id VARCHAR(255) UNIQUE,

  -- Profile
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100),
  date_of_birth DATE NOT NULL,
  gender VARCHAR(20) NOT NULL,
  bio TEXT,

  -- Location (PostGIS)
  location GEOGRAPHY(POINT, 4326),
  location_updated_at TIMESTAMP,

  -- Status
  verified BOOLEAN DEFAULT FALSE,
  active BOOLEAN DEFAULT TRUE,
  last_seen TIMESTAMP,

  -- Metadata
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  deleted_at TIMESTAMP,

  CONSTRAINT age_check CHECK (date_of_birth < CURRENT_DATE - INTERVAL '18 years')
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_location ON users USING GIST(location) WHERE location IS NOT NULL;
CREATE INDEX idx_users_active ON users(active) WHERE active = TRUE;
CREATE INDEX idx_users_gender ON users(gender);
CREATE INDEX idx_users_date_of_birth ON users(date_of_birth);
CREATE INDEX idx_users_last_seen ON users(last_seen) WHERE last_seen IS NOT NULL;

-- Update timestamp trigger
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION update_updated_at();
```

---

### User Preferences Table

```sql
CREATE TABLE user_preferences (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,

  -- Age range
  min_age INTEGER NOT NULL DEFAULT 18,
  max_age INTEGER NOT NULL DEFAULT 100,

  -- Distance
  max_distance INTEGER NOT NULL DEFAULT 50, -- kilometers

  -- Gender preference
  interested_in VARCHAR(20) NOT NULL,
  show_me VARCHAR(50) DEFAULT 'everyone', -- 'everyone', 'verified_only', etc.

  -- Other preferences
  show_distance BOOLEAN DEFAULT TRUE,
  show_age BOOLEAN DEFAULT TRUE,

  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),

  CONSTRAINT min_age_check CHECK (min_age >= 18 AND min_age <= max_age),
  CONSTRAINT max_age_check CHECK (max_age >= min_age AND max_age <= 100),
  CONSTRAINT max_distance_check CHECK (max_distance > 0 AND max_distance <= 500)
);

CREATE TRIGGER user_preferences_updated_at
BEFORE UPDATE ON user_preferences
FOR EACH ROW
EXECUTE FUNCTION update_updated_at();
```

---

### Photos Table

```sql
CREATE TABLE photos (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  url VARCHAR(500) NOT NULL,
  thumbnail_url VARCHAR(500),

  -- Display order
  "order" INTEGER DEFAULT 0,
  is_primary BOOLEAN DEFAULT FALSE,

  -- Metadata
  width INTEGER,
  height INTEGER,
  file_size INTEGER,

  created_at TIMESTAMP DEFAULT NOW(),

  CONSTRAINT one_primary_per_user UNIQUE (user_id, is_primary)
    WHERE is_primary = TRUE
);

CREATE INDEX idx_photos_user_id ON photos(user_id);
CREATE INDEX idx_photos_order ON photos(user_id, "order");
```

---

### Swipes Table

```sql
CREATE TABLE swipes (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  target_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  action VARCHAR(20) NOT NULL, -- 'like', 'dislike', 'super_like'

  created_at TIMESTAMP DEFAULT NOW(),

  CONSTRAINT no_self_swipe CHECK (user_id != target_user_id),
  CONSTRAINT unique_swipe UNIQUE (user_id, target_user_id)
);

CREATE INDEX idx_swipes_user_id ON swipes(user_id);
CREATE INDEX idx_swipes_target_user_id ON swipes(target_user_id);
CREATE INDEX idx_swipes_action ON swipes(action) WHERE action = 'like';
CREATE INDEX idx_swipes_created_at ON swipes(created_at DESC);

-- Composite index for match detection
CREATE INDEX idx_swipes_match_lookup ON swipes(target_user_id, user_id, action)
WHERE action IN ('like', 'super_like');
```

---

### Matches Table

```sql
CREATE TABLE matches (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

  -- Always store IDs in sorted order for consistency
  user_a_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  user_b_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  matched_at TIMESTAMP DEFAULT NOW(),
  unmatched_at TIMESTAMP,

  -- Last interaction for sorting match list
  last_message_at TIMESTAMP,

  CONSTRAINT user_order CHECK (user_a_id < user_b_id),
  CONSTRAINT unique_match UNIQUE (user_a_id, user_b_id)
);

CREATE INDEX idx_matches_user_a ON matches(user_a_id) WHERE unmatched_at IS NULL;
CREATE INDEX idx_matches_user_b ON matches(user_b_id) WHERE unmatched_at IS NULL;
CREATE INDEX idx_matches_last_message ON matches(last_message_at DESC NULLS LAST);

-- Function to get matches for a user
CREATE OR REPLACE FUNCTION get_user_matches(p_user_id UUID)
RETURNS TABLE (
  match_id UUID,
  other_user_id UUID,
  matched_at TIMESTAMP,
  last_message_at TIMESTAMP
) AS $$
BEGIN
  RETURN QUERY
  SELECT
    m.id,
    CASE
      WHEN m.user_a_id = p_user_id THEN m.user_b_id
      ELSE m.user_a_id
    END,
    m.matched_at,
    m.last_message_at
  FROM matches m
  WHERE (m.user_a_id = p_user_id OR m.user_b_id = p_user_id)
    AND m.unmatched_at IS NULL
  ORDER BY m.last_message_at DESC NULLS LAST;
END;
$$ LANGUAGE plpgsql;
```

---

### Blocks Table

```sql
CREATE TABLE blocks (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  blocked_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  reason VARCHAR(100),

  created_at TIMESTAMP DEFAULT NOW(),

  CONSTRAINT no_self_block CHECK (user_id != blocked_user_id),
  CONSTRAINT unique_block UNIQUE (user_id, blocked_user_id)
);

CREATE INDEX idx_blocks_user_id ON blocks(user_id);
CREATE INDEX idx_blocks_blocked_user_id ON blocks(blocked_user_id);
```

---

### Reports Table

```sql
CREATE TABLE reports (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  reported_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  reason VARCHAR(50) NOT NULL, -- 'spam', 'harassment', 'inappropriate', 'fake'
  description TEXT,

  status VARCHAR(20) DEFAULT 'pending', -- 'pending', 'reviewed', 'actioned'

  created_at TIMESTAMP DEFAULT NOW(),
  reviewed_at TIMESTAMP,
  reviewed_by UUID REFERENCES users(id)
);

CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_reported_user ON reports(reported_user_id);
CREATE INDEX idx_reports_created_at ON reports(created_at DESC);
```

---

## MongoDB Collections

### Messages Collection

```javascript
// MongoDB schema for messages
{
  _id: ObjectId,
  matchId: String, // UUID reference to PostgreSQL matches table
  senderId: String, // UUID reference to PostgreSQL users table

  // Content
  content: String,
  type: String, // 'text', 'image', 'voice', 'video'
  mediaUrl: String,

  // Status
  sentAt: Date,
  deliveredAt: Date,
  readAt: Date,

  // Soft delete
  deletedBy: [String], // Array of user IDs who deleted this message

  // Metadata
  metadata: {
    edited: Boolean,
    editedAt: Date,
  }
}
```

**Indexes:**
```javascript
db.messages.createIndex({ matchId: 1, sentAt: -1 });
db.messages.createIndex({ senderId: 1, sentAt: -1 });
db.messages.createIndex({ matchId: 1, readAt: 1 });

// TTL index - auto-delete messages after 1 year
db.messages.createIndex({ sentAt: 1 }, { expireAfterSeconds: 31536000 });
```

**MongoDB Schema Definition (Mongoose):**
```typescript
import mongoose from 'mongoose';

const messageSchema = new mongoose.Schema({
  matchId: {
    type: String,
    required: true,
    index: true,
  },
  senderId: {
    type: String,
    required: true,
    index: true,
  },
  content: {
    type: String,
    required: true,
    maxlength: 5000,
  },
  type: {
    type: String,
    enum: ['text', 'image', 'voice', 'video'],
    default: 'text',
  },
  mediaUrl: String,
  sentAt: {
    type: Date,
    default: Date.now,
    index: true,
  },
  deliveredAt: Date,
  readAt: {
    type: Date,
    index: true,
  },
  deletedBy: [String],
  metadata: {
    edited: { type: Boolean, default: false },
    editedAt: Date,
  },
});

// Compound indexes
messageSchema.index({ matchId: 1, sentAt: -1 });
messageSchema.index({ matchId: 1, readAt: 1 });

export const Message = mongoose.model('Message', messageSchema);
```

---

### User Events Collection

```javascript
// Track user activity for analytics
{
  _id: ObjectId,
  userId: String,
  eventType: String, // 'profile_view', 'swipe', 'match', 'message_sent', etc.

  // Event data
  targetUserId: String,
  metadata: Object,

  // Location at time of event
  location: {
    type: { type: String, default: 'Point' },
    coordinates: [Number] // [lng, lat]
  },

  timestamp: Date,

  // Session tracking
  sessionId: String,
  deviceId: String,
}
```

**Indexes:**
```javascript
db.user_events.createIndex({ userId: 1, timestamp: -1 });
db.user_events.createIndex({ eventType: 1, timestamp: -1 });
db.user_events.createIndex({ sessionId: 1 });

// Geospatial index
db.user_events.createIndex({ location: '2dsphere' });

// TTL index - auto-delete after 90 days
db.user_events.createIndex({ timestamp: 1 }, { expireAfterSeconds: 7776000 });
```

---

## Redis Data Structures

### User Locations (Geospatial)

```bash
# Store user locations with geospatial commands
GEOADD users:locations <longitude> <latitude> <user_id>

# Example
GEOADD users:locations -74.0060 40.7128 "user-123"

# Find nearby users
GEORADIUS users:locations -74.0060 40.7128 50 km WITHDIST WITHCOORD

# Get user's location
GEOPOS users:locations "user-123"

# Remove user location
ZREM users:locations "user-123"
```

**Implementation:**
```typescript
import { redis } from './cache/client';

export async function updateUserLocation(userId: string, lat: number, lng: number) {
  await redis.geoadd('users:locations', lng, lat, userId);

  // Set expiry on the user's location (remove if inactive for 24 hours)
  await redis.expire(`users:locations:${userId}`, 86400);
}

export async function findNearbyUsers(lat: number, lng: number, radiusKm: number) {
  const results = await redis.georadius(
    'users:locations',
    lng,
    lat,
    radiusKm,
    'km',
    'WITHDIST',
    'ASC'
  );

  return results.map(([userId, distance]) => ({
    userId,
    distance: parseFloat(distance),
  }));
}
```

---

### Session Storage

```bash
# Store JWT refresh tokens
SET refresh:<token_id> '{"userId":"user-123","expiresAt":"2024-12-06T10:30:00Z"}' EX 604800

# Get refresh token
GET refresh:<token_id>

# Invalidate refresh token
DEL refresh:<token_id>

# Store session data
HSET session:user-123 socketId "socket-abc-123"
HSET session:user-123 lastActive "2024-11-06T10:30:00Z"
EXPIRE session:user-123 3600
```

---

### Match Candidate Cache

```bash
# Cache match candidates for a user
SET match:candidates:user-123 '[{"id":"user-456","score":85},{"id":"user-789","score":72}]' EX 300

# Get cached candidates
GET match:candidates:user-123

# Invalidate cache (when user swipes or updates preferences)
DEL match:candidates:user-123
```

---

### User Profile Cache

```bash
# Cache user profile
SETEX user:profile:user-123 3600 '{"id":"user-123","name":"John","age":28,...}'

# Get cached profile
GET user:profile:user-123

# Invalidate on update
DEL user:profile:user-123
```

---

### Online Users (Set)

```bash
# Add user to online set
SADD online:users user-123

# Remove when user disconnects
SREM online:users user-123

# Check if user is online
SISMEMBER online:users user-123

# Get count of online users
SCARD online:users

# Get all online users (use with caution at scale)
SMEMBERS online:users
```

---

### Unread Message Counts

```bash
# Increment unread count for a match
INCR unread:match-123:user-456

# Get unread count
GET unread:match-123:user-456

# Reset unread count when user reads messages
SET unread:match-123:user-456 0
```

---

### Typing Indicators

```bash
# User is typing
SETEX typing:match-123:user-456 5 "1"

# Check if typing
EXISTS typing:match-123:user-456
```

---

### Rate Limiting

```bash
# Increment request count
INCR ratelimit:api:user-123:1699268400

# Set expiry (15 minutes)
EXPIRE ratelimit:api:user-123:1699268400 900

# Check count
GET ratelimit:api:user-123:1699268400
```

---

## Prisma Schema

Complete Prisma schema for PostgreSQL:

```prisma
// schema.prisma
generator client {
  provider = "prisma-client-js"
}

datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

model User {
  id            String    @id @default(uuid())
  email         String    @unique
  phone         String?   @unique
  passwordHash  String?   @map("password_hash")

  // OAuth
  googleId      String?   @unique @map("google_id")
  appleId       String?   @unique @map("apple_id")
  facebookId    String?   @unique @map("facebook_id")

  // Profile
  firstName     String    @map("first_name")
  lastName      String?   @map("last_name")
  dateOfBirth   DateTime  @map("date_of_birth")
  gender        String
  bio           String?

  // Location (stored as JSON since Prisma doesn't support PostGIS directly)
  location      Json?
  locationUpdatedAt DateTime? @map("location_updated_at")

  // Status
  verified      Boolean   @default(false)
  active        Boolean   @default(true)
  lastSeen      DateTime? @map("last_seen")

  // Timestamps
  createdAt     DateTime  @default(now()) @map("created_at")
  updatedAt     DateTime  @updatedAt @map("updated_at")
  deletedAt     DateTime? @map("deleted_at")

  // Relations
  preferences   UserPreferences?
  photos        Photo[]
  sentSwipes    Swipe[]   @relation("SentSwipes")
  receivedSwipes Swipe[]  @relation("ReceivedSwipes")
  matchesAsUserA Match[]  @relation("MatchUserA")
  matchesAsUserB Match[]  @relation("MatchUserB")
  blocks        Block[]   @relation("BlocksGiven")
  blockedBy     Block[]   @relation("BlocksReceived")
  reports       Report[]  @relation("ReportsGiven")
  reportedBy    Report[]  @relation("ReportsReceived")

  @@index([email])
  @@index([active])
  @@index([gender])
  @@index([dateOfBirth])
  @@index([lastSeen])
  @@map("users")
}

model UserPreferences {
  userId        String    @id @map("user_id")
  user          User      @relation(fields: [userId], references: [id], onDelete: Cascade)

  minAge        Int       @default(18) @map("min_age")
  maxAge        Int       @default(100) @map("max_age")
  maxDistance   Int       @default(50) @map("max_distance")

  interestedIn  String    @map("interested_in")
  showMe        String    @default("everyone") @map("show_me")

  showDistance  Boolean   @default(true) @map("show_distance")
  showAge       Boolean   @default(true) @map("show_age")

  createdAt     DateTime  @default(now()) @map("created_at")
  updatedAt     DateTime  @updatedAt @map("updated_at")

  @@map("user_preferences")
}

model Photo {
  id            String    @id @default(uuid())
  userId        String    @map("user_id")
  user          User      @relation(fields: [userId], references: [id], onDelete: Cascade)

  url           String
  thumbnailUrl  String?   @map("thumbnail_url")

  order         Int       @default(0)
  isPrimary     Boolean   @default(false) @map("is_primary")

  width         Int?
  height        Int?
  fileSize      Int?      @map("file_size")

  createdAt     DateTime  @default(now()) @map("created_at")

  @@index([userId])
  @@index([userId, order])
  @@map("photos")
}

model Swipe {
  id            String    @id @default(uuid())
  userId        String    @map("user_id")
  targetUserId  String    @map("target_user_id")

  user          User      @relation("SentSwipes", fields: [userId], references: [id], onDelete: Cascade)
  targetUser    User      @relation("ReceivedSwipes", fields: [targetUserId], references: [id], onDelete: Cascade)

  action        String    // 'like', 'dislike', 'super_like'

  createdAt     DateTime  @default(now()) @map("created_at")

  @@unique([userId, targetUserId])
  @@index([userId])
  @@index([targetUserId])
  @@index([action])
  @@index([targetUserId, userId, action])
  @@map("swipes")
}

model Match {
  id            String    @id @default(uuid())

  userAId       String    @map("user_a_id")
  userBId       String    @map("user_b_id")

  userA         User      @relation("MatchUserA", fields: [userAId], references: [id], onDelete: Cascade)
  userB         User      @relation("MatchUserB", fields: [userBId], references: [id], onDelete: Cascade)

  matchedAt     DateTime  @default(now()) @map("matched_at")
  unmatchedAt   DateTime? @map("unmatched_at")

  lastMessageAt DateTime? @map("last_message_at")

  @@unique([userAId, userBId])
  @@index([userAId])
  @@index([userBId])
  @@index([lastMessageAt])
  @@map("matches")
}

model Block {
  id            String    @id @default(uuid())
  userId        String    @map("user_id")
  blockedUserId String    @map("blocked_user_id")

  user          User      @relation("BlocksGiven", fields: [userId], references: [id], onDelete: Cascade)
  blockedUser   User      @relation("BlocksReceived", fields: [blockedUserId], references: [id], onDelete: Cascade)

  reason        String?

  createdAt     DateTime  @default(now()) @map("created_at")

  @@unique([userId, blockedUserId])
  @@index([userId])
  @@index([blockedUserId])
  @@map("blocks")
}

model Report {
  id              String    @id @default(uuid())
  reporterId      String    @map("reporter_id")
  reportedUserId  String    @map("reported_user_id")

  reporter        User      @relation("ReportsGiven", fields: [reporterId], references: [id], onDelete: Cascade)
  reportedUser    User      @relation("ReportsReceived", fields: [reportedUserId], references: [id], onDelete: Cascade)

  reason          String
  description     String?

  status          String    @default("pending")

  createdAt       DateTime  @default(now()) @map("created_at")
  reviewedAt      DateTime? @map("reviewed_at")
  reviewedBy      String?   @map("reviewed_by")

  @@index([status])
  @@index([reportedUserId])
  @@index([createdAt])
  @@map("reports")
}
```

---

## Database Initialization

### PostgreSQL Setup

```typescript
// src/db/client.ts
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient({
  log: ['query', 'error', 'warn'],
});

export { prisma as db };
```

```bash
# Initialize database
npx prisma migrate dev --name init

# Generate Prisma Client
npx prisma generate

# Seed database (optional)
npx prisma db seed
```

---

### MongoDB Setup

```typescript
// src/db/mongodb.ts
import mongoose from 'mongoose';

export async function connectMongoDB() {
  await mongoose.connect(process.env.MONGODB_URI!, {
    maxPoolSize: 10,
    serverSelectionTimeoutMS: 5000,
    socketTimeoutMS: 45000,
  });

  console.log('Connected to MongoDB');
}
```

---

### Redis Setup

```typescript
// src/cache/client.ts
import Redis from 'ioredis';

export const redis = new Redis({
  host: process.env.REDIS_HOST || 'localhost',
  port: parseInt(process.env.REDIS_PORT || '6379'),
  password: process.env.REDIS_PASSWORD,
  maxRetriesPerRequest: 3,
  enableReadyCheck: true,
  enableOfflineQueue: false,
  lazyConnect: false,
});

redis.on('connect', () => {
  console.log('Connected to Redis');
});

redis.on('error', (err) => {
  console.error('Redis error:', err);
});
```

---

## Backup & Recovery

### PostgreSQL Backups

```bash
# Daily automated backup
pg_dump -h localhost -U postgres -d dating_app \
  --format=custom \
  --file=/backups/dating_app_$(date +%Y%m%d).dump

# Restore from backup
pg_restore -h localhost -U postgres -d dating_app \
  /backups/dating_app_20241106.dump
```

### MongoDB Backups

```bash
# Backup
mongodump --uri="mongodb://localhost:27017/dating_app" \
  --out=/backups/mongodb_$(date +%Y%m%d)

# Restore
mongorestore --uri="mongodb://localhost:27017" \
  /backups/mongodb_20241106/dating_app
```

### Redis Persistence

```conf
# redis.conf
save 900 1        # Save after 900 seconds if 1 key changed
save 300 10       # Save after 300 seconds if 10 keys changed
save 60 10000     # Save after 60 seconds if 10000 keys changed

appendonly yes    # Enable AOF for better durability
appendfsync everysec
```

---

## Performance Optimization

### PostgreSQL Query Optimization

```sql
-- Analyze query performance
EXPLAIN ANALYZE
SELECT * FROM users WHERE location IS NOT NULL;

-- Vacuum regularly
VACUUM ANALYZE users;
VACUUM ANALYZE swipes;
VACUUM ANALYZE matches;

-- Create partial indexes for common queries
CREATE INDEX idx_active_users
ON users(last_seen)
WHERE active = TRUE AND deleted_at IS NULL;
```

### Connection Pooling

```typescript
// src/db/pool.ts
import { Pool } from 'pg';

export const pool = new Pool({
  host: process.env.DB_HOST,
  port: 5432,
  database: process.env.DB_NAME,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  max: 20, // Maximum number of connections
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
});
```

---

## Next Steps

- [Matching Algorithm Implementation](07-matching-algorithm.md)
- [Real-time Messaging](08-realtime-messaging.md)
- [Infrastructure & Deployment](09-infrastructure.md)
