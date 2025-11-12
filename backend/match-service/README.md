# Match Service

## Overview

Microservice responsible for handling swipes and calculating mutual matches between users. Built with Spring Boot 3.2.0, this service manages the core matching logic for the POC Dating application.

## Port
**8082** (internal, accessed via API Gateway at port 8080)

## Prerequisites

### Required (for Default PostgreSQL Setup)
- **Java 21+** - Required for running the service
- **Maven 3.8+** - Build tool
- **PostgreSQL 14+** - Default database (local installation)

#### PostgreSQL Installation

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

**macOS (Homebrew):**
```bash
brew install postgresql@14
brew services start postgresql@14
```

**Windows:**
Download and install from [postgresql.org](https://www.postgresql.org/download/windows/)

**Create Database:**
```bash
# Connect to PostgreSQL as postgres user
sudo -u postgres psql

# Or on Windows/Mac (if postgres user has no password):
psql -U postgres

# Create the database
CREATE DATABASE dating_matches;

# Exit psql
\q
```

Or use the quick setup script: `/home/user/POC_Dating/backend/setup-databases.sql`

### Optional Services (Not Required for Basic Functionality)
- **Redis 7+** - For advanced caching (uses simple in-memory cache by default)
- **RabbitMQ 3.11+** - For inter-service messaging (optional)
- **Docker** - For containerized deployment (optional)

## Technology Stack

- **Framework:** Spring Boot 3.2.0
- **Language:** Java 21
- **Database:** PostgreSQL (dating_matches)
- **Cache:** Redis
- **Messaging:** RabbitMQ
- **Security:** JWT (validated, not generated)
- **Inter-service Communication:** Spring Cloud OpenFeign
- **Build Tool:** Maven

## Responsibilities

### Swipe Recording
- Record user swipes (LIKE/PASS/SUPER_LIKE)
- Validate swipe eligibility
- Prevent duplicate swipes
- Track swipe history
- Detect mutual matches in real-time

### Match Management
- Create match records on mutual likes
- Retrieve user's active matches
- Unmatch users (soft delete)
- Provide match details with user profiles

### Recommendation Engine
- Generate personalized profile feed
- Exclude already-swiped users
- Exclude already-matched users
- Delegate preference filtering to user-service

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Match Service                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Controller Layer                                           │
│  ├── MatchController (REST endpoints)                       │
│                                                             │
│  Service Layer                                              │
│  ├── SwipeService (record swipes, detect matches)          │
│  ├── MatchService (manage matches)                          │
│  └── RecommendationService (generate feed)                  │
│                                                             │
│  Repository Layer                                           │
│  ├── SwipeRepository (JPA)                                  │
│  └── MatchRepository (JPA)                                  │
│                                                             │
│  Security Layer                                             │
│  ├── JwtTokenProvider (token validation)                    │
│  ├── JwtAuthenticationFilter (request filtering)            │
│  └── SecurityConfig (Spring Security)                       │
│                                                             │
│  External Communication                                     │
│  └── UserServiceClient (Feign - fetch user profiles)        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Database Schema

### Swipes Table
```sql
CREATE TABLE swipes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    swipe_type VARCHAR(20) NOT NULL,  -- LIKE, PASS, SUPER_LIKE
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, target_user_id)
);

CREATE INDEX idx_user_id ON swipes(user_id);
CREATE INDEX idx_target_user_id ON swipes(target_user_id);
CREATE INDEX idx_timestamp ON swipes(timestamp);
```

### Matches Table
```sql
CREATE TABLE matches (
    id BIGSERIAL PRIMARY KEY,
    user1_id BIGINT NOT NULL,      -- Always lower ID
    user2_id BIGINT NOT NULL,      -- Always higher ID
    matched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(user1_id, user2_id)
);

CREATE INDEX idx_user1_id ON matches(user1_id);
CREATE INDEX idx_user2_id ON matches(user2_id);
CREATE INDEX idx_matched_at ON matches(matched_at);
CREATE INDEX idx_is_active ON matches(is_active);
```

## API Endpoints

**Base URL:** `http://localhost:8082/api/matches`
**Via API Gateway:** `http://localhost:8080/api/matches`

All endpoints require JWT authentication via `Authorization: Bearer <token>` header.

### 1. Record a Swipe

**Endpoint:** `POST /api/matches/swipe`

**Description:** Record a swipe action (like/pass/super-like). Automatically detects mutual matches.

**Request:**
```json
{
  "targetUserId": 123,
  "swipeType": "LIKE"
}
```

**Response (200 OK):**
```json
{
  "swipeId": 456,
  "userId": 789,
  "targetUserId": 123,
  "swipeType": "LIKE",
  "timestamp": "2025-11-12T10:30:00",
  "isMatch": true,
  "matchId": 101
}
```

**Error Responses:**
- `400 Bad Request` - Invalid input (missing fields, invalid swipe type)
- `401 Unauthorized` - Missing or invalid JWT token
- `409 Conflict` - User already swiped on target user

**Matching Logic:**
1. User A likes User B
2. Check if User B previously liked User A
3. If yes: Create Match record with `(min(A,B), max(A,B))`
4. Return `isMatch=true` with `matchId`
5. Client shows "It's a Match!" celebration

---

### 2. Get Next Profile

**Endpoint:** `GET /api/matches/next-profile`

**Description:** Get the next profile for user to swipe on. Excludes already-swiped and matched users.

**Response (200 OK):**
```json
{
  "userId": 123,
  "name": "John Doe",
  "age": 28,
  "bio": "Adventure seeker and coffee enthusiast",
  "photoUrls": [
    "https://example.com/photos/user123_1.jpg",
    "https://example.com/photos/user123_2.jpg"
  ],
  "interests": ["Hiking", "Photography", "Travel"],
  "location": "San Francisco, CA",
  "distance": 5.2
}
```

**Response (204 No Content):** No more profiles available

**Error Responses:**
- `401 Unauthorized` - Missing or invalid JWT token
- `500 Internal Server Error` - Service error

---

### 3. Get Recommendations

**Endpoint:** `GET /api/matches/recommendations?limit=20`

**Description:** Get multiple profile recommendations at once.

**Query Parameters:**
- `limit` (optional, default=20) - Max number of profiles to return

**Response (200 OK):**
```json
[
  {
    "userId": 123,
    "name": "John Doe",
    "age": 28,
    "bio": "Adventure seeker",
    "photoUrls": ["..."],
    "interests": ["Hiking", "Travel"],
    "location": "San Francisco",
    "distance": 5.2
  },
  {
    "userId": 124,
    "name": "Jane Smith",
    "age": 26,
    "bio": "Artist and foodie",
    "photoUrls": ["..."],
    "interests": ["Art", "Cooking"],
    "location": "Oakland",
    "distance": 12.5
  }
]
```

---

### 4. Get My Matches

**Endpoint:** `GET /api/matches/my-matches`

**Description:** Get all active matches for authenticated user.

**Response (200 OK):**
```json
[
  {
    "matchId": 101,
    "matchedUser": {
      "userId": 123,
      "name": "John Doe",
      "age": 28,
      "bio": "Adventure seeker",
      "photoUrls": ["..."],
      "interests": ["Hiking", "Travel"],
      "location": "San Francisco",
      "distance": 5.2
    },
    "matchedAt": "2025-11-12T10:30:00",
    "isActive": true
  }
]
```

---

### 5. Get Match Details

**Endpoint:** `GET /api/matches/{matchId}`

**Description:** Get details of a specific match.

**Response (200 OK):**
```json
{
  "matchId": 101,
  "matchedUser": {
    "userId": 123,
    "name": "John Doe",
    "age": 28,
    "bio": "Adventure seeker",
    "photoUrls": ["..."],
    "interests": ["Hiking", "Travel"],
    "location": "San Francisco",
    "distance": 5.2
  },
  "matchedAt": "2025-11-12T10:30:00",
  "isActive": true
}
```

**Error Responses:**
- `404 Not Found` - Match doesn't exist or user not authorized

---

### 6. Unmatch

**Endpoint:** `DELETE /api/matches/{matchId}`

**Description:** Unmatch with another user (soft delete - sets `isActive=false`).

**Response (204 No Content):** Success

**Error Responses:**
- `404 Not Found` - Match doesn't exist or user not authorized

---

### 7. Who Liked Me (Premium Feature)

**Endpoint:** `GET /api/matches/who-liked-me`

**Description:** Get list of users who swiped right on you.

**Response (200 OK):**
```json
[
  {
    "userId": 125,
    "name": "Sarah Johnson",
    "age": 27,
    "bio": "Yoga instructor",
    "photoUrls": ["..."],
    "interests": ["Yoga", "Meditation"],
    "location": "Berkeley",
    "distance": 8.3
  }
]
```

---

## Matching Algorithm

### Real-Time Match Detection

**Flow:**
1. User A swipes right on User B
2. Save swipe to `swipes` table
3. Query: Does User B have a LIKE swipe on User A?
4. If YES:
   - Create Match record: `(min(A,B), max(A,B), now(), true)`
   - Return `isMatch=true` with `matchId`
   - TODO: Publish `match:created` event to RabbitMQ
5. If NO:
   - Return `isMatch=false`

**Why Ordered IDs (user1Id < user2Id)?**
- Ensures uniqueness: Prevents duplicate matches (A→B vs B→A)
- Simplifies queries: Only need to check one direction
- Standard pattern: Used by most dating apps

### Recommendation Algorithm (Basic POC)

**Exclusion Logic:**
1. Get all `targetUserId` where `userId = currentUser` (already swiped)
2. Get all `user1Id`, `user2Id` from active matches (already matched)
3. Add `currentUserId` (cannot swipe on self)
4. Send exclusion list to user-service
5. User-service applies preference filters and returns candidates

**Future Enhancements:**
- Elo-based scoring
- Machine learning recommendations
- Collaborative filtering
- Location-based ranking
- Interest similarity scoring

---

## Configuration

### application.yml

Key configurations in `/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: match-service

  server:
    port: 8082

  datasource:
    url: jdbc:postgresql://localhost:5432/dating_matches
    username: dating_user
    password: changeme123

  cloud:
    openfeign:
      client:
        config:
          user-service:
            url: http://localhost:8081/api

security:
  jwt:
    secret: change-me-with-a-secure-32-character-key-here
    expiration-ms: 86400000
```

**Environment Variables (Docker/Production):**
- `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`
- `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `JWT_SECRET` (must match user-service)
- `USER_SERVICE_URL`
- `REDIS_HOST`, `REDIS_PORT`
- `RABBITMQ_HOST`, `RABBITMQ_PORT`

---

## Security

### JWT Authentication

- **Validates** JWT tokens from user-service (does NOT generate)
- Extracts `userId` from token `subject` claim
- Sets authentication in `SecurityContextHolder`
- Controllers access user ID via `Authentication.getPrincipal()`

### Security Flow

1. Client sends request: `Authorization: Bearer <token>`
2. `JwtAuthenticationFilter` intercepts request
3. Extract token from header
4. Validate signature using shared secret
5. Extract user ID from claims
6. Set authentication in context
7. Controller receives authenticated user ID

### Public Endpoints

- `/actuator/health` - Health checks
- `/actuator/metrics` - Monitoring

All other endpoints require valid JWT token.

---

## Error Handling

| Status Code | Description                                  |
|-------------|----------------------------------------------|
| 200         | Success                                      |
| 204         | Success (no content)                         |
| 400         | Bad Request (invalid input)                  |
| 401         | Unauthorized (missing/invalid JWT)           |
| 404         | Not Found (match/user doesn't exist)         |
| 409         | Conflict (duplicate swipe)                   |
| 500         | Internal Server Error                        |

---

## Running the Service

### Run Locally with PostgreSQL (Default)

The service now uses PostgreSQL by default for persistence:

```bash
# 1. Ensure PostgreSQL is running and database is created (see Prerequisites above)

# 2. Start the service
cd backend/match-service
mvn spring-boot:run

# Optional: Set database password if different from default 'postgres'
DB_PASSWORD=your_password mvn spring-boot:run
```

That's it! The service will start on **port 8082** with:
- PostgreSQL database (localhost:5432/dating_matches)
- Auto-schema creation (DDL-auto: update)
- Simple in-memory cache (no Redis needed for basic functionality)
- No RabbitMQ required for basic functionality

**Access Points:**
- Service: http://localhost:8082/api
- Health Check: http://localhost:8082/actuator/health

**Configuration:**
- Database: `jdbc:postgresql://localhost:5432/dating_matches`
- Username: `postgres` (default)
- Password: Set via `DB_PASSWORD` environment variable (default: `postgres`)

**Testing the API:**
You'll need a JWT token from the user-service (running on port 8081) to test the match-service endpoints.

### Alternative: Run with H2 In-Memory Database

For quick testing without PostgreSQL, use the dev profile:

```bash
cd backend/match-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Access Points (H2 mode):**
- H2 Console: http://localhost:8082/h2-console
  - JDBC URL: `jdbc:h2:mem:dating_matches_dev`
  - Username: `sa`
  - Password: (leave empty)

**Note:** H2 data is stored in memory and will be lost when the service stops. PostgreSQL is recommended for persistent development.

### Running with Production Profile (PostgreSQL + Redis + RabbitMQ)

For production-like setup, you'll need Docker or manually installed services.

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL 14+ (for prod profile)
- Redis 6+ (for prod profile)
- RabbitMQ 3.11+ (for prod profile)

### Local Development with PostgreSQL

1. **Start PostgreSQL:**
```bash
docker run -d \
  --name postgres-matches \
  -e POSTGRES_DB=dating_matches \
  -e POSTGRES_USER=dating_user \
  -e POSTGRES_PASSWORD=changeme123 \
  -p 5432:5432 \
  postgres:14
```

2. **Start Redis:**
```bash
docker run -d \
  --name redis-matches \
  -p 6379:6379 \
  redis:7-alpine
```

3. **Build and Run with Production Profile:**
```bash
cd backend/match-service
mvn clean install

# Run with prod profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Or run the JAR directly with prod profile
java -jar target/match-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

4. **Verify:**
```bash
curl http://localhost:8082/actuator/health
```

### Docker Build

```bash
cd backend/match-service
docker build -t match-service:latest .
docker run -p 8082:8082 \
  -e POSTGRES_HOST=postgres \
  -e JWT_SECRET=your-secret-key \
  -e USER_SERVICE_URL=http://user-service:8081/api \
  match-service:latest
```

---

## Testing

### Unit Tests

Run with Maven:
```bash
mvn test
```

### Integration Tests

Requires TestContainers:
```bash
mvn verify
```

### Manual API Testing

1. **Register/Login** via user-service to get JWT token
2. **Use token** in Authorization header for all match-service calls

**Example with curl:**
```bash
# Get token from user-service
TOKEN=$(curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}' \
  | jq -r '.token')

# Record a swipe
curl -X POST http://localhost:8082/api/matches/swipe \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetUserId":123,"swipeType":"LIKE"}'

# Get matches
curl http://localhost:8082/api/matches/my-matches \
  -H "Authorization: Bearer $TOKEN"
```

---

## Future Enhancements

### Short-term
- [ ] Implement event publishing (RabbitMQ)
- [ ] Add Redis caching for matches
- [ ] Implement swipe rate limiting
- [ ] Add match expiration (24-48 hours to message)

### Long-term
- [ ] Machine learning recommendations
- [ ] Elo-based user scoring
- [ ] Collaborative filtering
- [ ] Location-based matching
- [ ] Super Like premium feature
- [ ] Undo swipe functionality
- [ ] Boost profile visibility

---

## Troubleshooting

### Issue: 401 Unauthorized
- Check JWT token is valid
- Verify `JWT_SECRET` matches user-service
- Check token expiration

### Issue: 500 Internal Server Error on swipe
- Verify user-service is running
- Check Feign client configuration
- Review logs: `logs/match-service.log`

### Issue: No recommendations returned
- Check if user has swiped on all available users
- Verify user-service has active users
- Check exclusion logic in logs

---

## Monitoring

### Health Check
```bash
curl http://localhost:8082/actuator/health
```

### Metrics
```bash
curl http://localhost:8082/actuator/metrics
```

### Logs
```bash
tail -f logs/match-service.log
```

---

## Support

For issues or questions:
1. Check logs in `logs/match-service.log`
2. Review this README
3. Check user-service integration
4. Verify database connectivity
