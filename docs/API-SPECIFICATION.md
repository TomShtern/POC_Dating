# API Specification - POC Dating Application

## Document Overview

Complete REST API specification for POC Dating application.

**Base URL:** http://localhost:8080/api
**API Version:** 1.0
**Authentication:** JWT Bearer Token
**Content-Type:** application/json

---

## Table of Contents

1. [Authentication Endpoints](#authentication-endpoints)
2. [User Management](#user-management)
3. [Matching & Swipes](#matching--swipes)
4. [Chat & Messaging](#chat--messaging)
5. [Recommendations](#recommendations)
6. [Error Handling](#error-handling)
7. [Response Formats](#response-formats)

---

## Authentication Endpoints

### Register User

```
POST /users/auth/register

Request Body:
{
  "email": "user@example.com",
  "username": "johndoe",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE"
}

Response (201 Created):
{
  "userId": "uuid-1234",
  "email": "user@example.com",
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 900
}
```

### Login

```
POST /users/auth/login

Request Body:
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}

Response (200 OK):
{
  "userId": "uuid-1234",
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 900
}
```

### Refresh Token

```
POST /users/auth/refresh

Request Body:
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 900
}
```

### Logout

```
POST /users/auth/logout

Headers:
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...

Response (200 OK):
{
  "message": "Logged out successfully"
}
```

---

## User Management

### Get User Profile

```
GET /users/{userId}

Headers:
Authorization: Bearer {token}

Response (200 OK):
{
  "id": "uuid-1234",
  "email": "user@example.com",
  "username": "johndoe",
  "firstName": "John",
  "lastName": "Doe",
  "age": 34,
  "gender": "MALE",
  "bio": "Adventure seeker, love hiking",
  "profilePictureUrl": "https://...",
  "status": "ACTIVE",
  "createdAt": "2025-11-01T10:00:00Z"
}
```

### Update User Profile

```
PUT /users/{userId}

Headers:
Authorization: Bearer {token}

Request Body:
{
  "firstName": "John",
  "lastName": "Doe",
  "bio": "Updated bio",
  "profilePictureUrl": "https://..."
}

Response (200 OK):
{
  "id": "uuid-1234",
  "email": "user@example.com",
  ...
  "updatedAt": "2025-11-11T10:00:00Z"
}
```

### Get User Preferences

```
GET /users/{userId}/preferences

Headers:
Authorization: Bearer {token}

Response (200 OK):
{
  "id": "uuid-5678",
  "userId": "uuid-1234",
  "minAge": 25,
  "maxAge": 40,
  "maxDistanceKm": 50,
  "interestedIn": "FEMALE",
  "interests": ["hiking", "cooking", "travel"],
  "updatedAt": "2025-11-01T10:00:00Z"
}
```

### Update User Preferences

```
PUT /users/{userId}/preferences

Headers:
Authorization: Bearer {token}

Request Body:
{
  "minAge": 25,
  "maxAge": 40,
  "maxDistanceKm": 50,
  "interestedIn": "FEMALE",
  "interests": ["hiking", "cooking", "travel"]
}

Response (200 OK):
{
  "id": "uuid-5678",
  ...
  "updatedAt": "2025-11-11T10:00:00Z"
}
```

### Delete Account

```
DELETE /users/{userId}

Headers:
Authorization: Bearer {token}

Response (204 No Content)
```

---

## Matching & Swipes

### Get Swiping Feed

```
GET /matches/feed/{userId}?limit=10&offset=0

Headers:
Authorization: Bearer {token}

Response (200 OK):
{
  "feed": [
    {
      "id": "uuid-user-2",
      "name": "Jane Smith",
      "age": 28,
      "profilePictureUrl": "https://...",
      "bio": "Love outdoor activities",
      "compatibilityScore": 85
    },
    {
      "id": "uuid-user-3",
      "name": "Emily Johnson",
      ...
    }
  ],
  "total": 245,
  "hasMore": true
}
```

### Record a Swipe

```
POST /matches/swipes

Headers:
Authorization: Bearer {token}

Request Body:
{
  "targetUserId": "uuid-user-2",
  "action": "LIKE"  // LIKE, PASS, SUPER_LIKE
}

Response (201 Created):
{
  "id": "uuid-swipe-1",
  "userId": "uuid-1234",
  "targetUserId": "uuid-user-2",
  "action": "LIKE",
  "isMatch": false,  // true if mutual match
  "createdAt": "2025-11-11T10:00:00Z"
}

Response (200 OK) if MATCH DETECTED:
{
  "id": "uuid-swipe-1",
  ...
  "isMatch": true,
  "matchId": "uuid-match-1",
  "matchedAt": "2025-11-11T10:00:00Z"
}
```

### Get All Matches

```
GET /matches?limit=20&offset=0

Headers:
Authorization: Bearer {token}

Response (200 OK):
{
  "matches": [
    {
      "id": "uuid-match-1",
      "matchedUser": {
        "id": "uuid-user-2",
        "name": "Jane Smith",
        "profilePictureUrl": "https://...",
        "lastMessage": "Hey! How are you?",
        "lastMessageTime": "2025-11-11T09:45:00Z",
        "unreadCount": 3
      },
      "matchedAt": "2025-11-10T15:30:00Z"
    }
  ],
  "total": 12,
  "hasMore": false
}
```

### Get Match Details

```
GET /matches/{matchId}

Headers:
Authorization: Bearer {token}

Response (200 OK):
{
  "id": "uuid-match-1",
  "user1": {
    "id": "uuid-1234",
    "name": "John Doe"
  },
  "user2": {
    "id": "uuid-user-2",
    "name": "Jane Smith"
  },
  "matchScore": 85,
  "scoreFactors": {
    "interestMatch": 40,
    "ageCompatibility": 30,
    "preferenceAlignment": 15
  },
  "matchedAt": "2025-11-10T15:30:00Z"
}
```

### Unmatch

```
DELETE /matches/{matchId}

Headers:
Authorization: Bearer {token}

Response (204 No Content)
```

---

## Chat & Messaging

### WebSocket Connection

```
Connection: ws://localhost:8080/api/chat/ws

Headers:
Authorization: Bearer {token}

Initial Message (Server → Client):
{
  "type": "CONNECT_SUCCESS",
  "userId": "uuid-1234",
  "conversations": ["uuid-match-1", "uuid-match-2"]
}
```

### Send Message (WebSocket)

```
Client → Server:
{
  "type": "SEND_MESSAGE",
  "conversationId": "uuid-match-1",
  "content": "Hi there!",
  "timestamp": "2025-11-11T10:00:00Z"
}

Server → Recipient Client:
{
  "type": "MESSAGE_RECEIVED",
  "id": "uuid-msg-1",
  "conversationId": "uuid-match-1",
  "senderId": "uuid-1234",
  "senderName": "John Doe",
  "content": "Hi there!",
  "timestamp": "2025-11-11T10:00:00Z"
}
```

### Get Conversation Messages (REST)

```
GET /chat/conversations/{conversationId}/messages?limit=50&offset=0

Headers:
Authorization: Bearer {token}

Response (200 OK):
{
  "conversationId": "uuid-match-1",
  "messages": [
    {
      "id": "uuid-msg-1",
      "senderId": "uuid-1234",
      "senderName": "John Doe",
      "content": "Hi there!",
      "status": "READ",  // SENT, DELIVERED, READ
      "createdAt": "2025-11-11T10:00:00Z",
      "readAt": "2025-11-11T10:01:00Z"
    },
    {
      "id": "uuid-msg-2",
      "senderId": "uuid-user-2",
      ...
    }
  ],
  "total": 127,
  "hasMore": true
}
```

### Mark as Read (WebSocket)

```
Client → Server:
{
  "type": "MARK_AS_READ",
  "conversationId": "uuid-match-1",
  "messageIds": ["uuid-msg-1", "uuid-msg-2"]
}

Server → Sender Client:
{
  "type": "MESSAGE_READ",
  "conversationId": "uuid-match-1",
  "messageIds": ["uuid-msg-1", "uuid-msg-2"],
  "readAt": "2025-11-11T10:01:00Z"
}
```

### Typing Indicator (WebSocket)

```
Client → Server:
{
  "type": "TYPING_START",
  "conversationId": "uuid-match-1"
}

Server → Other Client:
{
  "type": "USER_TYPING",
  "conversationId": "uuid-match-1",
  "userId": "uuid-user-2",
  "userName": "Jane Smith"
}

Client → Server:
{
  "type": "TYPING_STOP",
  "conversationId": "uuid-match-1"
}
```

### Get Conversations

```
GET /chat/conversations?limit=20

Headers:
Authorization: Bearer {token}

Response (200 OK):
{
  "conversations": [
    {
      "id": "uuid-match-1",
      "matchedUser": {
        "id": "uuid-user-2",
        "name": "Jane Smith",
        "profilePictureUrl": "https://..."
      },
      "lastMessage": "See you tomorrow!",
      "lastMessageTime": "2025-11-11T18:30:00Z",
      "unreadCount": 0,
      "createdAt": "2025-11-10T15:30:00Z"
    }
  ],
  "total": 5
}
```

---

## Recommendations

### Get Recommendations

```
GET /recommendations/{userId}?limit=10&algorithm=v1

Headers:
Authorization: Bearer {token}

Response (200 OK):
{
  "recommendations": [
    {
      "id": "uuid-rec-1",
      "recommendedUser": {
        "id": "uuid-user-5",
        "name": "Sarah Wilson",
        "age": 29,
        "profilePictureUrl": "https://...",
        "bio": "Foodie and traveler"
      },
      "score": 92,
      "scoreFactors": {
        "interestMatch": "8/10",
        "ageCompatibility": "Perfect",
        "preferenceAlignment": "High"
      },
      "reason": "Shares 5 interests with you"
    }
  ],
  "total": 150,
  "hasMore": true,
  "generatedAt": "2025-11-11T08:00:00Z"
}
```

### Get Compatibility Score

```
GET /recommendations/{userId}/{targetUserId}/score

Headers:
Authorization: Bearer {token}

Response (200 OK):
{
  "score": 85,
  "factors": {
    "interestMatch": 40,
    "ageCompatibility": 30,
    "preferenceAlignment": 15
  },
  "calculatedAt": "2025-11-11T10:00:00Z"
}
```

---

## Error Handling

### Error Response Format

```
{
  "error": "ERROR_CODE",
  "message": "Human-readable error message",
  "timestamp": "2025-11-11T10:00:00Z",
  "path": "/api/users/register",
  "details": {
    "field": "field-specific error details"
  }
}
```

### HTTP Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| 400 | Bad Request | Invalid email format |
| 401 | Unauthorized | Missing/invalid JWT token |
| 403 | Forbidden | User suspended, can't access |
| 404 | Not Found | User/match not found |
| 409 | Conflict | Email already registered |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server | Database connection error |
| 503 | Service Unavailable | Service temporarily down |

### Common Error Codes

```
INVALID_REQUEST       → Malformed request body
INVALID_TOKEN         → JWT signature invalid
TOKEN_EXPIRED         → JWT token expired
USER_NOT_FOUND        → User doesn't exist
EMAIL_ALREADY_EXISTS  → Email registered
USERNAME_TAKEN        → Username registered
INVALID_CREDENTIALS   → Email/password wrong
RATE_LIMIT_EXCEEDED   → Too many requests
SERVICE_UNAVAILABLE   → Backend service down
INTERNAL_ERROR        → Unexpected server error
```

---

## Response Formats

### Pagination

```
All list endpoints support:
- limit: Items per page (default: 20, max: 100)
- offset: Number of items to skip (default: 0)

Response includes:
{
  "data": [...],
  "total": 245,
  "limit": 20,
  "offset": 0,
  "hasMore": true
}
```

### Timestamps

```
All timestamps in ISO 8601 format (UTC):
"2025-11-11T10:30:45.123Z"
```

### Authentication Header

```
All authenticated requests require:
Authorization: Bearer {jwt_token}

Token format:
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI..."}
```

---

## Rate Limiting

```
Headers included in responses:
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 87
X-RateLimit-Reset: 1731310800

Limits:
- Authenticated users: 100 req/min
- Free tier: 30 req/min
- Premium tier: 1000 req/min
```

---

## Testing Examples

### cURL

```bash
# Register
curl -X POST http://localhost:8080/api/users/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "username": "testuser",
    "password": "Test123!",
    "firstName": "Test",
    "lastName": "User"
  }'

# Login
curl -X POST http://localhost:8080/api/users/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!"
  }'

# Get Profile (with token)
curl -X GET http://localhost:8080/api/users/uuid-1234 \
  -H "Authorization: Bearer eyJhbGc..."
```

### Postman

Import this collection: [postman-collection.json] (to be created)

---

## API Documentation Tools

- Swagger/OpenAPI: (to be generated with springdoc-openapi)
- Available at: http://localhost:8080/swagger-ui.html

---

## Versioning Strategy

```
Current: v1 (URL path: /api/v1/...)
Future versions will use:
/api/v2/...
/api/v3/...

Backwards compatibility maintained for 2 major versions.
Deprecation warnings provided 6 months before removal.
```

---

Document Version: 1.0
Last Updated: 2025-11-11
