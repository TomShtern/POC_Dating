# API Gateway

## Overview

Single entry point for all API requests, handling routing, authentication, rate limiting, and load balancing.

## Port
**8080** (public-facing)

## Responsibilities

### Request Routing
- Route API requests to correct microservice
- URL path-based routing
- HTTP method preservation
- Query parameter forwarding

### Security
- JWT token validation
- Enforce HTTPS (production)
- CORS configuration
- Rate limiting per user
- IP whitelisting (future)

### Resilience
- Circuit breaker for failing services
- Request retry logic
- Timeout management
- Fallback responses

### Observability
- Request/response logging
- Distributed tracing (Sleuth)
- Metrics collection
- Health checks

## Routing Rules

### User Service
```
POST   /api/users/auth/register          → user-service:8081
POST   /api/users/auth/login             → user-service:8081
POST   /api/users/auth/logout            → user-service:8081
GET    /api/users/{id}                   → user-service:8081
PUT    /api/users/{id}                   → user-service:8081
```

### Match Service
```
GET    /api/matches/feed/{userId}        → match-service:8082
POST   /api/matches/swipes               → match-service:8082
GET    /api/matches                      → match-service:8082
```

### Chat Service
```
WS     /api/chat/ws                      → chat-service:8083 (WebSocket)
GET    /api/chat/conversations           → chat-service:8083
GET    /api/chat/conversations/{id}      → chat-service:8083
```

### Recommendation Service
```
GET    /api/recommendations/{userId}     → recommendation-service:8084
GET    /api/recommendations/{userId}/{targetId}/score → recommendation-service:8084
```

## Authentication Flow

```
Client Request
    ↓
API Gateway Receives Request
    ↓
Extract JWT from Authorization header
    ↓
Validate JWT signature
    ↓
Check token expiration
    ↓
Extract userId from JWT claims
    ↓
Add userId to request headers (X-User-Id)
    ↓
Route to target microservice
    ↓
Target service uses X-User-Id for authorization
```

## Configuration

### Service URLs (in application.yml)
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://user-service:8081
          predicates:
            - Path=/api/users/**

        - id: match-service
          uri: http://match-service:8082
          predicates:
            - Path=/api/matches/**

        - id: chat-service
          uri: http://chat-service:8083
          predicates:
            - Path=/api/chat/**
          filters:
            - RewritePath=/api/chat/(?<segment>.*), /$\{segment}

        - id: recommendation-service
          uri: http://recommendation-service:8084
          predicates:
            - Path=/api/recommendations/**
```

## Rate Limiting

### Per-User Limits
```
Authenticated Users: 100 requests / minute
Free Tier: 30 requests / minute
Premium Tier: 1000 requests / minute
```

### Implementation
```
Redis-backed rate limiter
Key: ratelimit:{userId}:{endpoint}
Window: 1 minute sliding window
```

## Circuit Breaker Configuration

### Thresholds
```
Failure rate: > 50%
Slow request duration: > 2 seconds
Failure threshold count: > 10 in 30s window
```

### States
```
CLOSED: Normal operation, requests pass through
OPEN: Too many failures, requests rejected immediately
HALF_OPEN: Testing if service recovered, limited requests allowed
```

### Fallback Responses
```
503 Service Unavailable: Circuit open
504 Gateway Timeout: Service timeout
500 Internal Server: Routing error
```

## Logging & Tracing

### Log Format
```
[timestamp] [request-id] [user-id] METHOD /path STATUS response-time
2025-11-11 10:23:45.123 [abc123] [user-999] POST /api/users/auth/login 200 45ms
```

### Distributed Tracing
```
Each request gets trace ID in header
Propagated to all microservices
Allows following request through system
Collected in ELK stack for analysis
```

## Monitoring

### Health Endpoint
```
GET /actuator/health

Response:
{
  "status": "UP",
  "components": {
    "discoveryClient": { "status": "UP" },
    "circuitBreakers": {
      "user-service": { "status": "UP" },
      "match-service": { "status": "UP" },
      "chat-service": { "status": "UP" },
      "recommendation-service": { "status": "UP" }
    }
  }
}
```

### Metrics
```
GET /actuator/metrics

Track:
- Request count by endpoint
- Response times (p50, p95, p99)
- Error rates by service
- Circuit breaker state changes
- Rate limit violations
```

## Error Responses

```
400 Bad Request
{
  "error": "INVALID_REQUEST",
  "message": "Missing required field: email",
  "timestamp": "2025-11-11T10:23:45Z",
  "path": "/api/users/auth/register"
}

401 Unauthorized
{
  "error": "INVALID_TOKEN",
  "message": "JWT token expired",
  "timestamp": "2025-11-11T10:23:45Z"
}

503 Service Unavailable
{
  "error": "SERVICE_UNAVAILABLE",
  "message": "User service is temporarily unavailable",
  "timestamp": "2025-11-11T10:23:45Z"
}
```

## CORS Configuration

### Allowed Origins
```
Development: http://localhost:3000
Production: https://app.example.com
```

### Allowed Methods
```
GET, POST, PUT, DELETE, OPTIONS
```

### Allowed Headers
```
Authorization, Content-Type, X-Requested-With
```

## WebSocket Support

### Proxy Configuration
```
WS connections to /api/chat/ws
Routed to chat-service:8083/ws
Maintains persistent connections
RabbitMQ STOMP broker for message distribution
```

## Testing

### Unit Tests
- Route matching
- JWT validation
- Rate limiting logic

### Integration Tests
- End-to-end request routing
- Service discovery
- Circuit breaker behavior
- Rate limit enforcement

## Future Enhancements

- GraphQL gateway support
- Request aggregation (combine multiple service calls)
- Caching layer (response caching)
- API versioning (v1, v2 routes)
- Analytics dashboard
- API key management
- Webhook support
- Request/response transformation
- API documentation (Swagger/OpenAPI)
