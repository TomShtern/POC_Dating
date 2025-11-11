# Hybrid Architecture: Java + Node.js

## Table of Contents
- [Architecture Overview](#architecture-overview)
- [When to Use Java vs Node.js](#when-to-use-java-vs-nodejs)
- [Service Communication](#service-communication)
- [Shared Authentication](#shared-authentication)
- [Data Consistency](#data-consistency)
- [Deployment Strategy](#deployment-strategy)
- [Monitoring & Observability](#monitoring--observability)
- [Example Implementations](#example-implementations)

---

## Architecture Overview

### High-Level System Design

```
┌─────────────────────────────────────────────────────────────────┐
│                         MOBILE APPS                              │
│     ┌──────────────────┐              ┌──────────────────┐     │
│     │   Android        │              │      iOS         │     │
│     │  (Kotlin)        │              │    (Swift)       │     │
│     └──────────────────┘              └──────────────────┘     │
└────────────────────────┬────────────────────┬───────────────────┘
                         │                    │
                         ▼                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                    API GATEWAY (Kong / Spring Cloud Gateway)     │
│  • Request Routing                                               │
│  • Load Balancing                                                │
│  • Rate Limiting                                                 │
│  • Authentication Gateway                                        │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  JAVA SERVICES   │  │  JAVA SERVICES   │  │  NODE.JS         │
│  (Spring Boot)   │  │  (Spring Boot)   │  │  (Socket.IO)     │
│                  │  │                  │  │                  │
│  • Auth Service  │  │  • Match Service │  │  • Messaging     │
│  • User Service  │  │  • Geo Service   │  │  • WebSocket     │
│  • Profile Svc   │  │  • Photo Svc     │  │  • Presence      │
│  • Payment Svc   │  │  • Admin Svc     │  │  • Typing        │
└──────────────────┘  └──────────────────┘  └──────────────────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     MESSAGE BROKER (Kafka)                       │
│  • Event Distribution                                            │
│  • Service-to-Service Communication                              │
│  • Event Sourcing                                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        DATA LAYER                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ PostgreSQL   │  │    Redis     │  │   MongoDB    │         │
│  │ (Profiles)   │  │   (Cache)    │  │  (Messages)  │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

### Service Distribution Strategy

| Service | Language | Rationale |
|---------|----------|-----------|
| **Auth Service** | ☕ Java | Complex business logic, security-critical |
| **User Service** | ☕ Java | CRUD operations, JPA/Hibernate efficiency |
| **Match Service** | ☕ Java | CPU-intensive algorithm, parallel processing |
| **Geolocation** | ☕ Java | PostGIS integration, complex spatial queries |
| **Profile Service** | ☕ Java | Data validation, business rules |
| **Photo Service** | ☕ Java | Image processing, S3 integration |
| **Payment Service** | ☕ Java | Financial transactions, strong typing |
| **Admin Service** | ☕ Java | Complex queries, reporting |
| **Messaging** | 🟢 Node.js | WebSocket connections, real-time |
| **Presence Service** | 🟢 Node.js | Lightweight, high concurrency |
| **Notification** | 🟢 Node.js | Push notifications, FCM/APNS |

---

## When to Use Java vs Node.js

### Use Java (Spring Boot) When:

✅ **Complex Business Logic**
- Multi-step workflows with transactions
- Domain-driven design requirements
- Strong type safety needed

✅ **CPU-Intensive Operations**
- Matching algorithms with parallel processing
- Image/video processing
- Data analytics and reporting

✅ **Database-Heavy Operations**
- Complex queries with JPA/Hibernate
- Batch processing
- Data migrations

✅ **Enterprise Integration**
- Integration with existing Java systems
- Corporate security requirements
- Compliance and auditing

✅ **Financial Transactions**
- Payment processing
- Subscription management
- Strong consistency guarantees

**Example Services:**
```java
// Matching algorithm - CPU intensive, parallel processing
@Service
public class MatchingService {
    public List<UserMatch> findMatches(UUID userId) {
        // Parallel stream processing
        return candidates.parallelStream()
            .map(candidate -> calculateCompatibility(userId, candidate))
            .filter(match -> match.getScore() > threshold)
            .sorted(Comparator.comparing(UserMatch::getScore).reversed())
            .limit(50)
            .collect(Collectors.toList());
    }
}
```

### Use Node.js When:

✅ **Real-time Communication**
- WebSocket connections
- Server-Sent Events (SSE)
- Live updates and notifications

✅ **I/O-Bound Operations**
- Proxying requests
- File streaming
- API aggregation

✅ **High Concurrency (Lightweight)**
- Online presence tracking
- Typing indicators
- Read-heavy operations

✅ **Rapid Prototyping**
- Quick iteration needed
- Simple CRUD APIs
- Microservice experimentation

**Example Services:**
```typescript
// Real-time messaging - event-driven, high concurrency
io.on('connection', (socket) => {
  const userId = socket.data.userId;

  // Handle thousands of concurrent connections efficiently
  socket.on('send_message', async (data) => {
    const message = await saveMessage(data);
    io.to(`match:${data.matchId}`).emit('new_message', message);
  });

  socket.on('typing', (data) => {
    socket.to(`match:${data.matchId}`).emit('user_typing', { userId });
  });
});
```

---

## Service Communication

### 1. Synchronous Communication (REST)

**Java → Node.js (REST API)**

```java
// Java service calling Node.js service
@Service
@RequiredArgsConstructor
public class MessageProxyService {

    private final RestTemplate restTemplate;

    @Value("${services.messaging.url}")
    private String messagingServiceUrl;

    public MessageResponse sendMessage(SendMessageRequest request) {
        String url = messagingServiceUrl + "/api/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getCurrentUserToken());

        HttpEntity<SendMessageRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<MessageResponse> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            MessageResponse.class
        );

        return response.getBody();
    }
}
```

**Node.js → Java (REST API)**

```typescript
// Node.js service calling Java service
import axios from 'axios';

export class UserServiceClient {
  private readonly baseUrl = process.env.USER_SERVICE_URL;

  async getUserById(userId: string, token: string): Promise<User> {
    try {
      const response = await axios.get(`${this.baseUrl}/api/users/${userId}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        timeout: 5000, // 5 second timeout
      });

      return response.data;
    } catch (error) {
      console.error('Error fetching user:', error);
      throw new Error('Failed to fetch user from Java service');
    }
  }
}
```

### 2. Asynchronous Communication (Kafka)

**Kafka Configuration (Java)**

```java
// Java Kafka Producer
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}

// Publishing events
@Service
@RequiredArgsConstructor
public class MatchEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishMatchCreated(Match match) {
        MatchCreatedEvent event = MatchCreatedEvent.builder()
            .matchId(match.getId())
            .userId1(match.getUserId1())
            .userId2(match.getUserId2())
            .timestamp(LocalDateTime.now())
            .build();

        kafkaTemplate.send("match.created", match.getId().toString(), event);
    }
}

// Consuming events
@Service
@Slf4j
public class MatchEventConsumer {

    @KafkaListener(topics = "match.created", groupId = "notification-service")
    public void handleMatchCreated(MatchCreatedEvent event) {
        log.info("Match created: {}", event.getMatchId());
        // Send notifications to both users
    }
}
```

**Kafka Configuration (Node.js)**

```typescript
// Node.js Kafka Consumer
import { Kafka } from 'kafkajs';

const kafka = new Kafka({
  clientId: 'messaging-service',
  brokers: [process.env.KAFKA_BROKER],
});

const consumer = kafka.consumer({ groupId: 'messaging-group' });

export async function consumeMatchEvents() {
  await consumer.connect();
  await consumer.subscribe({ topic: 'match.created', fromBeginning: false });

  await consumer.run({
    eachMessage: async ({ topic, partition, message }) => {
      const event = JSON.parse(message.value.toString());

      console.log('Match created event received:', event);

      // Send welcome message to both users
      await sendWelcomeMessage(event.userId1, event.userId2, event.matchId);

      // Notify via WebSocket
      io.to(`user:${event.userId1}`).emit('new_match', event);
      io.to(`user:${event.userId2}`).emit('new_match', event);
    },
  });
}

// Node.js Kafka Producer
const producer = kafka.producer();

export async function publishMessageSent(message: Message) {
  await producer.connect();
  await producer.send({
    topic: 'message.sent',
    messages: [
      {
        key: message.matchId,
        value: JSON.stringify({
          messageId: message.id,
          matchId: message.matchId,
          senderId: message.senderId,
          content: message.content,
          timestamp: message.createdAt,
        }),
      },
    ],
  });
}
```

### 3. gRPC (High-Performance)

**Java gRPC Service**

```protobuf
// user-service.proto
syntax = "proto3";

option java_package = "com.datingapp.grpc";
option java_outer_classname = "UserServiceProto";

service UserService {
  rpc GetUser(GetUserRequest) returns (UserResponse);
  rpc GetUsersBatch(GetUsersBatchRequest) returns (GetUsersBatchResponse);
}

message GetUserRequest {
  string user_id = 1;
}

message UserResponse {
  string id = 1;
  string email = 2;
  string first_name = 3;
  string last_name = 4;
  int32 age = 5;
}

message GetUsersBatchRequest {
  repeated string user_ids = 1;
}

message GetUsersBatchResponse {
  repeated UserResponse users = 1;
}
```

```java
// Java gRPC Server Implementation
@GrpcService
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void getUser(GetUserRequest request, StreamObserver<UserResponse> responseObserver) {
        UUID userId = UUID.fromString(request.getUserId());

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new StatusRuntimeException(Status.NOT_FOUND));

        UserResponse response = UserResponse.newBuilder()
            .setId(user.getId().toString())
            .setEmail(user.getEmail())
            .setFirstName(user.getFirstName())
            .setLastName(user.getLastName())
            .setAge(user.getAge())
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getUsersBatch(GetUsersBatchRequest request, StreamObserver<GetUsersBatchResponse> responseObserver) {
        List<UUID> userIds = request.getUserIdsList().stream()
            .map(UUID::fromString)
            .collect(Collectors.toList());

        List<User> users = userRepository.findAllById(userIds);

        GetUsersBatchResponse.Builder responseBuilder = GetUsersBatchResponse.newBuilder();

        users.forEach(user -> {
            UserResponse userResponse = UserResponse.newBuilder()
                .setId(user.getId().toString())
                .setEmail(user.getEmail())
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setAge(user.getAge())
                .build();
            responseBuilder.addUsers(userResponse);
        });

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
```

**Node.js gRPC Client**

```typescript
// Node.js gRPC Client
import grpc from '@grpc/grpc-js';
import protoLoader from '@grpc/proto-loader';

const packageDefinition = protoLoader.loadSync('user-service.proto', {
  keepCase: true,
  longs: String,
  enums: String,
  defaults: true,
  oneofs: true,
});

const userProto = grpc.loadPackageDefinition(packageDefinition).UserService;

const client = new userProto(
  'user-service:50051',
  grpc.credentials.createInsecure()
);

export async function getUserById(userId: string): Promise<User> {
  return new Promise((resolve, reject) => {
    client.GetUser({ user_id: userId }, (error, response) => {
      if (error) {
        reject(error);
      } else {
        resolve({
          id: response.id,
          email: response.email,
          firstName: response.first_name,
          lastName: response.last_name,
          age: response.age,
        });
      }
    });
  });
}

export async function getUsersBatch(userIds: string[]): Promise<User[]> {
  return new Promise((resolve, reject) => {
    client.GetUsersBatch({ user_ids: userIds }, (error, response) => {
      if (error) {
        reject(error);
      } else {
        resolve(response.users.map(u => ({
          id: u.id,
          email: u.email,
          firstName: u.first_name,
          lastName: u.last_name,
          age: u.age,
        })));
      }
    });
  });
}
```

---

## Shared Authentication

### JWT Validation in Both Platforms

**Java JWT Validation (Spring Security)**

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);

            try {
                UUID userId = jwtService.extractUserId(jwt);

                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    if (jwtService.validateToken(jwt, userId)) {
                        UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                userId, null, Collections.emptyList()
                            );
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (Exception e) {
                log.error("JWT validation failed", e);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

**Node.js JWT Validation (Express Middleware)**

```typescript
import jwt from 'jsonwebtoken';
import { Request, Response, NextFunction } from 'express';

const JWT_SECRET = process.env.JWT_SECRET!;

export interface AuthRequest extends Request {
  userId?: string;
}

export function authenticateToken(req: AuthRequest, res: Response, next: NextFunction) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1]; // Bearer <token>

  if (!token) {
    return res.status(401).json({ error: 'Access token required' });
  }

  try {
    const payload = jwt.verify(token, JWT_SECRET) as any;

    if (payload.type !== 'access') {
      return res.status(403).json({ error: 'Invalid token type' });
    }

    req.userId = payload.sub; // User ID from JWT subject
    next();
  } catch (error) {
    if (error instanceof jwt.TokenExpiredError) {
      return res.status(401).json({ error: 'Access token expired', code: 'TOKEN_EXPIRED' });
    }
    return res.status(403).json({ error: 'Invalid token' });
  }
}

// Usage
app.post('/api/messages', authenticateToken, async (req: AuthRequest, res) => {
  const userId = req.userId;
  // Handle message sending
});
```

### Shared Secret Management

Both services must use the **same JWT secret**. Options:

1. **Environment Variables** (Simplest)
```bash
# .env for both Java and Node.js
JWT_SECRET=your-256-bit-secret-here
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000
```

2. **AWS Secrets Manager / HashiCorp Vault** (Production)
```java
// Java - Fetch from AWS Secrets Manager
@Configuration
public class SecretsConfig {
    @Bean
    public String jwtSecret() {
        AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard()
            .withRegion("us-east-1")
            .build();
        GetSecretValueRequest request = new GetSecretValueRequest()
            .withSecretId("dating-app/jwt-secret");
        GetSecretValueResult result = client.getSecretValue(request);
        return result.getSecretString();
    }
}
```

```typescript
// Node.js - Fetch from AWS Secrets Manager
import AWS from 'aws-sdk';

const secretsManager = new AWS.SecretsManager({ region: 'us-east-1' });

export async function getJwtSecret(): Promise<string> {
  const data = await secretsManager
    .getSecretValue({ SecretId: 'dating-app/jwt-secret' })
    .promise();
  return data.SecretString!;
}
```

---

## Data Consistency

### Event Sourcing Pattern

**Java - Publishing Domain Events**

```java
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Match createMatch(UUID userId1, UUID userId2) {
        // 1. Save to database
        Match match = Match.builder()
            .userId1(userId1)
            .userId2(userId2)
            .createdAt(LocalDateTime.now())
            .build();

        Match savedMatch = matchRepository.save(match);

        // 2. Publish event (within same transaction)
        MatchCreatedEvent event = MatchCreatedEvent.builder()
            .matchId(savedMatch.getId())
            .userId1(userId1)
            .userId2(userId2)
            .timestamp(LocalDateTime.now())
            .build();

        kafkaTemplate.send("match.created", savedMatch.getId().toString(), event);

        return savedMatch;
    }
}
```

**Node.js - Consuming and Updating State**

```typescript
// Node.js consumes event and updates its own state
consumer.run({
  eachMessage: async ({ topic, message }) => {
    const event = JSON.parse(message.value.toString());

    if (topic === 'match.created') {
      // Update MongoDB with match info for messaging context
      await matchesCollection.insertOne({
        _id: event.matchId,
        userId1: event.userId1,
        userId2: event.userId2,
        createdAt: new Date(event.timestamp),
        lastMessageAt: null,
      });

      // Send real-time notification
      io.to(`user:${event.userId1}`).emit('new_match', event);
      io.to(`user:${event.userId2}`).emit('new_match', event);

      // Send welcome message
      await sendWelcomeMessage(event.matchId, event.userId1, event.userId2);
    }
  },
});
```

### Saga Pattern for Distributed Transactions

```java
// Java - Orchestrator
@Service
@RequiredArgsConstructor
public class PremiumSubscriptionSaga {

    private final PaymentService paymentService;
    private final UserService userService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void subscribeToPremium(UUID userId, PaymentRequest payment) {
        String sagaId = UUID.randomUUID().toString();

        try {
            // Step 1: Process payment
            PaymentResult result = paymentService.processPayment(payment);

            if (!result.isSuccessful()) {
                throw new PaymentFailedException("Payment failed");
            }

            // Step 2: Upgrade user account
            userService.upgradeToPremium(userId);

            // Step 3: Publish success event
            kafkaTemplate.send("subscription.completed", sagaId, new SubscriptionCompletedEvent(userId));

        } catch (Exception e) {
            // Compensating transactions
            log.error("Saga failed, executing compensations", e);

            if (result != null && result.isSuccessful()) {
                paymentService.refundPayment(result.getTransactionId());
            }

            kafkaTemplate.send("subscription.failed", sagaId, new SubscriptionFailedEvent(userId, e.getMessage()));
        }
    }
}
```

---

## Deployment Strategy

### Docker Compose (Development)

```yaml
# docker-compose.yml
version: '3.8'

services:
  # Java Services
  auth-service:
    build: ./dating-app-auth-service
    ports:
      - "8081:8081"
    environment:
      - DB_HOST=postgres
      - REDIS_HOST=redis
      - KAFKA_BROKER=kafka:9092
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      - postgres
      - redis
      - kafka

  user-service:
    build: ./dating-app-user-service
    ports:
      - "8082:8082"
    environment:
      - DB_HOST=postgres
      - REDIS_HOST=redis
      - KAFKA_BROKER=kafka:9092
    depends_on:
      - postgres
      - redis

  match-service:
    build: ./dating-app-match-service
    ports:
      - "8083:8083"
    environment:
      - DB_HOST=postgres
      - REDIS_HOST=redis
      - KAFKA_BROKER=kafka:9092
    depends_on:
      - postgres
      - redis

  # Node.js Services
  messaging-service:
    build: ./messaging-service
    ports:
      - "3001:3001"
    environment:
      - MONGODB_URI=mongodb://mongo:27017/dating_app
      - REDIS_URL=redis://redis:6379
      - KAFKA_BROKER=kafka:9092
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      - mongo
      - redis
      - kafka

  # Databases
  postgres:
    image: postgres:15-alpine
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=dating_app
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
    volumes:
      - postgres_data:/var/lib/postgresql/data

  mongo:
    image: mongo:7
    ports:
      - "27017:27017"
    volumes:
      - mongo_data:/data/db

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  # Kafka
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

volumes:
  postgres_data:
  mongo_data:
  redis_data:
```

### Kubernetes (Production)

```yaml
# Java Service Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: match-service
spec:
  replicas: 3
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
        image: dating-app/match-service:1.0.0
        ports:
        - containerPort: 8083
        env:
        - name: DB_HOST
          value: "postgres-service"
        - name: REDIS_HOST
          value: "redis-service"
        - name: KAFKA_BROKER
          value: "kafka-service:9092"
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8083
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8083
          initialDelaySeconds: 20
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
  - port: 8083
    targetPort: 8083
  type: ClusterIP
---
# Node.js Service Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: messaging-service
spec:
  replicas: 5  # More replicas for WebSocket connections
  selector:
    matchLabels:
      app: messaging-service
  template:
    metadata:
      labels:
        app: messaging-service
    spec:
      containers:
      - name: messaging-service
        image: dating-app/messaging-service:1.0.0
        ports:
        - containerPort: 3001
        env:
        - name: MONGODB_URI
          valueFrom:
            secretKeyRef:
              name: mongodb-secret
              key: uri
        - name: REDIS_URL
          value: "redis://redis-service:6379"
        - name: KAFKA_BROKER
          value: "kafka-service:9092"
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health
            port: 3001
          initialDelaySeconds: 10
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 3001
          initialDelaySeconds: 5
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: messaging-service
spec:
  selector:
    app: messaging-service
  ports:
  - port: 3001
    targetPort: 3001
  type: ClusterIP
  sessionAffinity: ClientIP  # Sticky sessions for WebSocket
```

---

## Monitoring & Observability

### Distributed Tracing (OpenTelemetry)

**Java Configuration**

```java
@Configuration
public class TracingConfig {

    @Bean
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault()
            .merge(Resource.create(Attributes.of(
                ResourceAttributes.SERVICE_NAME, "match-service",
                ResourceAttributes.SERVICE_VERSION, "1.0.0"
            )));

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(
                OtlpGrpcSpanExporter.builder()
                    .setEndpoint("http://otel-collector:4317")
                    .build()
            ).build())
            .setResource(resource)
            .build();

        return OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .buildAndRegisterGlobal();
    }
}
```

**Node.js Configuration**

```typescript
import { NodeTracerProvider } from '@opentelemetry/sdk-trace-node';
import { Resource } from '@opentelemetry/resources';
import { SemanticResourceAttributes } from '@opentelemetry/semantic-conventions';
import { BatchSpanProcessor } from '@opentelemetry/sdk-trace-base';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-grpc';

const provider = new NodeTracerProvider({
  resource: new Resource({
    [SemanticResourceAttributes.SERVICE_NAME]: 'messaging-service',
    [SemanticResourceAttributes.SERVICE_VERSION]: '1.0.0',
  }),
});

const exporter = new OTLPTraceExporter({
  url: 'http://otel-collector:4317',
});

provider.addSpanProcessor(new BatchSpanProcessor(exporter));
provider.register();
```

### Unified Logging

```yaml
# Fluentd configuration for log aggregation
<source>
  @type forward
  port 24224
</source>

<filter **>
  @type record_transformer
  <record>
    hostname "#{Socket.gethostname}"
    tag ${tag}
  </record>
</filter>

<match java.**>
  @type elasticsearch
  host elasticsearch
  port 9200
  index_name java-logs
  type_name _doc
</match>

<match nodejs.**>
  @type elasticsearch
  host elasticsearch
  port 9200
  index_name nodejs-logs
  type_name _doc
</match>
```

---

## Example Implementations

### Complete Match Flow (Java → Kafka → Node.js)

**1. Java creates match:**
```java
@Service
public class SwipeService {
    public SwipeResult processSwipe(UUID userId, UUID targetUserId, SwipeAction action) {
        // Check if mutual like
        Optional<Swipe> reciprocalSwipe = swipeRepository
            .findByUserIdAndTargetUserId(targetUserId, userId);

        if (reciprocalSwipe.isPresent() && reciprocalSwipe.get().getAction() == SwipeAction.LIKE) {
            // Create match
            Match match = matchService.createMatch(userId, targetUserId);

            // Publish event
            kafkaTemplate.send("match.created", new MatchCreatedEvent(match));

            return SwipeResult.matched(match);
        }

        return SwipeResult.swiped();
    }
}
```

**2. Node.js receives event and notifies users:**
```typescript
consumer.run({
  eachMessage: async ({ message }) => {
    const event: MatchCreatedEvent = JSON.parse(message.value.toString());

    // Send real-time notification via WebSocket
    io.to(`user:${event.userId1}`).emit('new_match', {
      matchId: event.matchId,
      userId: event.userId2,
    });

    io.to(`user:${event.userId2}`).emit('new_match', {
      matchId: event.matchId,
      userId: event.userId1,
    });

    // Create conversation in MongoDB
    await conversationsCollection.insertOne({
      _id: event.matchId,
      participants: [event.userId1, event.userId2],
      createdAt: new Date(),
      messages: [],
    });
  },
});
```

---

## Summary

### Architecture Benefits

✅ **Use best tool for each job**: Java for business logic, Node.js for real-time
✅ **Scalability**: Scale services independently based on load
✅ **Resilience**: Failure in one service doesn't affect others
✅ **Team flexibility**: Different teams can work in their preferred language
✅ **Performance**: Optimal performance for each use case

### Communication Summary

| Method | Use Case | Performance | Complexity |
|--------|----------|-------------|------------|
| **REST** | Simple CRUD, user-facing APIs | Good | Low |
| **gRPC** | Service-to-service, high throughput | Excellent | Medium |
| **Kafka** | Events, async processing, decoupling | Good | High |
| **WebSocket** | Real-time bidirectional | Excellent | Medium |

### Next Steps

1. Set up Kafka for event streaming
2. Implement OpenTelemetry for distributed tracing
3. Create shared authentication library
4. Set up CI/CD for both Java and Node.js services
5. Implement API Gateway (Kong or Spring Cloud Gateway)

**Next:** See `16-android-kotlin-architecture.md` for native Android implementation.
