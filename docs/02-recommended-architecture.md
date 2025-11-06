# Recommended Dating App Architecture

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [System Components](#system-components)
3. [Data Architecture](#data-architecture)
4. [Infrastructure Design](#infrastructure-design)
5. [Scalability Strategy](#scalability-strategy)
6. [Security Architecture](#security-architecture)

---

## Architecture Overview

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                              │
├─────────────────────────────────────────────────────────────────┤
│  iOS App (Swift/React Native)  │  Android App (Kotlin/RN)       │
│  Web App (React/Next.js)        │  Admin Dashboard               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CDN & EDGE LAYER                              │
├─────────────────────────────────────────────────────────────────┤
│  CloudFront CDN (Static Assets, Images, Videos)                 │
│  API Gateway / Load Balancer                                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    API GATEWAY LAYER                             │
├─────────────────────────────────────────────────────────────────┤
│  REST API Gateway      │  WebSocket Gateway (Real-time)         │
│  Authentication/JWT    │  Rate Limiting                          │
│  Request Routing       │  API Versioning                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  MICROSERVICES LAYER                             │
├──────────────────┬──────────────────┬───────────────────────────┤
│  User Service    │  Match Service   │  Messaging Service        │
│  Auth Service    │  Profile Service │  Notification Service     │
│  Geo Service     │  ML/AI Service   │  Payment Service          │
│  Media Service   │  Search Service  │  Analytics Service        │
│  Moderation Svc  │  Admin Service   │  Recommendation Engine    │
└──────────────────┴──────────────────┴───────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    DATA LAYER                                    │
├──────────────────┬──────────────────┬───────────────────────────┤
│  PostgreSQL      │  Redis Cache     │  MongoDB                  │
│  (User Profiles) │  (Sessions)      │  (Messages, Events)       │
│                  │  (Geo Cache)     │                           │
├──────────────────┼──────────────────┼───────────────────────────┤
│  S3              │  ElasticSearch   │  Analytics DB             │
│  (Media Storage) │  (User Search)   │  (ClickHouse/BigQuery)    │
└──────────────────┴──────────────────┴───────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  MESSAGE QUEUE & EVENT BUS                       │
├─────────────────────────────────────────────────────────────────┤
│  Kafka / AWS SQS / RabbitMQ                                     │
│  (Event-driven communication between services)                   │
└─────────────────────────────────────────────────────────────────┘
```

### Architecture Principles

1. **Microservices-First**: Independent services that can scale and deploy separately
2. **Event-Driven**: Services communicate via events/messages for loose coupling
3. **API-First**: Well-defined REST/GraphQL APIs with versioning
4. **Cloud-Native**: Built for cloud deployment (AWS/GCP)
5. **Security by Design**: Authentication, encryption, and privacy at every layer
6. **Data-Driven**: Comprehensive analytics and ML integration
7. **Mobile-First**: Optimized for mobile app experience

---

## System Components

### 1. User Service

**Responsibilities**:
- User registration and onboarding
- Profile management (bio, photos, preferences)
- Account settings and preferences
- User verification and safety

**Technology**:
- Language: Node.js (TypeScript) or Python (FastAPI)
- Database: PostgreSQL (primary), Redis (cache)
- Storage: S3 for profile photos

**API Endpoints**:
```
POST   /api/v1/users/register
POST   /api/v1/users/login
GET    /api/v1/users/:id/profile
PUT    /api/v1/users/:id/profile
POST   /api/v1/users/:id/photos
DELETE /api/v1/users/:id/photos/:photoId
PUT    /api/v1/users/:id/preferences
```

**Data Model**:
```sql
users (
  id UUID PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  phone VARCHAR(20),
  password_hash VARCHAR(255),
  first_name VARCHAR(100),
  date_of_birth DATE,
  gender VARCHAR(20),
  bio TEXT,
  location GEOGRAPHY(POINT),
  last_seen TIMESTAMP,
  verified BOOLEAN DEFAULT false,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)

user_photos (
  id UUID PRIMARY KEY,
  user_id UUID REFERENCES users(id),
  photo_url VARCHAR(500),
  order_index INTEGER,
  is_primary BOOLEAN DEFAULT false,
  created_at TIMESTAMP
)

user_preferences (
  user_id UUID PRIMARY KEY REFERENCES users(id),
  min_age INTEGER,
  max_age INTEGER,
  max_distance INTEGER,
  interested_in VARCHAR(50),
  show_me VARCHAR(50),
  updated_at TIMESTAMP
)
```

---

### 2. Authentication Service

**Responsibilities**:
- User authentication (login/logout)
- JWT token generation and validation
- OAuth integration (Facebook, Google, Apple)
- Two-factor authentication (2FA)
- Password reset flows

**Technology**:
- Language: Node.js or Go (high performance)
- Token: JWT with RS256 signing
- Storage: Redis (refresh tokens, blacklist)
- OAuth Providers: Passport.js / OAuth2.0

**Flow**:
```
1. User submits credentials
2. Auth service validates against user database
3. Generate access token (JWT, 15min TTL) + refresh token (7 days)
4. Store refresh token in Redis
5. Return tokens to client
6. Client includes JWT in Authorization header for subsequent requests
7. API Gateway validates JWT before routing to services
```

**Security Features**:
- Argon2 or bcrypt for password hashing
- JWT with short expiration (15 minutes)
- Refresh token rotation
- Rate limiting on login attempts
- Device fingerprinting
- Suspicious activity detection

---

### 3. Geolocation Service

**Responsibilities**:
- Process and store user locations
- Find users within specified radius
- Update location in real-time (background updates)
- Calculate distances between users

**Technology**:
- Language: Go or Node.js (high throughput)
- Database: PostgreSQL with PostGIS extension
- Cache: Redis with geospatial commands (GEORADIUS)

**Location Update Strategy**:
```javascript
// Client periodically sends location updates
// Frequency depends on user activity:
// - Active in app: every 1-2 minutes
// - Background: every 15-30 minutes (with permission)
// - Idle: on app open

Location Update Flow:
1. Mobile app sends lat/lng to Geo Service
2. Geo Service updates Redis (GEOADD users:locations)
3. Geo Service updates PostgreSQL (batched every 5 minutes)
4. TTL on Redis location: 1 hour (prevents stale data)
```

**Search Query**:
```sql
-- PostgreSQL with PostGIS
SELECT
  u.id,
  u.first_name,
  ST_Distance(u.location, ST_MakePoint(:lng, :lat)::geography) as distance
FROM users u
WHERE ST_DWithin(
  u.location,
  ST_MakePoint(:lng, :lat)::geography,
  :max_distance_meters
)
AND u.id != :current_user_id
ORDER BY distance
LIMIT 100;
```

**Redis Cache**:
```bash
# Store user locations
GEOADD users:locations <longitude> <latitude> <user_id>

# Find users within radius (in meters)
GEORADIUS users:locations <longitude> <latitude> 50000 m WITHDIST
```

---

### 4. Match Service (Recommendation Engine)

**Responsibilities**:
- Generate potential matches for users
- Apply filters (age, distance, preferences)
- Implement swipe mechanics (like/dislike/super like)
- Detect mutual likes (matches)
- Calculate compatibility scores

**Technology**:
- Language: Python (ML libraries) + Node.js (API)
- Database: PostgreSQL (match data), Redis (swipe cache)
- ML Framework: TensorFlow/PyTorch, scikit-learn
- Message Queue: Kafka (emit match events)

**Matching Algorithm (Multi-Stage)**:

**Stage 1: Filtering**
```python
# Basic filtering based on preferences
candidates = filter_users(
    current_user=user,
    filters={
        'age': (user.prefs.min_age, user.prefs.max_age),
        'distance': user.prefs.max_distance,
        'gender': user.prefs.interested_in,
        'exclude_seen': True,  # Don't show previously seen profiles
        'exclude_blocked': True
    }
)
```

**Stage 2: Scoring**
```python
# Multi-factor scoring
def calculate_match_score(user_a, user_b):
    score = 0

    # Profile completeness (10 points)
    score += profile_completeness_score(user_b) * 10

    # Collaborative filtering (30 points)
    # Users who liked similar profiles
    score += collaborative_filtering_score(user_a, user_b) * 30

    # Content-based filtering (20 points)
    # Profile attributes similarity
    score += content_similarity_score(user_a, user_b) * 20

    # Activity score (15 points)
    # Active users get boosted
    score += activity_score(user_b) * 15

    # Recency (15 points)
    # Recently active users
    score += recency_score(user_b) * 15

    # Response rate (10 points)
    # Users who respond to messages
    score += response_rate_score(user_b) * 10

    return min(score, 100)  # Normalize to 0-100
```

**Stage 3: Ranking**
```python
# Sort by score with randomization
# Add slight randomness to avoid showing same order to everyone
ranked_candidates = sorted(
    candidates,
    key=lambda x: (
        calculate_match_score(user, x) +
        random.uniform(-5, 5)  # Small random factor
    ),
    reverse=True
)
```

**Swipe Data Model**:
```sql
swipes (
  id UUID PRIMARY KEY,
  user_id UUID REFERENCES users(id),
  target_user_id UUID REFERENCES users(id),
  swipe_type VARCHAR(20), -- 'like', 'dislike', 'super_like'
  created_at TIMESTAMP,
  UNIQUE(user_id, target_user_id)
)

matches (
  id UUID PRIMARY KEY,
  user_a_id UUID REFERENCES users(id),
  user_b_id UUID REFERENCES users(id),
  matched_at TIMESTAMP,
  unmatched_at TIMESTAMP,
  CHECK (user_a_id < user_b_id),  -- Ensure consistent ordering
  UNIQUE(user_a_id, user_b_id)
)
```

**Match Detection**:
```sql
-- When user A swipes right on user B, check if B already swiped right on A
SELECT 1 FROM swipes
WHERE user_id = :user_b_id
  AND target_user_id = :user_a_id
  AND swipe_type IN ('like', 'super_like')
LIMIT 1;

-- If exists, create match and emit event
INSERT INTO matches (user_a_id, user_b_id, matched_at)
VALUES (LEAST(:user_a_id, :user_b_id), GREATEST(:user_a_id, :user_b_id), NOW());

-- Emit event to Kafka
PUBLISH 'matches' { user_a: user_a_id, user_b: user_b_id, timestamp: NOW() }
```

---

### 5. Messaging Service

**Responsibilities**:
- Real-time chat between matched users
- Message persistence
- Delivery and read receipts
- Media messages (photos, voice notes)
- Push notifications for new messages

**Technology**:
- Language: Node.js (WebSocket handling)
- Real-time: WebSocket (Socket.io or native WebSocket)
- Database: MongoDB (message storage) or Cassandra (high write throughput)
- Cache: Redis (online status, typing indicators)
- Queue: Kafka (message delivery events)

**Architecture**:
```
Mobile App  ←→  API Gateway (WebSocket)  ←→  Message Service  ←→  MongoDB
                                               ↓
                                          Redis (presence)
                                               ↓
                                          Kafka (events)
                                               ↓
                                     Notification Service
```

**WebSocket Connection**:
```javascript
// Client establishes WebSocket connection
const socket = io('wss://api.datingapp.com/ws', {
  auth: { token: jwt_token },
  transports: ['websocket']
});

// Server authenticates and stores connection
io.use((socket, next) => {
  const token = socket.handshake.auth.token;
  const user = verifyJWT(token);
  if (user) {
    socket.userId = user.id;
    next();
  } else {
    next(new Error('Authentication error'));
  }
});

// Store connection in Redis
io.on('connection', (socket) => {
  redis.sadd(`online:users`, socket.userId);
  redis.set(`socket:${socket.userId}`, socket.id, 'EX', 3600);

  socket.on('disconnect', () => {
    redis.srem(`online:users`, socket.userId);
    redis.del(`socket:${socket.userId}`);
  });
});
```

**Message Flow**:
```javascript
// User sends message
socket.emit('send_message', {
  match_id: 'match-uuid',
  content: 'Hello!',
  type: 'text'
});

// Server processes
io.on('send_message', async (data) => {
  // 1. Verify users are matched
  const match = await verifyMatch(socket.userId, data.match_id);

  // 2. Save message to database
  const message = await saveMessage({
    match_id: data.match_id,
    sender_id: socket.userId,
    content: data.content,
    type: data.type,
    sent_at: new Date()
  });

  // 3. Get recipient socket
  const recipientSocketId = await redis.get(`socket:${match.other_user_id}`);

  // 4. Send to recipient if online
  if (recipientSocketId) {
    io.to(recipientSocketId).emit('new_message', message);
  } else {
    // 5. Queue push notification
    kafka.publish('push_notifications', {
      user_id: match.other_user_id,
      type: 'new_message',
      data: message
    });
  }

  // 6. Send acknowledgment to sender
  socket.emit('message_sent', { message_id: message.id });
});
```

**Data Model**:
```javascript
// MongoDB schema
{
  _id: ObjectId,
  match_id: UUID,
  sender_id: UUID,
  content: String,
  type: 'text' | 'image' | 'voice' | 'video',
  media_url: String,  // if type != text
  sent_at: ISODate,
  delivered_at: ISODate,
  read_at: ISODate,
  deleted_by: [UUID]  // soft delete per user
}

// Index for fast queries
db.messages.createIndex({ match_id: 1, sent_at: -1 });
db.messages.createIndex({ sender_id: 1, sent_at: -1 });
```

---

### 6. Notification Service

**Responsibilities**:
- Push notifications (new match, message, like)
- Email notifications
- In-app notifications
- Notification preferences management

**Technology**:
- Language: Node.js or Go
- Push: Firebase Cloud Messaging (FCM), Apple Push Notification Service (APNS)
- Email: SendGrid, AWS SES
- Queue: Kafka or SQS (consume events)
- Database: PostgreSQL (notification history)

**Event-Driven Flow**:
```
Match Service → Kafka → Notification Service → FCM/APNS → Mobile Device
Message Service → Kafka → Notification Service → FCM/APNS → Mobile Device
```

**Notification Types**:
```javascript
const notificationTemplates = {
  new_match: {
    title: 'It\'s a Match!',
    body: '{user_name} likes you too!',
    priority: 'high',
    sound: 'match.mp3'
  },
  new_message: {
    title: '{user_name}',
    body: '{message_preview}',
    priority: 'high',
    sound: 'message.mp3'
  },
  new_like: {
    title: 'Someone likes you!',
    body: 'See who liked you with Premium',
    priority: 'normal'
  }
};
```

**Implementation**:
```javascript
// Consume events from Kafka
kafka.subscribe('matches', async (event) => {
  const { user_a, user_b } = event;

  // Send notification to both users
  await sendPushNotification(user_a, 'new_match', { user_name: user_b.name });
  await sendPushNotification(user_b, 'new_match', { user_name: user_a.name });
});

async function sendPushNotification(userId, type, data) {
  // Get user's device tokens
  const devices = await getDeviceTokens(userId);

  // Check user's notification preferences
  const prefs = await getNotificationPreferences(userId);
  if (!prefs[type]) return;  // User disabled this notification type

  // Build notification
  const template = notificationTemplates[type];
  const notification = {
    title: replaceTokens(template.title, data),
    body: replaceTokens(template.body, data),
    sound: template.sound,
    priority: template.priority,
    data: { type, ...data }
  };

  // Send to all user devices
  for (const device of devices) {
    if (device.platform === 'ios') {
      await apns.send(device.token, notification);
    } else {
      await fcm.send(device.token, notification);
    }
  }

  // Log notification
  await logNotification(userId, type, notification);
}
```

---

### 7. Media Service

**Responsibilities**:
- Upload and store photos/videos
- Image processing (resize, compress, format conversion)
- Content moderation (detect inappropriate content)
- CDN integration for fast delivery

**Technology**:
- Language: Node.js or Go
- Storage: AWS S3
- CDN: CloudFront
- Image Processing: Sharp (Node.js) or ImageMagick
- AI Moderation: AWS Rekognition or custom ML model

**Upload Flow**:
```javascript
// 1. Client requests upload URL (pre-signed)
POST /api/v1/media/upload-url
Response: {
  upload_url: 's3-presigned-url',
  photo_id: 'uuid',
  max_size: 10485760  // 10MB
}

// 2. Client uploads directly to S3
PUT s3-presigned-url (binary data)

// 3. S3 triggers Lambda/webhook on upload complete
S3 Event → Lambda → Media Service

// 4. Media Service processes image
async function processUploadedImage(photoId) {
  // Download original
  const original = await s3.download(`uploads/${photoId}/original.jpg`);

  // Generate thumbnails
  const sizes = [
    { name: 'thumbnail', width: 150, height: 150 },
    { name: 'medium', width: 640, height: 640 },
    { name: 'large', width: 1080, height: 1080 }
  ];

  for (const size of sizes) {
    const resized = await sharp(original)
      .resize(size.width, size.height, { fit: 'cover' })
      .jpeg({ quality: 85 })
      .toBuffer();

    await s3.upload(`uploads/${photoId}/${size.name}.jpg`, resized);
  }

  // Run content moderation
  const moderationResult = await rekognition.detectModerationLabels(original);

  if (moderationResult.unsafe) {
    await flagForReview(photoId);
    await notifyUser(userId, 'photo_flagged');
  } else {
    await markPhotoApproved(photoId);
  }
}
```

---

### 8. Analytics Service

**Responsibilities**:
- Track user events (swipes, matches, messages, logins)
- Generate insights for ML models
- Business analytics (retention, engagement, conversion)
- A/B testing infrastructure

**Technology**:
- Language: Python (data processing)
- Event Streaming: Kafka
- Data Warehouse: BigQuery, Redshift, or ClickHouse
- ETL: Apache Airflow or AWS Glue
- Analytics: Metabase, Looker, or custom dashboards

**Event Tracking**:
```javascript
// Client sends events
analytics.track('profile_view', {
  user_id: currentUserId,
  viewed_user_id: profileId,
  timestamp: Date.now(),
  source: 'discovery'
});

// Server processes and stores
kafka.publish('analytics', {
  event_type: 'profile_view',
  user_id: currentUserId,
  properties: { ... },
  timestamp: timestamp
});
```

---

## Data Architecture

### Database Selection Matrix

| Service | Primary DB | Cache | Rationale |
|---------|-----------|-------|-----------|
| User Service | PostgreSQL | Redis | Structured data, ACID compliance |
| Auth Service | Redis | - | Fast token lookup |
| Geolocation | PostgreSQL (PostGIS) | Redis | Spatial queries, geospatial cache |
| Match Service | PostgreSQL | Redis | Relational data, complex queries |
| Messaging | MongoDB/Cassandra | Redis | High write throughput, simple queries |
| Analytics | ClickHouse/BigQuery | - | OLAP, complex analytics |
| Search | Elasticsearch | - | Full-text search |

### Data Flow

```
User Action → API Gateway → Microservice → Primary DB → Event to Kafka
                                         ↘ Redis Cache

Kafka Event → Analytics Service → Data Warehouse
           ↘ Notification Service
           ↘ ML Pipeline
```

---

## Infrastructure Design

### AWS Architecture

```
Route 53 (DNS)
    ↓
CloudFront (CDN)
    ↓
Application Load Balancer
    ↓
ECS/EKS Cluster (Microservices)
    ↓
RDS (PostgreSQL) / DocumentDB (MongoDB)
ElastiCache (Redis)
S3 (Media Storage)
Lambda (Serverless functions)
SQS/SNS (Message Queue)
CloudWatch (Monitoring)
```

### Kubernetes Deployment

```yaml
# Example deployment for Match Service
apiVersion: apps/v1
kind: Deployment
metadata:
  name: match-service
spec:
  replicas: 5
  selector:
    matchLabels:
      app: match-service
  template:
    metadata:
      labels:
        app: match-service
    spec:
      containers:
      - name: match-service
        image: datingapp/match-service:v1.2.3
        ports:
        - containerPort: 3000
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: match-service-url
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /health
            port: 3000
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 3000
          initialDelaySeconds: 10
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: match-service
spec:
  selector:
    app: match-service
  ports:
  - protocol: TCP
    port: 80
    targetPort: 3000
  type: ClusterIP
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: match-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: match-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

---

## Scalability Strategy

### Horizontal Scaling

- **Stateless Services**: All microservices designed to be stateless
- **Auto-scaling**: Based on CPU, memory, request rate
- **Load Balancing**: Round-robin with health checks

### Database Scaling

**Read Replicas**:
```
Primary DB (writes) → Replica 1 (reads)
                   → Replica 2 (reads)
                   → Replica 3 (reads)
```

**Sharding Strategy** (for extreme scale):
```
User ID hash % 10 = Shard Number
Shard 0: Users 0, 10, 20, ...
Shard 1: Users 1, 11, 21, ...
...
Shard 9: Users 9, 19, 29, ...
```

### Caching Strategy

**Multi-Level Cache**:
```
1. Application cache (in-memory, per instance)
2. Redis cache (shared, cluster-wide)
3. CDN cache (for static assets)
```

**Cache Keys**:
```
user:profile:{user_id}           TTL: 1 hour
user:preferences:{user_id}       TTL: 6 hours
match:candidates:{user_id}       TTL: 5 minutes
geo:location:{user_id}           TTL: 1 hour
```

---

## Security Architecture

### Authentication Flow

```
1. User logs in → Auth Service validates credentials
2. Generate JWT (access token, 15 min) + Refresh Token (7 days)
3. Store refresh token in Redis with user ID mapping
4. Return both tokens to client
5. Client stores tokens securely (iOS Keychain / Android Keystore)
6. Client sends JWT in Authorization header for API calls
7. API Gateway validates JWT signature and expiration
8. If expired, client uses refresh token to get new access token
9. Refresh token rotation: issue new refresh token on use
```

### Data Encryption

- **In Transit**: TLS 1.3 for all API communication
- **At Rest**: AES-256 for database encryption (RDS encryption)
- **S3**: Server-side encryption (SSE-S3 or SSE-KMS)
- **Passwords**: Argon2id hashing with salt

### Privacy & Compliance

**GDPR Compliance**:
- Right to access: API endpoint for data export
- Right to deletion: Cascade delete across all services
- Right to portability: JSON export of all user data
- Consent management: Granular privacy settings

**Implementation**:
```javascript
// Data export
GET /api/v1/users/:id/export
Returns: ZIP file with:
- profile.json
- photos/
- conversations.json
- swipes.json
- matches.json

// Data deletion
DELETE /api/v1/users/:id
1. Soft delete in PostgreSQL (mark as deleted, schedule cleanup)
2. Remove from all caches
3. Delete photos from S3
4. Anonymize messages (replace with "[deleted user]")
5. Remove from ML training data
6. Send confirmation email
```

---

## Monitoring & Observability

### Metrics to Track

**Application Metrics**:
- Request rate per service
- Response time (p50, p95, p99)
- Error rate (4xx, 5xx)
- Database query performance
- Cache hit rate

**Business Metrics**:
- Daily Active Users (DAU)
- New registrations
- Swipes per user
- Match rate
- Message response rate
- Conversion to paid (if premium features)

**Infrastructure Metrics**:
- CPU/Memory usage
- Network traffic
- Disk I/O
- Database connections
- Cache memory usage

### Logging

**Structured Logging**:
```json
{
  "timestamp": "2024-11-06T10:30:45Z",
  "level": "INFO",
  "service": "match-service",
  "trace_id": "abc123",
  "user_id": "user-uuid",
  "action": "generate_candidates",
  "duration_ms": 245,
  "candidates_count": 50
}
```

**Centralized Logging**:
- Tool: ELK Stack (Elasticsearch, Logstash, Kibana) or CloudWatch Logs
- Retention: 30 days for debug logs, 1 year for audit logs

### Alerting

```yaml
Alerts:
  - name: High Error Rate
    condition: error_rate > 5%
    duration: 5 minutes
    action: Page on-call engineer

  - name: High Response Time
    condition: p95_response_time > 1000ms
    duration: 10 minutes
    action: Notify team Slack channel

  - name: Database Connection Pool Exhausted
    condition: db_connections > 90%
    action: Auto-scale database read replicas

  - name: Low Match Rate
    condition: match_rate < 1%
    duration: 1 hour
    action: Notify ML team (potential algorithm issue)
```

---

## Disaster Recovery

### Backup Strategy

- **Database**: Automated daily backups, 30-day retention
- **S3**: Versioning enabled, cross-region replication
- **Configuration**: Infrastructure as Code (Terraform/CloudFormation)

### Recovery Time Objective (RTO) & Recovery Point Objective (RPO)

- **RTO**: 4 hours (time to restore service)
- **RPO**: 1 hour (acceptable data loss)

### Failover Plan

```
Primary Region (us-east-1) DOWN
    ↓
DNS failover (Route 53 health check)
    ↓
Traffic routed to Secondary Region (us-west-2)
    ↓
Database promoted from read replica to primary
    ↓
Services auto-scale in secondary region
```

---

## Cost Optimization

### Estimated Monthly Costs (10,000 DAU)

| Component | Cost | Notes |
|-----------|------|-------|
| Compute (ECS/EKS) | $500 | ~10 containers |
| Database (RDS) | $300 | db.r5.large |
| Cache (ElastiCache) | $150 | cache.t3.medium |
| S3 Storage | $200 | 10TB images |
| CloudFront CDN | $100 | 50TB transfer |
| Load Balancer | $50 | Application LB |
| Total | **$1,300** | |

### Scaling Projections

- **100K DAU**: ~$8,000/month
- **1M DAU**: ~$50,000/month
- **10M DAU**: ~$300,000/month

### Cost Savings Strategies

1. **Reserved Instances**: 40% savings on compute
2. **S3 Lifecycle Policies**: Move old photos to Glacier
3. **Spot Instances**: For non-critical workloads
4. **Right-sizing**: Monitor and adjust instance sizes
5. **CDN Optimization**: Aggressive caching policies

---

## Summary

This architecture provides:

✅ **Scalability**: Microservices can scale independently
✅ **Performance**: Multi-level caching, CDN, geographically distributed
✅ **Reliability**: Redundancy, auto-scaling, disaster recovery
✅ **Security**: Encryption, authentication, GDPR compliance
✅ **Maintainability**: Clear service boundaries, comprehensive monitoring
✅ **Cost-Effective**: Pay-as-you-grow cloud infrastructure

**Next Steps**: Proceed to [Tech Stack Recommendations](03-tech-stack-recommendations.md) for specific technology choices.
