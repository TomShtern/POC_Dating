# User Service

## Overview

Microservice responsible for user authentication, profile management, and preference handling.

## Port
**8081** (internal, accessed via API Gateway on 8080)

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
CREATE DATABASE dating_users;

# Exit psql
\q
```

Or use the quick setup script: `/home/user/POC_Dating/backend/setup-databases.sql`

### Optional Services (Not Required for Basic Functionality)
- **Redis 7+** - For advanced caching (uses simple in-memory cache by default)
- **RabbitMQ 3.11+** - For inter-service messaging (optional)
- **Docker** - For containerized deployment (optional)

## Quick Start

### Run Locally with PostgreSQL (Default)

The service now uses PostgreSQL by default for persistence:

```bash
# 1. Ensure PostgreSQL is running and database is created (see Prerequisites above)

# 2. Start the service
cd backend/user-service
mvn spring-boot:run

# Optional: Set database password if different from default 'postgres'
DB_PASSWORD=your_password mvn spring-boot:run
```

That's it! The service will start on **port 8081** with:
- PostgreSQL database (localhost:5432/dating_users)
- Auto-schema creation (DDL-auto: update)
- Simple in-memory cache (no Redis needed for basic functionality)
- No RabbitMQ required for basic functionality

**Access Points:**
- Service: http://localhost:8081/api
- Health Check: http://localhost:8081/actuator/health

**Configuration:**
- Database: `jdbc:postgresql://localhost:5432/dating_users`
- Username: `postgres` (default)
- Password: Set via `DB_PASSWORD` environment variable (default: `postgres`)

### Alternative: Run with H2 In-Memory Database

For quick testing without PostgreSQL, use the dev profile:

```bash
cd backend/user-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Access Points (H2 mode):**
- H2 Console: http://localhost:8081/h2-console
  - JDBC URL: `jdbc:h2:mem:dating_users_dev`
  - Username: `sa`
  - Password: (leave empty)

**Note:** H2 data is stored in memory and will be lost when the service stops. PostgreSQL is recommended for persistent development.

### Running with Production Profile (PostgreSQL + Redis + RabbitMQ)

For production-like setup, you'll need Docker or manually installed services.

### Prerequisites
- Java 21+
- Maven 3.8+
- PostgreSQL 14+ (for prod profile)
- Redis 7+ (for prod profile)
- RabbitMQ (for prod profile)
- Docker (optional, for containerized deployment)

### Running Locally with PostgreSQL

1. **Configure Database:**
   Ensure PostgreSQL is running and create the database:
   ```bash
   createdb dating_users
   ```

2. **Configure Environment Variables:**
   Create a `.env` file or export variables:
   ```bash
   export POSTGRES_HOST=localhost
   export POSTGRES_PORT=5432
   export POSTGRES_DB=dating_users
   export POSTGRES_USER=postgres
   export POSTGRES_PASSWORD=yourpassword
   export JWT_SECRET=your-secret-key-at-least-32-characters-long
   export REDIS_HOST=localhost
   export REDIS_PORT=6379
   ```

3. **Build and Run with Production Profile:**
   ```bash
   # From the user-service directory
   mvn clean install

   # Run with prod profile
   mvn spring-boot:run -Dspring-boot.run.profiles=prod

   # Or run the JAR directly with prod profile
   java -jar target/user-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
   ```

4. **Verify Service is Running:**
   ```bash
   curl http://localhost:8081/actuator/health
   ```

### Running with Docker

1. **Build Docker Image:**
   ```bash
   # From the user-service directory
   docker build -t user-service:latest .
   ```

2. **Run Container:**
   ```bash
   docker run -d \
     --name user-service \
     -p 8081:8081 \
     -e POSTGRES_HOST=host.docker.internal \
     -e POSTGRES_DB=dating_users \
     -e POSTGRES_USER=postgres \
     -e POSTGRES_PASSWORD=yourpassword \
     -e JWT_SECRET=your-secret-key-at-least-32-characters-long \
     -e REDIS_HOST=host.docker.internal \
     user-service:latest
   ```

### Testing the API

Register a new user:
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!",
    "firstName": "Test",
    "lastName": "User",
    "age": 25,
    "gender": "male"
  }'
```

Login:
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!"
  }'
```

## Responsibilities

### Authentication
- User registration
- Login and JWT token generation
- Token refresh
- Token validation
- Logout (token blacklist in Redis)

### Profile Management
- User profile CRUD
- Profile picture upload/update
- Biography, age, location management
- Account status management (active, suspended, deleted)

### Preferences
- User matching preferences (age range, distance, interests)
- Notification preferences
- Privacy settings

## Database Schema

```
users
├── id (UUID, PK)
├── email (UNIQUE)
├── username (UNIQUE)
├── password_hash
├── first_name
├── last_name
├── date_of_birth
├── gender
├── bio
├── status (ACTIVE, SUSPENDED, DELETED)
├── created_at
├── updated_at
└── last_login

user_preferences
├── id (UUID, PK)
├── user_id (FK)
├── min_age
├── max_age
├── max_distance_km
├── interested_in (MALE, FEMALE, BOTH)
├── interests (JSON array)
└── updated_at

refresh_tokens
├── id (UUID, PK)
├── user_id (FK)
├── token_hash
├── expires_at
├── created_at
└── revoked (boolean)
```

## API Endpoints

**Base URL:** http://localhost:8080/api (via API Gateway) or http://localhost:8081/api (direct)

### Authentication Endpoints

#### POST /api/auth/register
Register a new user account.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "age": 25,
  "gender": "male",
  "bio": "Hello! Looking forward to meeting new people.",
  "location": "New York, NY"
}
```

**Response (201 Created):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

#### POST /api/auth/login
Login with existing credentials.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

#### POST /api/auth/refresh
Refresh access token using refresh token.

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

#### POST /api/auth/logout
Logout and revoke refresh token.

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response (200 OK):**
```json
"Logged out successfully"
```

### User Management Endpoints

All user management endpoints require authentication via JWT token in the Authorization header:
```
Authorization: Bearer <access_token>
```

#### GET /api/users/me
Get current authenticated user's profile.

**Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "age": 25,
  "gender": "male",
  "bio": "Hello! Looking forward to meeting new people.",
  "photoUrl": "https://example.com/photos/user.jpg",
  "location": "New York, NY",
  "preferences": "{\"ageRange\": [22, 35], \"maxDistance\": 50}",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00",
  "lastLogin": "2024-01-20T14:22:00",
  "status": "ACTIVE"
}
```

#### GET /api/users/{userId}
Get user profile by ID.

**Response (200 OK):** Same as above

#### GET /api/users/{userId}/summary
Get lightweight user summary (for other services).

**Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "firstName": "John",
  "lastName": "Doe",
  "age": 25,
  "gender": "male",
  "photoUrl": "https://example.com/photos/user.jpg",
  "location": "New York, NY"
}
```

#### PUT /api/users/me
Update current user's profile.

**Request Body (all fields optional):**
```json
{
  "firstName": "John",
  "lastName": "Smith",
  "age": 26,
  "gender": "male",
  "bio": "Updated bio text",
  "photoUrl": "https://example.com/photos/new-photo.jpg",
  "location": "Los Angeles, CA",
  "preferences": "{\"ageRange\": [23, 40], \"maxDistance\": 100}"
}
```

**Response (200 OK):** Updated user profile (same format as GET /me)

#### PUT /api/users/{userId}
Update user profile by ID (admin use case).

**Request Body:** Same as PUT /api/users/me

**Response (200 OK):** Updated user profile

#### DELETE /api/users/me
Delete current user's account (soft delete).

**Response (200 OK):**
```json
"Account deleted successfully"
```

#### DELETE /api/users/{userId}
Delete user account by ID (admin use case).

**Response (200 OK):**
```json
"Account deleted successfully"
```

## Events Published

### Via RabbitMQ
```
user:registered
├── Payload: UserId, Email, CreatedAt
├── Purpose: Notify other services of new user
└── Consumed by: Match Service, Recommendation Service

user:profile-updated
├── Payload: UserId, UpdatedFields, Timestamp
├── Purpose: Trigger recommendation recalculation
└── Consumed by: Recommendation Service

user:preferences-updated
├── Payload: UserId, PreferencesJSON, Timestamp
├── Purpose: Update matching algorithm
└── Consumed by: Match Service

user:deleted
├── Payload: UserId, DeletedAt
├── Purpose: Cleanup user data
└── Consumed by: All services
```

## Events Consumed

```
match:found
├── Source: Match Service
├── Action: Increment user's match counter
└── Storage: Cache in Redis for quick access
```

## Security

- **JWT Authentication**: Access tokens (15 min), Refresh tokens (7 days)
- **Password Hashing**: BCrypt with salt
- **Rate Limiting**: Prevent brute force attacks
- **Email Verification**: Confirm ownership on registration
- **Token Blacklist**: Redis-backed for fast logout

## Caching Strategy

```
Redis Keys:
├── user:{userId}:profile     → Full user profile (TTL: 1 hour)
├── user:{userId}:preferences → User preferences (TTL: 1 hour)
├── user:{email}:id           → Email to ID mapping (TTL: 1 day)
├── user:{username}:id        → Username to ID mapping (TTL: 1 day)
└── blacklist:token:{tokenHash} → Revoked tokens (TTL: token expiry)
```

## Error Handling

All errors follow a consistent response format:

### Error Response Structure
```json
{
  "timestamp": "2024-01-20T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register",
  "validationErrors": {
    "email": "Email should be valid",
    "password": "Password must be at least 8 characters"
  }
}
```

### HTTP Status Codes

| Status Code | Description | Example |
|-------------|-------------|---------|
| 400 Bad Request | Invalid input, validation failure | Missing required fields, invalid email format |
| 401 Unauthorized | Missing or invalid authentication token | No JWT token, expired token |
| 403 Forbidden | User account suspended or deleted | Account status is not ACTIVE |
| 404 Not Found | Requested resource not found | User ID does not exist |
| 409 Conflict | Resource already exists | Email already registered |
| 500 Internal Server Error | Unexpected server error | Database connection failure |

### Example Error Responses

**Validation Error (400):**
```json
{
  "timestamp": "2024-01-20T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register",
  "validationErrors": {
    "age": "Age must be at least 18"
  }
}
```

**Authentication Error (401):**
```json
{
  "timestamp": "2024-01-20T14:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password",
  "path": "/api/auth/login"
}
```

**Duplicate Resource (409):**
```json
{
  "timestamp": "2024-01-20T14:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "User already exists with email: 'user@example.com'",
  "path": "/api/auth/register"
}
```

**Not Found (404):**
```json
{
  "timestamp": "2024-01-20T14:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: '123e4567-e89b-12d3-a456-426614174000'",
  "path": "/api/users/123e4567-e89b-12d3-a456-426614174000"
}
```

## Testing

### Unit Tests
- Controller layer: Request validation, response formatting
- Service layer: Business logic, preference calculations
- Security: JWT generation/validation, password hashing

### Integration Tests
- Database: CRUD operations with TestContainers
- Redis: Token storage/retrieval
- RabbitMQ: Event publishing

### Test Data
- Multiple user profiles with varying preferences
- Invalid input scenarios
- Security test cases

## Future Enhancements

- Email verification
- Two-factor authentication
- Social login (Google, Facebook)
- Profile analytics
- Activity logging
- User blocking/reporting
- Privacy settings enhancement
