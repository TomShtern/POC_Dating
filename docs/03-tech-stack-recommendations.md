# Tech Stack Recommendations for Dating App

## Table of Contents
1. [Mobile Development](#mobile-development)
2. [Backend Services](#backend-services)
3. [Databases](#databases)
4. [Infrastructure & Cloud](#infrastructure--cloud)
5. [DevOps & CI/CD](#devops--cicd)
6. [Monitoring & Analytics](#monitoring--analytics)
7. [Complete Stack Summary](#complete-stack-summary)

---

## Mobile Development

### Recommended Approach: **React Native**

#### Why React Native?

**Pros**:
✅ Single codebase for iOS and Android (40% faster development)
✅ Large ecosystem of libraries and components
✅ Hot reloading for rapid iteration
✅ 90% of native performance with proper optimization
✅ Strong community support and extensive documentation
✅ Cost-effective for startups (one team vs. two)
✅ Easy to hire JavaScript/TypeScript developers

**Cons**:
❌ Slightly less performant than native for compute-intensive tasks
❌ Large bundle size (can be optimized)
❌ Platform-specific bugs occasionally

**When to Choose Native Instead**:
- Budget allows for separate iOS/Android teams
- Need absolute maximum performance (rare for dating apps)
- Deep platform-specific features required
- Already have separate mobile teams

### React Native Stack

```javascript
// Core Framework
"react-native": "^0.73.0"
"react": "^18.2.0"

// Navigation
"@react-navigation/native": "^6.1.9"
"@react-navigation/stack": "^6.3.20"
"@react-navigation/bottom-tabs": "^6.5.11"

// State Management
"@reduxjs/toolkit": "^2.0.1"
"react-redux": "^9.0.4"
// Alternative: "zustand" for simpler state management

// API Communication
"axios": "^1.6.2"
"react-query": "^3.39.3"  // Now @tanstack/react-query

// Real-time Communication
"socket.io-client": "^4.6.1"

// UI Components
"react-native-paper": "^5.11.3"
"react-native-vector-icons": "^10.0.3"
"react-native-elements": "^3.4.3"

// Maps & Geolocation
"react-native-maps": "^1.10.0"
"@react-native-community/geolocation": "^3.1.0"

// Media Handling
"react-native-image-picker": "^7.1.0"
"react-native-fast-image": "^8.6.3"
"react-native-video": "^5.2.1"

// Authentication
"@react-native-firebase/auth": "^19.0.1"
"@react-native-google-signin/google-signin": "^11.0.0"
"react-native-fbsdk-next": "^12.1.2"

// Push Notifications
"@react-native-firebase/messaging": "^19.0.1"
"@notifee/react-native": "^7.8.0"

// Animation
"react-native-reanimated": "^3.6.1"
"react-native-gesture-handler": "^2.14.1"
"react-native-deck-swiper": "^2.0.17"  // For Tinder-style swipe

// Storage
"@react-native-async-storage/async-storage": "^1.21.0"

// Security
"react-native-keychain": "^8.1.2"  // Secure token storage

// Analytics
"@react-native-firebase/analytics": "^19.0.1"

// Development Tools
"@react-native-community/eslint-config": "^3.2.0"
"typescript": "^5.3.3"
"@types/react": "^18.2.45"
"@types/react-native": "^0.72.8"
```

### Alternative: Flutter

**Consider Flutter if**:
- Team has Dart experience
- Want slightly better performance than React Native
- Need complex custom UI animations
- Google ecosystem integration is priority

**Flutter Stack**:
```yaml
dependencies:
  flutter:
    sdk: flutter

  # State Management
  riverpod: ^2.4.9

  # HTTP & API
  dio: ^5.4.0

  # Real-time
  socket_io_client: ^2.0.3

  # Navigation
  go_router: ^12.1.3

  # UI
  flutter_svg: ^2.0.9
  cached_network_image: ^3.3.0

  # Location
  geolocator: ^10.1.0
  google_maps_flutter: ^2.5.0

  # Firebase
  firebase_core: ^2.24.2
  firebase_auth: ^4.15.3
  firebase_messaging: ^14.7.9

  # Image Picker
  image_picker: ^1.0.5

  # Storage
  shared_preferences: ^2.2.2
  flutter_secure_storage: ^9.0.0
```

### Native Development (If Budget Allows)

#### iOS Stack
```swift
// Language: Swift 5.9+
// UI Framework: SwiftUI (modern) or UIKit (mature)
// Architecture: MVVM or Clean Architecture

// Key Libraries
- Alamofire: Networking
- SocketIO: Real-time communication
- Kingfisher: Image caching
- SnapKit: Auto Layout DSL
- Firebase: Auth, Analytics, Messaging
- Combine: Reactive programming
- CoreLocation: Geolocation
```

#### Android Stack
```kotlin
// Language: Kotlin 1.9+
// UI Framework: Jetpack Compose (modern) or XML (mature)
// Architecture: MVVM with ViewModel

// Key Libraries
- Retrofit: Networking
- Socket.IO: Real-time communication
- Coil: Image loading
- Hilt: Dependency injection
- Room: Local database
- Firebase: Auth, Analytics, FCM
- Coroutines & Flow: Async programming
- Fused Location Provider: Geolocation
```

---

## Backend Services

### Recommended Primary Stack: **Node.js with TypeScript**

#### Why Node.js?

**Pros**:
✅ Excellent for I/O-bound operations (typical for dating apps)
✅ Non-blocking, event-driven (great for real-time features)
✅ JavaScript/TypeScript consistency with React Native frontend
✅ Massive ecosystem (npm)
✅ Strong WebSocket support
✅ Quick prototyping and iteration
✅ Easy to find developers

**Cons**:
❌ Not ideal for CPU-intensive tasks (ML model training)
❌ Single-threaded (solved with clustering)

### Node.js Backend Stack

```json
{
  "dependencies": {
    // Core Framework
    "express": "^4.18.2",
    // Alternative: "fastify": "^4.25.2" (faster)

    // TypeScript
    "typescript": "^5.3.3",
    "@types/node": "^20.10.5",
    "@types/express": "^4.17.21",

    // API Documentation
    "swagger-ui-express": "^5.0.0",
    "@types/swagger-ui-express": "^4.1.6",

    // Validation
    "zod": "^3.22.4",
    "joi": "^17.11.0",

    // Authentication
    "jsonwebtoken": "^9.0.2",
    "bcryptjs": "^2.4.3",
    "passport": "^0.7.0",
    "passport-jwt": "^4.0.1",
    "passport-google-oauth20": "^2.0.0",

    // Database ORMs
    "prisma": "^5.7.1",  // PostgreSQL ORM (recommended)
    "@prisma/client": "^5.7.1",
    "mongoose": "^8.0.3",  // MongoDB ODM

    // Redis Client
    "ioredis": "^5.3.2",

    // Real-time
    "socket.io": "^4.6.1",

    // Message Queue
    "kafkajs": "^2.2.4",

    // HTTP Client
    "axios": "^1.6.2",

    // Environment Variables
    "dotenv": "^16.3.1",

    // Logging
    "winston": "^3.11.0",
    "morgan": "^1.10.0",

    // Security
    "helmet": "^7.1.0",
    "cors": "^2.8.5",
    "express-rate-limit": "^7.1.5",

    // File Upload
    "multer": "^1.4.5-lts.1",
    "aws-sdk": "^2.1514.0",

    // Scheduling
    "node-cron": "^3.0.3",

    // Testing
    "jest": "^29.7.0",
    "supertest": "^6.3.3",
    "@types/jest": "^29.5.11",

    // Code Quality
    "eslint": "^8.56.0",
    "prettier": "^3.1.1",
    "@typescript-eslint/eslint-plugin": "^6.15.0"
  }
}
```

### Example Express Setup

```typescript
// src/app.ts
import express from 'express';
import helmet from 'helmet';
import cors from 'cors';
import rateLimit from 'express-rate-limit';
import { errorHandler } from './middleware/errorHandler';
import userRoutes from './routes/userRoutes';
import matchRoutes from './routes/matchRoutes';

const app = express();

// Security middleware
app.use(helmet());
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(','),
  credentials: true
}));

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});
app.use('/api/', limiter);

// Body parsing
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// Routes
app.use('/api/v1/users', userRoutes);
app.use('/api/v1/matches', matchRoutes);

// Error handling
app.use(errorHandler);

export default app;
```

### Alternative Backend: **Python with FastAPI**

**Use Python if**:
- Heavy ML/AI integration needed
- Team has Python expertise
- Data science workflows important

```python
# requirements.txt
fastapi==0.109.0
uvicorn[standard]==0.27.0
pydantic==2.5.3
sqlalchemy==2.0.25
alembic==1.13.1
asyncpg==0.29.0  # Async PostgreSQL
redis==5.0.1
python-jose[cryptography]==3.3.0  # JWT
passlib[bcrypt]==1.7.4
python-multipart==0.0.6
aiokafka==0.10.0
python-socketio==5.11.0
boto3==1.34.24  # AWS SDK
pillow==10.2.0  # Image processing
scikit-learn==1.4.0
numpy==1.26.3
pandas==2.1.4
pytest==7.4.4
```

### Alternative Backend: **Go**

**Use Go if**:
- Maximum performance required
- Low latency critical
- Team has Go expertise

```go
// Key libraries
github.com/gin-gonic/gin          // Web framework
github.com/lib/pq                 // PostgreSQL driver
github.com/go-redis/redis/v8      // Redis client
github.com/golang-jwt/jwt/v5      // JWT
github.com/segmentio/kafka-go     // Kafka client
gorm.io/gorm                      // ORM
```

---

## Databases

### Primary Database: **PostgreSQL**

#### Why PostgreSQL?

**Pros**:
✅ ACID compliance (data integrity)
✅ Excellent for relational data (user profiles, matches)
✅ PostGIS extension for geospatial queries
✅ JSON support (hybrid relational/document)
✅ Strong consistency guarantees
✅ Mature ecosystem and tooling
✅ Great performance with proper indexing

**Recommended Version**: PostgreSQL 15 or 16

**Configuration**:
```sql
-- Enable PostGIS for geolocation
CREATE EXTENSION postgis;

-- Enable pg_trgm for fuzzy text search
CREATE EXTENSION pg_trgm;

-- Example indexes
CREATE INDEX idx_users_location ON users USING GIST(location);
CREATE INDEX idx_swipes_user_id ON swipes(user_id);
CREATE INDEX idx_matches_users ON matches(user_a_id, user_b_id);
```

**Connection Pooling**:
```typescript
import { Pool } from 'pg';

const pool = new Pool({
  host: process.env.DB_HOST,
  port: 5432,
  database: process.env.DB_NAME,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  max: 20,  // Max connections
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
});
```

### ORM: **Prisma**

**Why Prisma?**
✅ Type-safe database access
✅ Auto-generated TypeScript types
✅ Excellent migration system
✅ Great developer experience

```typescript
// schema.prisma
datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

generator client {
  provider = "prisma-client-js"
}

model User {
  id            String    @id @default(uuid())
  email         String    @unique
  firstName     String
  dateOfBirth   DateTime
  bio           String?
  location      Json?     // { lat: number, lng: number }
  createdAt     DateTime  @default(now())
  updatedAt     DateTime  @updatedAt

  photos        Photo[]
  sentSwipes    Swipe[]   @relation("SentSwipes")
  receivedSwipes Swipe[]  @relation("ReceivedSwipes")
  matchesA      Match[]   @relation("MatchUserA")
  matchesB      Match[]   @relation("MatchUserB")

  @@index([email])
}

model Swipe {
  id           String    @id @default(uuid())
  userId       String
  targetUserId String
  swipeType    SwipeType
  createdAt    DateTime  @default(now())

  user         User      @relation("SentSwipes", fields: [userId], references: [id])
  targetUser   User      @relation("ReceivedSwipes", fields: [targetUserId], references: [id])

  @@unique([userId, targetUserId])
  @@index([userId])
  @@index([targetUserId])
}

enum SwipeType {
  LIKE
  DISLIKE
  SUPER_LIKE
}

model Match {
  id          String    @id @default(uuid())
  userAId     String
  userBId     String
  matchedAt   DateTime  @default(now())
  unmatchedAt DateTime?

  userA       User      @relation("MatchUserA", fields: [userAId], references: [id])
  userB       User      @relation("MatchUserB", fields: [userBId], references: [id])

  @@unique([userAId, userBId])
  @@index([userAId])
  @@index([userBId])
}
```

### Cache: **Redis**

#### Why Redis?

**Pros**:
✅ Extremely fast (in-memory)
✅ Built-in geospatial commands (GEOADD, GEORADIUS)
✅ Pub/Sub for real-time features
✅ Support for various data structures (strings, hashes, sets, sorted sets)
✅ Session storage
✅ Rate limiting

**Use Cases**:
- Session tokens
- User location cache
- Match candidate cache
- Online user presence
- Rate limiting
- Message queue (simple cases)

**Configuration**:
```typescript
import Redis from 'ioredis';

const redis = new Redis({
  host: process.env.REDIS_HOST,
  port: 6379,
  password: process.env.REDIS_PASSWORD,
  maxRetriesPerRequest: 3,
  enableReadyCheck: true,
  db: 0,
});

// Cluster mode for high availability
const redisCluster = new Redis.Cluster([
  { host: 'node1', port: 6379 },
  { host: 'node2', port: 6379 },
  { host: 'node3', port: 6379 },
]);

// Example usage
await redis.setex('user:session:abc123', 3600, JSON.stringify(userData));
await redis.geoadd('users:locations', longitude, latitude, userId);
const nearby = await redis.georadius('users:locations', lng, lat, 50, 'km');
```

### Message Storage: **MongoDB**

#### Why MongoDB for Messages?

**Pros**:
✅ High write throughput
✅ Flexible schema (message types can vary)
✅ Horizontal scaling (sharding)
✅ Time-series data (messages)

**Use Cases**:
- Chat messages
- User events/activity log
- Notifications

**Configuration**:
```typescript
import mongoose from 'mongoose';

mongoose.connect(process.env.MONGODB_URI, {
  maxPoolSize: 10,
  serverSelectionTimeoutMS: 5000,
});

const messageSchema = new mongoose.Schema({
  matchId: { type: String, required: true, index: true },
  senderId: { type: String, required: true },
  content: { type: String, required: true },
  type: { type: String, enum: ['text', 'image', 'voice', 'video'] },
  mediaUrl: String,
  sentAt: { type: Date, default: Date.now },
  deliveredAt: Date,
  readAt: Date,
  deletedBy: [String]
});

messageSchema.index({ matchId: 1, sentAt: -1 });

const Message = mongoose.model('Message', messageSchema);
```

### Search Engine: **Elasticsearch**

**Use for**:
- User search by name, interests
- Full-text search
- Complex filtering

**Alternative**: PostgreSQL full-text search (simpler, good for small-medium scale)

```typescript
import { Client } from '@elastic/elasticsearch';

const esClient = new Client({
  node: process.env.ELASTICSEARCH_URL,
  auth: {
    username: process.env.ES_USER,
    password: process.env.ES_PASSWORD
  }
});

// Index user profiles
await esClient.index({
  index: 'users',
  id: user.id,
  document: {
    firstName: user.firstName,
    bio: user.bio,
    interests: user.interests,
    location: {
      lat: user.latitude,
      lon: user.longitude
    }
  }
});

// Search
const result = await esClient.search({
  index: 'users',
  body: {
    query: {
      bool: {
        must: [
          { match: { interests: 'hiking' } }
        ],
        filter: [
          { geo_distance: {
            distance: '50km',
            location: { lat: 40.7128, lon: -74.0060 }
          }}
        ]
      }
    }
  }
});
```

### Analytics Database: **ClickHouse** or **BigQuery**

**ClickHouse** (self-hosted):
```sql
CREATE TABLE events (
  timestamp DateTime,
  user_id UUID,
  event_type String,
  properties String
) ENGINE = MergeTree()
ORDER BY (timestamp, user_id);
```

**BigQuery** (managed):
```javascript
import { BigQuery } from '@google-cloud/bigquery';

const bigquery = new BigQuery();

await bigquery
  .dataset('dating_app')
  .table('events')
  .insert({
    timestamp: new Date().toISOString(),
    user_id: userId,
    event_type: 'profile_view',
    properties: JSON.stringify(properties)
  });
```

---

## Infrastructure & Cloud

### Recommended: **AWS (Amazon Web Services)**

#### Why AWS?

**Pros**:
✅ Most comprehensive service offering
✅ Global reach (multiple regions)
✅ Mature ecosystem
✅ Extensive documentation
✅ Used by Tinder, Bumble, Hinge
✅ Strong ML/AI services (SageMaker)

### Core AWS Services

| Service | Purpose | Alternative |
|---------|---------|-------------|
| **EC2** | Compute instances | Fargate (serverless) |
| **ECS/EKS** | Container orchestration | Self-managed Kubernetes |
| **RDS** | Managed PostgreSQL | Self-hosted on EC2 |
| **ElastiCache** | Managed Redis | Self-hosted on EC2 |
| **S3** | Object storage (images/videos) | - |
| **CloudFront** | CDN | Cloudflare |
| **Lambda** | Serverless functions | - |
| **API Gateway** | API management | ALB + Custom |
| **SQS/SNS** | Message queue/pub-sub | Kafka, RabbitMQ |
| **Route 53** | DNS | Cloudflare DNS |
| **CloudWatch** | Logging/Monitoring | DataDog, New Relic |
| **Cognito** | User authentication | Custom auth service |
| **SageMaker** | ML model training | Self-hosted |

### Infrastructure as Code: **Terraform**

```hcl
# main.tf
provider "aws" {
  region = "us-east-1"
}

# VPC
resource "aws_vpc" "main" {
  cidr_block = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support = true

  tags = {
    Name = "dating-app-vpc"
  }
}

# RDS PostgreSQL
resource "aws_db_instance" "postgres" {
  identifier = "dating-app-db"
  engine = "postgres"
  engine_version = "15.4"
  instance_class = "db.r6g.large"
  allocated_storage = 100
  storage_encrypted = true

  db_name = "datingapp"
  username = var.db_username
  password = var.db_password

  vpc_security_group_ids = [aws_security_group.db.id]
  db_subnet_group_name = aws_db_subnet_group.main.name

  backup_retention_period = 7
  backup_window = "03:00-04:00"
  maintenance_window = "mon:04:00-mon:05:00"

  multi_az = true

  tags = {
    Name = "dating-app-postgres"
  }
}

# ElastiCache Redis
resource "aws_elasticache_cluster" "redis" {
  cluster_id = "dating-app-redis"
  engine = "redis"
  node_type = "cache.r6g.large"
  num_cache_nodes = 1
  parameter_group_name = "default.redis7"
  port = 6379

  subnet_group_name = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]

  tags = {
    Name = "dating-app-redis"
  }
}

# S3 Bucket for media
resource "aws_s3_bucket" "media" {
  bucket = "dating-app-media"

  tags = {
    Name = "dating-app-media"
  }
}

resource "aws_s3_bucket_versioning" "media" {
  bucket = aws_s3_bucket.media.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "media" {
  bucket = aws_s3_bucket.media.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# ECS Cluster
resource "aws_ecs_cluster" "main" {
  name = "dating-app-cluster"

  setting {
    name = "containerInsights"
    value = "enabled"
  }
}
```

### Alternative: **Google Cloud Platform (GCP)**

**Consider GCP if**:
- Better pricing for your workload
- Prefer Google's ML tools (TensorFlow)
- Strong BigQuery needs

**Key GCP Services**:
- Compute Engine (VMs)
- Google Kubernetes Engine (GKE)
- Cloud SQL (PostgreSQL)
- Memorystore (Redis)
- Cloud Storage (S3 equivalent)
- Cloud CDN
- Cloud Functions (Lambda equivalent)
- Pub/Sub (SNS/SQS equivalent)

---

## DevOps & CI/CD

### Container Orchestration: **Kubernetes (EKS)**

**Why Kubernetes?**
✅ Industry standard
✅ Portable across clouds
✅ Auto-scaling and self-healing
✅ Service mesh integration (Istio, Linkerd)

**Alternative**: **AWS ECS** (simpler, AWS-specific)

### CI/CD Pipeline: **GitHub Actions**

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '20'

      - name: Install dependencies
        run: npm ci

      - name: Run tests
        run: npm test

      - name: Run linter
        run: npm run lint

  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: us-east-1

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v1

      - name: Build and push Docker image
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          IMAGE_TAG: ${{ github.sha }}
        run: |
          docker build -t $ECR_REGISTRY/match-service:$IMAGE_TAG .
          docker push $ECR_REGISTRY/match-service:$IMAGE_TAG

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to EKS
        run: |
          aws eks update-kubeconfig --name dating-app-cluster
          kubectl set image deployment/match-service \
            match-service=$ECR_REGISTRY/match-service:${{ github.sha }}
          kubectl rollout status deployment/match-service
```

### Alternative CI/CD:

- **GitLab CI/CD**: Integrated with GitLab
- **CircleCI**: Good for complex workflows
- **Jenkins**: Self-hosted, highly customizable

---

## Monitoring & Analytics

### Application Monitoring: **DataDog** or **New Relic**

**DataDog**:
```javascript
import { StatsD } from 'node-dogstatsd';

const dogstatsd = new StatsD();

// Track metrics
dogstatsd.increment('user.signup');
dogstatsd.timing('api.response_time', 245);
dogstatsd.gauge('matches.pending', 1234);
```

**Alternative**: **CloudWatch** (AWS native, simpler but less features)

### Error Tracking: **Sentry**

```typescript
import * as Sentry from '@sentry/node';

Sentry.init({
  dsn: process.env.SENTRY_DSN,
  environment: process.env.NODE_ENV,
  tracesSampleRate: 0.1,
});

// Automatically capture errors
app.use(Sentry.Handlers.errorHandler());
```

### Analytics: **Mixpanel** or **Amplitude**

```typescript
import Mixpanel from 'mixpanel';

const mixpanel = Mixpanel.init(process.env.MIXPANEL_TOKEN);

// Track events
mixpanel.track('Profile View', {
  distinct_id: userId,
  viewed_user_id: profileId,
  source: 'discovery'
});

// User properties
mixpanel.people.set(userId, {
  $name: user.firstName,
  $email: user.email,
  premium: user.isPremium
});
```

**Alternative**: **Google Analytics 4** (free, web-focused)

### Log Aggregation: **ELK Stack** or **CloudWatch Logs**

**ELK** (Elasticsearch, Logstash, Kibana):
- Self-hosted or managed (Elastic Cloud)
- Powerful querying
- Custom dashboards

**CloudWatch Logs**:
- AWS native
- Simpler setup
- Good for AWS-centric architecture

---

## Complete Stack Summary

### Recommended Tech Stack (Startup/MVP)

```
┌─────────────────────────────────────────────────────────┐
│ MOBILE                                                   │
├─────────────────────────────────────────────────────────┤
│ React Native + TypeScript                               │
│ Redux Toolkit (state management)                        │
│ React Navigation                                         │
│ Socket.io-client (real-time)                           │
│ React Query (API caching)                               │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ BACKEND                                                  │
├─────────────────────────────────────────────────────────┤
│ Node.js 20 LTS + TypeScript                            │
│ Express (or Fastify)                                    │
│ Socket.io (real-time)                                   │
│ Prisma (ORM)                                            │
│ JWT (authentication)                                     │
│ KafkaJS (event streaming)                              │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ DATABASES                                                │
├─────────────────────────────────────────────────────────┤
│ PostgreSQL 15 (primary data)                            │
│   └─ PostGIS extension (geolocation)                   │
│ Redis 7 (cache + real-time)                            │
│ MongoDB 7 (messages + events)                           │
│ Elasticsearch 8 (search) [optional]                    │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ INFRASTRUCTURE                                           │
├─────────────────────────────────────────────────────────┤
│ AWS                                                      │
│   ├─ ECS/EKS (containers)                              │
│   ├─ RDS PostgreSQL (managed DB)                       │
│   ├─ ElastiCache Redis                                 │
│   ├─ S3 (media storage)                                │
│   ├─ CloudFront (CDN)                                  │
│   ├─ ALB (load balancer)                               │
│   └─ SQS/SNS (messaging)                               │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ ML/AI                                                    │
├─────────────────────────────────────────────────────────┤
│ Python 3.11                                             │
│ FastAPI (ML service endpoints)                          │
│ scikit-learn (matching algorithm)                      │
│ TensorFlow/PyTorch (deep learning)                     │
│ AWS SageMaker (training/deployment)                    │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ DEVOPS                                                   │
├─────────────────────────────────────────────────────────┤
│ Docker (containerization)                               │
│ Terraform (infrastructure as code)                      │
│ GitHub Actions (CI/CD)                                  │
│ Kubernetes (orchestration)                              │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ MONITORING                                               │
├─────────────────────────────────────────────────────────┤
│ DataDog (APM + logs)                                    │
│ Sentry (error tracking)                                 │
│ Mixpanel (product analytics)                            │
│ CloudWatch (AWS metrics)                                │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ THIRD-PARTY SERVICES                                     │
├─────────────────────────────────────────────────────────┤
│ Firebase (push notifications)                           │
│ Twilio (SMS verification)                               │
│ SendGrid (emails)                                       │
│ Stripe (payments)                                       │
│ Auth0/Firebase Auth (social login) [optional]          │
└─────────────────────────────────────────────────────────┘
```

### Development Timeline Estimate

**Phase 1: MVP (3-4 months)**
- User registration/auth
- Profile creation
- Basic swipe mechanics
- Match detection
- Simple messaging
- Core backend infrastructure

**Phase 2: Enhanced Features (2-3 months)**
- Real-time messaging improvements
- Push notifications
- Photo verification
- Basic recommendation algorithm
- Premium features (if applicable)

**Phase 3: Scale & Optimize (ongoing)**
- ML-powered matching
- Performance optimization
- Advanced analytics
- A/B testing framework
- Internationalization

---

## Decision Matrix

Use this to decide on key technology choices:

### Mobile Development

| Factor | React Native | Flutter | Native (Swift/Kotlin) |
|--------|--------------|---------|------------------------|
| Time to Market | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |
| Performance | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Developer Pool | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| Community/Libraries | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Cost | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ |

**Recommendation**: React Native for MVP, consider native for scale

### Backend Language

| Factor | Node.js | Python | Go |
|--------|---------|--------|-----|
| Real-time Features | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| ML Integration | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| Developer Pool | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| Performance | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Ecosystem | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |

**Recommendation**: Node.js for main API, Python for ML service

### Cloud Provider

| Factor | AWS | GCP | Azure |
|--------|-----|-----|-------|
| Service Breadth | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| ML/AI Tools | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| Pricing | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| Documentation | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Market Leader | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |

**Recommendation**: AWS (industry standard for dating apps)

---

## Next Steps

1. **Set up development environment**
2. **Initialize Git repository**
3. **Set up CI/CD pipeline**
4. **Create MVP project structure**
5. **Implement core services (User, Auth, Match)**
6. **Deploy to staging environment**
7. **Test at scale**
8. **Launch MVP**

Proceed to [Differentiators & Best Practices](04-differentiators-best-practices.md) for strategic recommendations.
