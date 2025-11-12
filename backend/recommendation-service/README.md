# Recommendation Service

## Overview

Microservice for generating personalized recommendations for users based on their preferences, behavior, and demographics. This service implements a simple but extensible recommendation algorithm that can be enhanced with machine learning in the future.

## Port
**8084** (internal, accessed via API Gateway)

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
CREATE DATABASE dating_recommendations;

# Exit psql
\q
```

Or use the quick setup script: `/home/user/POC_Dating/backend/setup-databases.sql`

### Optional Services (Not Required for Basic Functionality)
- **Redis 7+** - For recommendation caching (optional)
- **RabbitMQ 3.11+** - For event-driven updates (optional)
- **Docker** - For containerized deployment (optional)

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 21
- **Database**: PostgreSQL (dating_recommendations)
- **Security**: JWT-based authentication
- **Inter-service Communication**: Spring Cloud OpenFeign
- **Caching**: Redis
- **Message Broker**: RabbitMQ

## Key Features

### 1. User Preference Management
- Create, read, update, and delete user matching preferences
- Age range filtering (min/max age)
- Gender preferences
- Maximum distance preferences
- Interest-based matching
- Flexible matching options

### 2. Recommendation Generation
- Real-time recommendation generation
- Multi-factor scoring algorithm
- Exclusion of already swiped/matched users
- Configurable algorithm weights
- Score transparency (breakdown provided)

### 3. Integration with Other Services
- **User Service**: Fetch user profiles and demographics via Feign client
- **Match Service**: Get swipe history to exclude already swiped users

## Recommendation Algorithm

### Algorithm Overview

The recommendation engine uses a weighted scoring system based on three key factors:

```
Total Score = (Age Compatibility × 0.3) +
              (Distance Proximity × 0.4) +
              (Common Interests × 0.3)
```

Each component score ranges from 0-100, resulting in a final score of 0-100.

### 1. Age Compatibility Score (Weight: 0.3)

Calculates how well the candidate's age matches the user's preferences and actual age.

**Algorithm:**
- Perfect match (same age or within 2 years): 100 points
- Score decreases linearly as age difference increases
- Formula: `100 - (ageDifference × 100 / maxAgeDifference)`
- Missing age data: 50 points (neutral)

**Example:**
```
User age: 28
Candidate age: 26
Age difference: 2 years
Score: 100 (perfect match)
```

### 2. Distance Proximity Score (Weight: 0.4)

Calculates how close the candidate is geographically using the Haversine formula.

**Algorithm:**
- Perfect match (within 5 km): 100 points
- Score decreases linearly as distance increases
- Formula: `100 - (distanceKm × 100 / maxDistance)`
- Filters out users beyond maxDistance (unless flexible distance enabled)
- Missing location data: 50 points (neutral)

**Example:**
```
User location: (40.7128° N, 74.0060° W)
Candidate location: (40.7489° N, 73.9680° W)
Distance: ~4.5 km
Max distance preference: 50 km
Score: 100 (within perfect range)
```

**Haversine Formula:**
```java
EARTH_RADIUS = 6371 km

distance = 2 × EARTH_RADIUS × arcsin(√(
    sin²((lat2 - lat1) / 2) +
    cos(lat1) × cos(lat2) × sin²((lon2 - lon1) / 2)
))
```

### 3. Common Interests Score (Weight: 0.3)

Calculates the overlap between user and candidate interests.

**Algorithm:**
- Perfect match: 100 points (if all user interests are shared)
- Formula: `(commonInterests / totalUserInterests) × 100`
- No interests or no overlap: 0 points
- Missing interest data: 50 points (neutral)

**Example:**
```
User interests: [hiking, photography, cooking, travel]
Candidate interests: [hiking, cooking, yoga, travel]
Common interests: [hiking, cooking, travel] = 3
Score: (3 / 4) × 100 = 75
```

### Filtering Rules

Before scoring, candidates are filtered based on:

1. **Active users only** - Inactive accounts are excluded
2. **Gender preference** - If not "ANY", only matching genders are included
3. **Age range** - Unless flexible age range is enabled
4. **Already swiped/matched** - Excluded from recommendations
5. **Self-exclusion** - User cannot be recommended themselves

### Configuration

Algorithm weights and limits can be configured in `application.yml`:

```yaml
recommendation:
  algorithm:
    max-results: 50
    max-distance-km: 100
    score-weights:
      age-compatibility: 0.3
      distance-proximity: 0.4
      common-interests: 0.3
    cache-ttl-minutes: 30
```

## Database Schema

### user_preferences
Stores user matching preferences.

```sql
CREATE TABLE user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    min_age INTEGER NOT NULL,
    max_age INTEGER NOT NULL,
    preferred_gender VARCHAR(50) NOT NULL,
    max_distance INTEGER NOT NULL,
    flexible_age_range BOOLEAN DEFAULT FALSE,
    flexible_distance BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE user_preference_interests (
    preference_id BIGINT NOT NULL,
    interest VARCHAR(255),
    FOREIGN KEY (preference_id) REFERENCES user_preferences(id)
);
```

### recommendation_scores (Optional Cache)
Caches computed recommendation scores for performance optimization.

```sql
CREATE TABLE recommendation_scores (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    candidate_user_id BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    score_breakdown TEXT,
    shown BOOLEAN DEFAULT FALSE,
    shown_at TIMESTAMP,
    valid BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    INDEX idx_user_candidate (user_id, candidate_user_id),
    INDEX idx_user_score (user_id, score)
);
```

## API Endpoints

### Recommendation Endpoints

#### GET /api/recommendations
Get all recommendations for the authenticated user.

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Response:** `200 OK`
```json
[
  {
    "userId": 123,
    "username": "john_doe",
    "firstName": "John",
    "lastName": "Doe",
    "age": 28,
    "gender": "MALE",
    "bio": "Love hiking and photography",
    "photos": ["photo1.jpg", "photo2.jpg"],
    "interests": ["hiking", "photography", "travel"],
    "latitude": 40.7128,
    "longitude": -74.0060,
    "city": "New York",
    "country": "USA",
    "matchScore": 85.5,
    "distanceKm": 4.2,
    "scoreBreakdown": {
      "ageCompatibilityScore": 90.0,
      "distanceProximityScore": 95.0,
      "commonInterestsScore": 75.0,
      "commonInterestCount": 2
    }
  }
]
```

#### GET /api/recommendations/next
Get the next recommendation (single profile).

**Response:** `200 OK` (single profile) or `204 No Content` (no recommendations available)

#### POST /api/recommendations/refresh
Refresh recommendations for the authenticated user.

**Response:** `200 OK` (returns fresh recommendations)

#### GET /api/recommendations/stats
Get recommendation statistics.

**Response:** `200 OK`
```json
{
  "totalRecommendations": 42,
  "averageScore": 72.5,
  "topScore": 95.3,
  "hasPreferences": true
}
```

### Preference Endpoints

#### GET /api/preferences
Get current user's preferences.

**Response:** `200 OK`
```json
{
  "id": 1,
  "userId": 456,
  "minAge": 25,
  "maxAge": 35,
  "preferredGender": "FEMALE",
  "maxDistance": 50,
  "interests": ["hiking", "cooking", "travel"],
  "flexibleAgeRange": false,
  "flexibleDistance": true,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-15T14:30:00"
}
```

#### PUT /api/preferences
Update current user's preferences.

**Request Body:**
```json
{
  "minAge": 25,
  "maxAge": 35,
  "preferredGender": "FEMALE",
  "maxDistance": 50,
  "interests": ["hiking", "cooking", "travel"],
  "flexibleAgeRange": false,
  "flexibleDistance": true
}
```

**Validation:**
- minAge: 18-100
- maxAge: 18-100
- maxAge must be >= minAge
- maxDistance: 1-500 km
- preferredGender: required

**Response:** `200 OK` (returns updated preferences)

#### POST /api/preferences
Create default preferences for the user.

**Response:** `201 Created` (returns default preferences)

Default values:
- minAge: 18
- maxAge: 99
- preferredGender: "ANY"
- maxDistance: 50
- flexibleAgeRange: true
- flexibleDistance: true

#### DELETE /api/preferences
Delete current user's preferences.

**Response:** `204 No Content`

#### GET /api/preferences/exists
Check if user has preferences.

**Response:** `200 OK`
```json
true
```

## Inter-Service Communication

### Feign Clients

#### UserServiceClient
Communicates with User Service to fetch user profiles.

**Methods:**
- `getUserById(userId)` - Get single user profile
- `getUsersByIds(userIds)` - Get multiple user profiles
- `getActiveUsers(excludeUserId, gender, minAge, maxAge)` - Get all active users with filters

#### MatchServiceClient
Communicates with Match Service to get swipe/match history.

**Methods:**
- `getSwipedUserIds(userId)` - Get all swiped user IDs
- `getMatchedUserIds(userId)` - Get all matched user IDs

### Feign Configuration

- **Connection timeout**: 5000ms
- **Read timeout**: 5000ms
- **Logger level**: FULL (for debugging)
- **JWT token forwarding**: Automatic via RequestInterceptor

## Security

### JWT Authentication

All endpoints (except actuator) require JWT authentication.

**Token Validation:**
- JWT tokens are validated using the shared secret key
- User ID is extracted from the token subject
- Authentication is set in Spring Security context

**Configuration:**
```yaml
jwt:
  secret: your-256-bit-secret-key-here
```

**Note:** The JWT secret must be the same across all services.

## Error Handling

| HTTP Status | Description |
|-------------|-------------|
| 200 OK | Request successful |
| 201 Created | Resource created successfully |
| 204 No Content | Request successful, no content to return |
| 400 Bad Request | Invalid request parameters or validation error |
| 401 Unauthorized | Missing or invalid JWT token |
| 404 Not Found | Resource not found |
| 500 Internal Server Error | Server error or downstream service failure |

## Performance Considerations

### Caching Strategy

1. **Redis Caching** - Cache user preferences and recommendation scores
2. **Score Cache** - Optional caching of computed scores in database
3. **TTL** - Configurable cache expiration (default: 30 minutes)

### Optimization Techniques

1. **Batch Processing** - Fetch multiple user profiles in one call
2. **Lazy Loading** - Only fetch full profiles for top N candidates
3. **Score Invalidation** - Invalidate cached scores when preferences change
4. **Database Indexing** - Indexes on user_id, candidate_user_id, and score

### Scalability

- **Stateless Service** - Can be horizontally scaled
- **Database Connection Pool** - Configurable (default: max 10 connections)
- **Async Processing** - Future enhancement for background score computation

## Testing

### Unit Tests
- Algorithm scoring logic
- Distance calculation (Haversine formula)
- Interest overlap calculation
- Preference filtering

### Integration Tests
- Feign client integration
- Database operations
- JWT authentication
- End-to-end API testing

## Build and Run

### Run Locally with PostgreSQL (Default)

The service now uses PostgreSQL by default for persistence:

```bash
# 1. Ensure PostgreSQL is running and database is created (see Prerequisites above)

# 2. Start the service
cd backend/recommendation-service
mvn spring-boot:run

# Optional: Set database password if different from default 'postgres'
DB_PASSWORD=your_password mvn spring-boot:run
```

That's it! The service will start on **port 8084** with:
- PostgreSQL database (localhost:5432/dating_recommendations)
- Auto-schema creation (DDL-auto: update)
- No Redis required for basic functionality (optional for caching)
- No RabbitMQ required for basic functionality (optional for event publishing)

**Access Points:**
- Service: http://localhost:8084/api
- Health Check: http://localhost:8084/actuator/health

**Configuration:**
- Database: `jdbc:postgresql://localhost:5432/dating_recommendations`
- Username: `postgres` (default)
- Password: Set via `DB_PASSWORD` environment variable (default: `postgres`)

**Testing the API:**
You'll need:
1. JWT token from user-service (port 8081)
2. The service communicates with user-service and match-service via Feign clients

### Alternative: Run with H2 In-Memory Database

For quick testing without PostgreSQL, use the dev profile:

```bash
cd backend/recommendation-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Access Points (H2 mode):**
- H2 Console: http://localhost:8084/h2-console
  - JDBC URL: `jdbc:h2:mem:dating_recommendations_dev`
  - Username: `sa`
  - Password: (leave empty)

**Note:** H2 data is stored in memory and will be lost when the service stops. PostgreSQL is recommended for persistent user preferences.

### Running with Production Profile (PostgreSQL + Redis + RabbitMQ)

For production-like setup, you'll need Docker or manually installed services.

### Build with Maven
```bash
cd backend/recommendation-service
mvn clean install
```

### Run with Production Profile
```bash
# Run with prod profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Or run the JAR directly with prod profile
java -jar target/recommendation-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

### Run with Docker
```bash
docker build -t recommendation-service .
docker run -p 8084:8084 recommendation-service
```

### Environment Variables (Production)
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/dating_recommendations
SPRING_DATASOURCE_USERNAME=dating_user
SPRING_DATASOURCE_PASSWORD=dating_password
JWT_SECRET=your-secret-key
FEIGN_USER_SERVICE_URL=http://localhost:8081
FEIGN_MATCH_SERVICE_URL=http://localhost:8082
REDIS_HOST=localhost
REDIS_PORT=6379
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
```

## Health Checks

### Actuator Endpoints
- `/actuator/health` - Service health status
- `/actuator/info` - Service information
- `/actuator/metrics` - Service metrics
- `/actuator/prometheus` - Prometheus metrics

## Future Enhancements

### Algorithm Improvements
- **Collaborative Filtering** - "Users like you also liked..."
- **Machine Learning** - Neural network-based scoring
- **Reinforcement Learning** - Learn from user feedback
- **Time-based Patterns** - Recommend when user is most active

### Features
- **A/B Testing** - Test different algorithms
- **Diversity** - Avoid echo chamber effect
- **Serendipity** - Occasional random suggestions
- **Re-ranking** - Real-time score adjustment based on engagement
- **Cold Start** - Better handling of new users with limited data

### Performance
- **Background Processing** - Pre-compute recommendations nightly
- **Apache Spark** - Distributed computation for large datasets
- **GraphQL** - More flexible API queries
- **Websockets** - Real-time recommendation updates

## Contributing

See the main project README for contribution guidelines.

## License

See the main project README for license information.
