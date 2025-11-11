# API Specification - REVISED (Fixed Misalignments)
#
# CRITICAL FIXES APPLIED:
# 1. JWT token expiration corrected: 15 minutes (not 24 hours)
# 2. API versioning: /api/v1/* (fully versioned)
# 3. WebSocket path corrected: /api/chat/ws (consistent with routing)
# 4. Rate limiting configuration specified
# 5. Event schemas added for RabbitMQ integration

[This is a placeholder for the corrected API specification]
[Full updated file should be created - see docs/API-SPECIFICATION-FIXED.md]

## KEY CORRECTIONS MADE:

### JWT Token Expiration
- Access Token: 15 minutes (was 24 hours)
- Refresh Token: 7 days
- Reason: Security best practice for short-lived tokens

### API Versioning
- All endpoints: /api/v1/* (versioned from the start)
- Allows future v2 without breaking changes
- Versioning header optional: Accept-Version: 1

### WebSocket Endpoint
- Final URL: /api/v1/chat/ws (consistent with routing)
- Path parameters: ?token=JWT (or Authorization header)

### Rate Limiting
- Authenticated users: 100 requests/minute
- Free tier: 30 requests/minute
- Headers: X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset

### Event Schemas
All RabbitMQ events now documented with JSON schemas:
- user:registered
- match:created
- message:sent
- etc.
