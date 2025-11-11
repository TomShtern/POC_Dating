# User Service

## Overview

Microservice responsible for user authentication, profile management, and preference handling.

## Port
**8081** (internal, accessed via API Gateway on 8080)

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

**Base URL:** http://localhost:8080/api/users

### Authentication
```
POST   /auth/register          → Register new user
POST   /auth/login             → Login, get JWT token
POST   /auth/refresh           → Refresh JWT token
POST   /auth/logout            → Logout, revoke token
POST   /auth/forgot-password   → Request password reset
POST   /auth/reset-password    → Reset password
```

### User Management
```
GET    /{id}                   → Get user profile
PUT    /{id}                   → Update user profile
DELETE /{id}                   → Delete account
GET    /{id}/preferences       → Get user preferences
PUT    /{id}/preferences       → Update preferences
GET    /search                 → Search users by criteria
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

```
400 Bad Request      → Invalid input, validation failure
401 Unauthorized     → Missing/invalid token
403 Forbidden        → User suspended/deleted
404 Not Found        → User not found
409 Conflict         → Email/username already exists
429 Too Many Requests→ Rate limit exceeded
500 Internal Server  → Database or server error
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
