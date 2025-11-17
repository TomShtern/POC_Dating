# Troubleshooting Guide - POC Dating Application

**Version:** 1.0.0
**Last Updated:** 2025-11-15
**Purpose:** Quick reference for debugging and resolving common issues

---

## Table of Contents

1. [Quick Start: Triage Checklist](#quick-start-triage-checklist)
2. [Common Pitfalls & Solutions](#common-pitfalls--solutions)
3. [Debugging Workflows](#debugging-workflows)
4. [Monitoring & Inspection](#monitoring--inspection)
5. [Service-Specific Issues](#service-specific-issues)
6. [Emergency Procedures](#emergency-procedures)

---

## Quick Start: Triage Checklist

When something isn't working, use this checklist to diagnose the issue:

```bash
# Step 1: Check Docker status
docker-compose ps

# Step 2: Check service health
for port in 8080 8081 8082 8083 8084 8090; do
  echo "Port $port: $(curl -s http://localhost:$port/actuator/health 2>/dev/null | jq -r '.status // "OFFLINE"')"
done

# Step 3: Check logs for recent errors
docker-compose logs --tail=50 | grep -i error

# Step 4: Check database connectivity
docker exec -it dating_postgres psql -U dating_user -d dating_db -c "SELECT NOW();" 2>/dev/null && echo "DB OK" || echo "DB FAILED"

# Step 5: Check Redis
docker exec -it dating_redis redis-cli ping 2>/dev/null && echo "Redis OK" || echo "Redis FAILED"

# Step 6: Check RabbitMQ
curl -s http://guest:guest@localhost:15672/api/aliveness-test/%2F | jq . >/dev/null 2>&1 && echo "RabbitMQ OK" || echo "RabbitMQ FAILED"
```

---

## Common Pitfalls & Solutions

### 1. Database Connection Errors

**Symptoms:**
- Service fails to start
- `org.postgresql.util.PSQLException: Connection refused`
- Service shows "database unavailable"

**Diagnosis:**
```bash
# Check if PostgreSQL container is running
docker-compose ps | grep postgres

# Check PostgreSQL logs
docker-compose logs postgres | tail -20

# Test direct connection
docker exec -it dating_postgres psql -U dating_user -d dating_db -c "SELECT 1;"
```

**Solutions:**

**Solution A: PostgreSQL not running**
```bash
# Start PostgreSQL
docker-compose up -d postgres

# Wait for it to be ready (may take 10-15 seconds)
sleep 15

# Verify it's running
docker exec -it dating_postgres pg_isready -U dating_user
```

**Solution B: Incorrect credentials**
```bash
# Verify credentials in .env
cat .env | grep POSTGRES

# Expected output:
# POSTGRES_USER=dating_user
# POSTGRES_PASSWORD=dating_password
# POSTGRES_DB=dating_db

# If wrong, update .env and restart
docker-compose down
docker-compose up -d postgres
```

**Solution C: Port conflict (5432 already in use)**
```bash
# Find process using port 5432
sudo lsof -i :5432

# Option 1: Kill the process
kill -9 <PID>

# Option 2: Change port in docker-compose.yml
# Change "5432:5432" to "5433:5432"
# Update .env: POSTGRES_PORT=5433
docker-compose up -d postgres
```

**Solution D: Database not initialized**
```bash
# Check if database exists
docker exec -it dating_postgres psql -U dating_user -d postgres -c "\l"

# If dating_db missing, recreate volume
docker-compose down -v
docker-compose up -d postgres

# Verify schema is created
docker exec -it dating_postgres psql -U dating_user -d dating_db -c "\dt"
```

---

### 2. Port Already in Use

**Symptoms:**
- `Address already in use`
- `Cannot bind to port 8081`
- Service fails to start

**Quick Fix:**
```bash
# For port 8081 (User Service)
lsof -i :8081
kill -9 <PID>

# For port 8082 (Match Service)
lsof -i :8082
kill -9 <PID>

# For all services at once
for port in 8080 8081 8082 8083 8084 8090; do
  PID=$(lsof -t -i :$port 2>/dev/null)
  if [ ! -z "$PID" ]; then
    echo "Killing process on port $port (PID: $PID)"
    kill -9 $PID
  fi
done
```

**Alternative: Change port in docker-compose.yml**
```yaml
# Locate the port mapping (e.g., "8081:8081")
# Change to a different external port (e.g., "8091:8081")
# Then restart
docker-compose up -d user-service
```

---

### 3. JWT Token Issues

**Symptom A: 401 Unauthorized after 15 minutes**

**Cause:** JWT token expired

**Solution:**
```bash
# Check JWT expiration in .env
cat .env | grep JWT

# Expected:
# JWT_SECRET=your-secret-key
# JWT_EXPIRATION_MS=900000  # 15 minutes

# For development, increase expiration
# Edit .env:
JWT_EXPIRATION_MS=3600000  # 1 hour

# Restart services
docker-compose restart user-service api-gateway
```

**Symptom B: Invalid token signature**

**Cause:** JWT_SECRET mismatch between services

**Solution:**
```bash
# Verify JWT_SECRET is same in all services
grep JWT_SECRET .env

# All services must have identical secret
# Update .env if different
JWT_SECRET=your-consistent-secret-key

# Restart all services
docker-compose restart
```

**Symptom C: Token validation fails at API Gateway**

**Solution:**
```bash
# Check API Gateway logs
docker-compose logs -f api-gateway | grep -i token

# Verify token format in request
# Should be: Authorization: Bearer <token>

# Test with manual request
TOKEN=$(curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}' \
  | jq -r '.accessToken')

echo "Token: $TOKEN"

# Use token in next request
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/users/me
```

---

### 4. Cache Invalidation Issues

**Symptom:** Updated data not reflected, old data persists

**Cause:** Redis cache not invalidated after update

**Example Scenario:**
```
1. User updates profile (firstName = "John")
2. Cache still returns old value (firstName = "Jane")
3. User sees cached data, not updated data
```

**Quick Fix: Clear entire cache**
```bash
# Connect to Redis
docker exec -it dating_redis redis-cli

# Clear all cache
> FLUSHALL

# Or clear specific cache keys
> KEYS user:*
> DEL user:123:profile
> DEL user:*  # Clear all user caches

# Exit
> EXIT
```

**Proper Fix: Ensure @CacheEvict is applied**

Check the service code:
```java
// WRONG: Missing @CacheEvict
@Service
public class UserService {
    public void updateUser(UUID userId, UpdateUserRequest request) {
        // User updated but cache not cleared!
        userRepository.save(user);
    }
}

// CORRECT: @CacheEvict applied
@Service
public class UserService {
    @CacheEvict(value = "users", key = "#userId")
    public void updateUser(UUID userId, UpdateUserRequest request) {
        // Cache cleared before update
        userRepository.save(user);
    }
}
```

**Verify cache implementation:**
```bash
# Check Redis cache keys
docker exec -it dating_redis redis-cli

# View cache
> KEYS user:*
1) "user:550e8400-e29b-41d4-a716-446655440000:profile"

# Check TTL
> TTL user:550e8400-e29b-41d4-a716-446655440000:profile
(integer) 3599  # Expires in 3599 seconds

# View cache contents
> GET user:550e8400-e29b-41d4-a716-446655440000:profile
```

---

### 5. Circular Dependencies

**Symptom:**
- `org.springframework.beans.factory.BeanCurrentlyInCreationException`
- Service fails to start
- Error: "Circular dependency detected"

**Cause:** Service A depends on Service B, and Service B depends on Service A

**Example:**
```java
// Service A
@Service
@RequiredArgsConstructor
public class UserService {
    private final MatchService matchService;  // ← Depends on MatchService
}

// Service B
@Service
@RequiredArgsConstructor
public class MatchService {
    private final UserService userService;  // ← Depends on UserService
    // CIRCULAR!
}
```

**Solution 1: Use event-driven architecture**
```java
// Service A - publishes event
@Service
@RequiredArgsConstructor
public class UserService {
    private final RabbitTemplate rabbitTemplate;

    public void register(UserRegistrationRequest request) {
        User user = createUser(request);
        // Publish event instead of calling MatchService directly
        rabbitTemplate.convertAndSend("user.exchange", "user.registered",
            new UserRegisteredEvent(user.getId()));
    }
}

// Service B - consumes event (no direct dependency)
@Component
public class UserEventListener {
    @RabbitListener(queues = "match.user.registered.queue")
    public void handleUserRegistered(UserRegisteredEvent event) {
        // React to event, no circular dependency
    }
}
```

**Solution 2: Use @Lazy annotation (not recommended)**
```java
@Service
@RequiredArgsConstructor
public class UserService {
    @Lazy  // Defer instantiation
    private final MatchService matchService;
}
```

**Solution 3: Refactor into separate service**
```java
// Create new service that both depend on
@Service
@RequiredArgsConstructor
public class SharedService {
    // Common logic
}

@Service
@RequiredArgsConstructor
public class UserService {
    private final SharedService sharedService;  // No circular dependency
}

@Service
@RequiredArgsConstructor
public class MatchService {
    private final SharedService sharedService;  // No circular dependency
}
```

---

### 6. Redis Connection Errors

**Symptom:**
- `redis.clients.jedis.exceptions.JedisConnectionException`
- Cache operations fail silently
- Session data not persisting

**Quick Fix:**
```bash
# Check if Redis is running
docker-compose ps | grep redis

# Check Redis health
docker exec -it dating_redis redis-cli ping

# View Redis logs
docker-compose logs redis | tail -20
```

**Restart Redis:**
```bash
docker-compose restart redis

# Wait a moment
sleep 3

# Verify connection
docker exec -it dating_redis redis-cli ping
# Expected: PONG
```

---

### 7. RabbitMQ Message Queue Issues

**Symptom:**
- Messages not being delivered
- `org.springframework.amqp.AmqpConnectException`
- Events not triggering handlers

**Quick Fix:**
```bash
# Check RabbitMQ status
docker-compose ps | grep rabbitmq

# Check RabbitMQ health
curl http://localhost:15672/api/aliveness-test/%2F \
  -u guest:guest

# View RabbitMQ logs
docker-compose logs rabbitmq | tail -30
```

**Purge and restart:**
```bash
# Stop RabbitMQ
docker-compose stop rabbitmq

# Remove volume to clear queues
docker volume rm poc_dating_rabbitmq_data

# Start fresh
docker-compose up -d rabbitmq

# Wait for startup (30 seconds)
sleep 30

# Verify queues are declared
curl http://localhost:15672/api/queues \
  -u guest:guest | jq '.[].name'
```

---

## Debugging Workflows

### Workflow 1: Diagnose Service Startup Failure

**When:** Service won't start and logs don't help

**Steps:**

```bash
# Step 1: Check service logs (last 100 lines)
docker-compose logs user-service --tail=100

# Step 2: Look for specific error patterns
docker-compose logs user-service | grep -i "error\|exception\|failed"

# Step 3: Check if dependencies are available
docker-compose ps  # See all service statuses

# Step 4: Check for port conflicts
netstat -tulpn | grep 8081  # Check port 8081

# Step 5: Check service health endpoint
curl http://localhost:8081/actuator/health -s | jq .

# Step 6: Review recent code changes
git log --oneline -10

# Step 7: Check environment variables
cat .env | grep -i user

# Step 8: Rebuild service
docker-compose build --no-cache user-service

# Step 9: Restart with verbose logging
docker-compose up user-service  # Don't use -d, watch logs live
```

---

### Workflow 2: Diagnose API Request Failure

**When:** API request returns error or unexpected response

**Steps:**

```bash
# Step 1: Test endpoint with curl
curl -v http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Step 2: Check response status code
# 200-299: Success
# 400-499: Client error (usually wrong input)
# 500-599: Server error (backend issue)

# Step 3: Check API Gateway logs
docker-compose logs api-gateway | grep -i "8080\|error"

# Step 4: Check downstream service logs
docker-compose logs user-service | tail -50

# Step 5: Verify request format
# Check Content-Type header
# Check JSON format with jq
echo '{"email":"test@example.com"}' | jq .

# Step 6: Test without API Gateway (direct to service)
curl -v http://localhost:8081/api/users/me \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000"
# Note: Direct requests need X-User-Id header instead of JWT

# Step 7: Check database state
docker exec -it dating_postgres psql -U dating_user -d dating_db \
  -c "SELECT id, email FROM users LIMIT 5;"

# Step 8: Review request in Postman/curl history
# Did parameters change?
# Did URL encoding break?
```

---

### Workflow 3: Diagnose Performance Issues

**When:** Requests are slow (>1 second response time)

**Steps:**

```bash
# Step 1: Identify slow endpoint
time curl http://localhost:8080/api/users/feed

# Step 2: Check service CPU/memory
docker stats --no-stream user-service

# Step 3: Check database connection pool
docker-compose logs user-service | grep -i "pool\|connection"

# Step 4: Identify slow database queries
# Connect to PostgreSQL
docker exec -it dating_postgres psql -U dating_user -d dating_db

# View slow queries (if enabled)
SELECT query, mean_exec_time FROM pg_stat_statements
ORDER BY mean_exec_time DESC LIMIT 10;

# Find queries taking >100ms
SELECT query, mean_exec_time FROM pg_stat_statements
WHERE mean_exec_time > 100
ORDER BY mean_exec_time DESC;

# Step 5: Check cache effectiveness
docker exec -it dating_redis redis-cli INFO stats

# Look for:
# - keyspace_hits: successful cache retrievals
# - keyspace_misses: cache misses
# - Cache hit ratio = hits / (hits + misses)

# Step 6: Check for N+1 query problems
# Look for repeated similar queries in logs
docker-compose logs user-service | grep "SELECT" | sort | uniq -c

# Step 7: Enable query logging (temporary)
# Add to application.yml:
# spring:
#   jpa:
#     properties:
#       hibernate:
#         generate_statistics: true
# logging:
#   level:
#     org.hibernate.stat: debug

# Step 8: Profile with JFR (Java Flight Recorder)
# Advanced debugging, requires Spring Cloud Sleuth setup
```

---

### Workflow 4: Diagnose Authentication/Authorization Issues

**When:** User can't login, gets 401/403 errors

**Steps:**

```bash
# Step 1: Test login endpoint
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }' | jq .

# Expected response:
# {
#   "accessToken": "eyJhbGciOiJIUzI1NiIs...",
#   "refreshToken": "abcdef123456...",
#   "expiresIn": 900
# }

# Step 2: Check if user exists in database
docker exec -it dating_postgres psql -U dating_user -d dating_db \
  -c "SELECT id, email, status FROM users WHERE email='test@example.com';"

# Step 3: Check if password is correct
# Manually hash and compare
# Note: Passwords are BCrypt hashed, can't reverse

# Step 4: Check JWT token validity
# Decode token at https://jwt.io
TOKEN="your_token_here"

# Or decode with command line
echo $TOKEN | cut -d. -f2 | base64 -d | jq .

# Check:
# - exp: expiration timestamp (should be future)
# - sub: subject (usually userId)
# - iat: issued at

# Step 5: Check if token is blacklisted
docker exec -it dating_redis redis-cli

# Search for blacklisted tokens
> KEYS "*:blacklist*"
> GET "token_blacklist:$TOKEN_ID"

# Exit
> EXIT

# Step 6: Check API Gateway auth filter
docker-compose logs api-gateway | grep -i "auth\|token\|401"

# Step 7: Verify JWT_SECRET consistency
# All services must have same secret
grep JWT_SECRET .env

# If different, update and restart:
docker-compose restart

# Step 8: Check role-based access
# Verify user has required role for endpoint
docker exec -it dating_postgres psql -U dating_user -d dating_db \
  -c "SELECT id, email, role FROM users WHERE email='test@example.com';"
```

---

## Monitoring & Inspection

### Health Check Commands

**Check all services at once:**
```bash
#!/bin/bash
# Save as scripts/health-check.sh

SERVICES=("api-gateway:8080" "user-service:8081" "match-service:8082" \
          "chat-service:8083" "recommendation-service:8084" "vaadin-ui:8090")

echo "Service Health Check - $(date)"
echo "=============================="

for service in "${SERVICES[@]}"; do
  IFS=':' read -r name port <<< "$service"

  status=$(curl -s http://localhost:$port/actuator/health 2>/dev/null | jq -r '.status // "OFFLINE"')

  if [ "$status" = "UP" ]; then
    echo "✓ $name ($port): $status"
  else
    echo "✗ $name ($port): $status"
  fi
done

echo ""
echo "Infrastructure Check"
echo "===================="

# PostgreSQL
pg_status=$(docker exec -it dating_postgres pg_isready -U dating_user 2>/dev/null)
echo "PostgreSQL: $pg_status"

# Redis
redis_status=$(docker exec -it dating_redis redis-cli ping 2>/dev/null)
echo "Redis: $redis_status"

# RabbitMQ
rabbitmq_status=$(curl -s http://localhost:15672/api/aliveness-test/%2F -u guest:guest 2>/dev/null | jq -r '.status // "OFFLINE"')
echo "RabbitMQ: $rabbitmq_status"
```

**Run health check:**
```bash
chmod +x scripts/health-check.sh
./scripts/health-check.sh
```

---

### Log Viewing & Filtering

**View logs from all services:**
```bash
# Last 50 lines from all services
docker-compose logs --tail=50

# Follow logs live (Ctrl+C to stop)
docker-compose logs -f

# Follow logs from specific service
docker-compose logs -f user-service

# Show logs since 10 minutes ago
docker-compose logs --since 10m

# Show logs until 5 minutes ago
docker-compose logs --until 5m
```

**Filter logs for errors:**
```bash
# Show only ERROR level logs
docker-compose logs | grep ERROR

# Show errors with context (3 lines before/after)
docker-compose logs | grep -C 3 ERROR

# Show specific service errors
docker-compose logs user-service | grep -i "error\|exception"

# Show errors with timestamps
docker-compose logs | grep ERROR | cut -d' ' -f1-3

# Find logs for specific user
docker-compose logs | grep "userId=550e8400-e29b-41d4-a716-446655440000"

# Find slow query logs
docker-compose logs user-service | grep "Query took"
```

**Save logs to file for analysis:**
```bash
# Save all logs
docker-compose logs > logs/debug-$(date +%Y%m%d-%H%M%S).log

# Save with timestamps
docker-compose logs --timestamps > logs/debug-with-timestamps.log

# Analyze log file
grep ERROR logs/debug-*.log | wc -l  # Count errors
grep "Exception" logs/debug-*.log | head -10  # First 10 exceptions
```

---

### Database Query Inspection

**Connect to PostgreSQL:**
```bash
docker exec -it dating_postgres psql -U dating_user -d dating_db
```

**Common inspection queries:**
```sql
-- List all tables
\dt

-- Describe users table
\d users

-- Show table size
SELECT schemaname, tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Count records
SELECT 'users' as table_name, COUNT(*) FROM users
UNION ALL
SELECT 'swipes', COUNT(*) FROM swipes
UNION ALL
SELECT 'matches', COUNT(*) FROM matches
UNION ALL
SELECT 'messages', COUNT(*) FROM messages;

-- Find users
SELECT id, email, username, status, created_at FROM users LIMIT 5;

-- Find recent swipes
SELECT user_id, target_user_id, swipe_type, created_at
FROM swipes
ORDER BY created_at DESC
LIMIT 10;

-- Find active matches
SELECT m.id, u1.email, u2.email, m.matched_at
FROM matches m
JOIN users u1 ON m.user1_id = u1.id
JOIN users u2 ON m.user2_id = u2.id
WHERE m.status = 'ACTIVE'
LIMIT 10;

-- Check for slow queries
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
WHERE mean_exec_time > 100
ORDER BY mean_exec_time DESC
LIMIT 20;

-- Find unused indexes
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY pg_relation_size(indexrelid) DESC;

-- Check database size
SELECT pg_database.datname,
  pg_size_pretty(pg_database_size(pg_database.datname)) AS size
FROM pg_database
ORDER BY pg_database_size(pg_database.datname) DESC;

-- Check active connections
SELECT datname, usename, application_name, state, query
FROM pg_stat_activity
WHERE datname = 'dating_db';
```

---

### Redis Inspection

**Connect to Redis:**
```bash
docker exec -it dating_redis redis-cli
```

**Common inspection commands:**
```bash
# Ping Redis
PING
# Expected: PONG

# Get basic info
INFO
# Shows: redis version, memory usage, connected clients, etc.

# List all keys
KEYS *

# Count keys by pattern
KEYS user:* | wc -l
KEYS session:* | wc -l
KEYS match:* | wc -l

# Get memory usage
INFO memory

# Get cache hit ratio
INFO stats
# Look for keyspace_hits and keyspace_misses

# View specific key
GET user:550e8400-e29b-41d4-a716-446655440000:profile

# Check TTL (time to live)
TTL user:550e8400-e29b-41d4-a716-446655440000:profile
# Returns: seconds remaining (-1 = no expiry, -2 = doesn't exist)

# Clear all cache
FLUSHALL

# Clear database 0
FLUSHDB

# Monitor real-time operations
MONITOR

# Get cache size
DBSIZE

# Delete specific keys
DEL user:550e8400-e29b-41d4-a716-446655440000:profile

# Delete all keys matching pattern
EVAL "return redis.call('del', unpack(redis.call('keys', 'user:*')))" 0

# View memory stats per type
INFO keyspace

# Find largest keys
MEMORY STATS

# Exit Redis CLI
EXIT
```

---

### RabbitMQ Monitoring

**Access RabbitMQ Management UI:**
```
URL: http://localhost:15672
Username: guest
Password: guest
```

**Check via command line:**
```bash
# Check RabbitMQ health
curl http://localhost:15672/api/aliveness-test/%2F -u guest:guest

# List all queues
curl http://localhost:15672/api/queues -u guest:guest | jq '.[] | {name, messages_ready, messages_acked}'

# List all exchanges
curl http://localhost:15672/api/exchanges -u guest:guest | jq '.[] | {name, type}'

# View queue details
curl http://localhost:15672/api/queues/%2F/match.user.registered.queue \
  -u guest:guest | jq '{name, messages, messages_details}'

# Check connections
curl http://localhost:15672/api/connections -u guest:guest | jq '.[] | {name, state}'

# Check channels
curl http://localhost:15672/api/channels -u guest:guest | jq '.[] | {name, state}'
```

**Purge a queue:**
```bash
# Via command line
curl -X DELETE http://localhost:15672/api/queues/%2F/QUEUE_NAME/contents \
  -u guest:guest

# Via management console
# Navigate to Queues tab
# Click queue name
# Click "Purge Messages"
```

---

## Service-Specific Issues

### User Service (Port 8081)

**Issue: Login not working**

```bash
# Check service health
curl http://localhost:8081/actuator/health | jq .

# Check logs
docker-compose logs user-service | grep -i "login\|auth"

# Verify user exists
docker exec -it dating_postgres psql -U dating_user -d dating_db \
  -c "SELECT id, email FROM users WHERE email='test@example.com';"

# Test login endpoint directly
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' | jq .

# Check password encoder
# BCrypt encoded passwords can't be reversed, must use login endpoint to verify
```

**Issue: Registration fails**

```bash
# Check validation errors
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"","password":""}' | jq .

# Verify email not duplicate
docker exec -it dating_postgres psql -U dating_user -d dating_db \
  -c "SELECT email FROM users WHERE email='duplicate@example.com';"

# Check email format validation
# Should be valid email format

# Verify password requirements
# Usually: minimum 8 characters, alphanumeric + special char
```

**Issue: Profile update not persisting**

```bash
# Check cache invalidation
docker exec -it dating_redis redis-cli

# Clear user cache
> DEL user:550e8400-e29b-41d4-a716-446655440000:profile

# Exit and retry
> EXIT

# Verify in database
docker exec -it dating_postgres psql -U dating_user -d dating_db \
  -c "SELECT id, first_name, last_name FROM users WHERE id='550e8400-e29b-41d4-a716-446655440000';"
```

---

### Match Service (Port 8082)

**Issue: Feed generation slow or empty**

```bash
# Check service logs
docker-compose logs match-service | tail -50

# Check feed cache
docker exec -it dating_redis redis-cli

# View cached feed
> GET match:550e8400-e29b-41d4-a716-446655440000:feed

# Check feed generation algorithm
# Verify user preferences exist
docker exec -it dating_postgres psql -U dating_user -d dating_db \
  -c "SELECT * FROM user_preferences WHERE user_id='550e8400-e29b-41d4-a716-446655440000';"

# Check candidate pool
docker exec -it dating_postgres psql -U dating_user -d dating_db \
  -c "SELECT COUNT(*) as total_users FROM users WHERE status='ACTIVE';"

# Check if too many swipes (hitting rate limit)
docker-compose logs match-service | grep -i "rate limit"
```

**Issue: Matching algorithm not finding matches**

```bash
# Check swipe data
docker exec -it dating_postgres psql -U dating_user -d dating_db -c "
SELECT user_id, COUNT(*) as swipe_count FROM swipes GROUP BY user_id LIMIT 10;
"

# Check match detection
docker-compose logs match-service | grep -i "match detected"

# Verify RabbitMQ is delivering match events
curl http://localhost:15672/api/queues/%2F/match.detection.queue \
  -u guest:guest | jq '.messages_ready'
```

---

### Chat Service (Port 8083)

**Issue: Messages not received (WebSocket issues)**

```bash
# Check if service is running
curl http://localhost:8083/actuator/health | jq .

# Check WebSocket connection logs
docker-compose logs chat-service | grep -i "websocket\|push"

# Verify message was saved
docker exec -it dating_postgres psql -U dating_user -d dating_db \
  -c "SELECT id, sender_id, content, status FROM messages ORDER BY created_at DESC LIMIT 10;"

# Check message delivery status
docker-compose logs chat-service | grep -i "delivered\|read"

# Verify @Push is configured in Vaadin
grep -r "@Push" backend/vaadin-ui-service/
```

**Issue: Real-time messages have delay**

```bash
# Check network latency
# From browser dev tools: Network tab, check message delivery time

# Check service logs for slow message processing
docker-compose logs chat-service | grep -E "Query took|took .* ms"

# Check RabbitMQ message queue depth
curl http://localhost:15672/api/queues/%2F/chat.message.queue \
  -u guest:guest | jq '.messages_ready, .messages'

# If queue depth is high, messages are backed up
# Restart chat service or increase concurrency
docker-compose restart chat-service

# Check database performance
docker exec -it dating_postgres psql -U dating_user -d dating_db \
  -c "SELECT COUNT(*) FROM messages WHERE created_at > NOW() - INTERVAL '1 hour';"
```

---

### Vaadin UI Service (Port 8090)

**Issue: Page won't load (blank screen)**

```bash
# Check if service is running
curl http://localhost:8090 -I | head -5

# Check Vaadin logs
docker-compose logs vaadin-ui-service | tail -50

# Verify backend connectivity
# Vaadin needs to call API Gateway on port 8080
docker-compose logs vaadin-ui-service | grep -i "api\|gateway\|8080"

# Check browser console for JavaScript errors
# Open http://localhost:8090 in browser
# Press F12, go to Console tab
# Look for red errors

# Clear browser cache
# In browser: Ctrl+Shift+Delete (Windows/Linux) or Cmd+Shift+Delete (Mac)
# Or disable cache in dev tools
```

**Issue: Slow page loads**

```bash
# Check Vaadin compilation logs
docker-compose logs vaadin-ui-service | grep -i "compile\|build"

# Check API calls from Vaadin
# In browser dev tools: Network tab
# Look for slow API calls from localhost:8090 to localhost:8080

# Check backend response times
docker-compose logs api-gateway | grep -E "took .* ms|response time"

# Check Vaadin theme compilation
# First page load compiles themes, subsequent are faster
# Check if running in production mode (theme pre-compiled)
grep -r "production-mode" backend/vaadin-ui-service/
```

**Issue: WebSocket connection fails (real-time updates not working)**

```bash
# Check Vaadin @Push configuration
grep -r "@Push" backend/vaadin-ui-service/src/main/java/com/dating/ui/views/

# Expected annotation:
# @Push(transport = Transport.WEBSOCKET_XHR)
# or
# @Push

# Check if transport is available
docker-compose logs vaadin-ui-service | grep -i "websocket\|transport"

# Monitor WebSocket connections
# In browser dev tools: Network tab, filter by WS

# Check if UI updates are called properly
grep -r "ui.access" backend/vaadin-ui-service/

# Restart Vaadin service
docker-compose restart vaadin-ui-service
```

---

## Emergency Procedures

### Complete System Reset

**When:** Everything is broken, need to start from scratch

```bash
# WARNING: This deletes all data!

# Step 1: Stop all services
docker-compose down

# Step 2: Remove all volumes (deletes data)
docker volume rm poc_dating_postgres_data \
                poc_dating_redis_data \
                poc_dating_rabbitmq_data

# Step 3: Remove all images (optional, force rebuild)
docker-compose down --rmi all

# Step 4: Clean Maven cache (optional, slow rebuild)
cd backend
mvn clean
cd ..

# Step 5: Rebuild everything
docker-compose build --no-cache

# Step 6: Start fresh
docker-compose up -d

# Step 7: Verify health
sleep 30
./scripts/health-check.sh
```

---

### Database Recovery

**When:** Database is corrupted or unreachable

```bash
# Step 1: Check if database exists
docker exec -it dating_postgres psql -U dating_user -d postgres -c "\l"

# Step 2: If dating_db exists, try to repair
docker exec -it dating_postgres psql -U dating_user -d dating_db \
  -c "REINDEX DATABASE dating_db;"

# Step 3: If still broken, drop and recreate
docker exec -it dating_postgres psql -U dating_user -d postgres \
  -c "DROP DATABASE IF EXISTS dating_db;"

docker exec -it dating_postgres psql -U dating_user -d postgres \
  -c "CREATE DATABASE dating_db;"

# Step 4: Re-run schema initialization
docker exec -it dating_postgres psql -U dating_user -d dating_db \
  -f /docker-entrypoint-initdb.d/01-schema.sql

# Step 5: Verify
docker exec -it dating_postgres psql -U dating_user -d dating_db \
  -c "\dt"
```

---

### Memory Leak Diagnosis

**When:** Service uses increasing memory over time

```bash
# Monitor memory usage
docker stats --no-stream

# Check for memory leaks
docker-compose logs user-service | grep -i "memory\|gc"

# Generate heap dump (advanced)
# For Java service, use jmap:
docker exec dating_user_service jmap -dump:live,format=b,file=/tmp/heap.bin 1

# Analyze with tools like Eclipse MAT or VisualVM
# Copy heap.bin locally and analyze

# Immediate fix: Restart service
docker-compose restart user-service

# Check for memory-intensive queries
docker exec -it dating_postgres psql -U dating_user -d dating_db \
  -c "SELECT query, calls, mean_exec_time FROM pg_stat_statements ORDER BY calls DESC LIMIT 10;"
```

---

### High CPU Usage Investigation

**When:** Service consuming 100% CPU

```bash
# Identify service
docker stats --no-stream

# Check logs for infinite loops or busy processing
docker-compose logs user-service | tail -100 | grep -E "loop|retry|processing"

# Check for database lock
docker exec -it dating_postgres psql -U dating_user -d dating_db \
  -c "SELECT * FROM pg_stat_activity WHERE state != 'idle';"

# Check for hanging requests
docker-compose logs api-gateway | grep -i "timeout\|stuck\|hang"

# Quick fix: Restart service
docker-compose restart user-service

# If persists, check recent code changes
git log --oneline -5
git diff HEAD~1

# Check for thread dump (advanced)
# Use jstack to get thread dump and analyze
docker exec dating_user_service jstack 1 > /tmp/threads.txt
```

---

### Network Connectivity Issues

**When:** Services can't communicate with each other

```bash
# Check Docker network
docker network ls
docker network inspect dating_network

# Verify container is connected
docker inspect dating_user_service | grep NetworkSettings

# Test connectivity between containers
docker exec dating_user_service ping dating_postgres

# Test DNS resolution
docker exec dating_user_service nslookup postgres

# Check firewall rules
sudo iptables -L -n | grep DOCKER

# Restart network
docker network prune
docker-compose restart

# Check service discovery (if using Docker Compose)
# Service names should be resolvable within network
# e.g., "postgres" should resolve to PostgreSQL container IP
```

---

## Quick Reference

### Common Port Mappings
- **8080** - API Gateway
- **8081** - User Service
- **8082** - Match Service
- **8083** - Chat Service
- **8084** - Recommendation Service
- **8090** - Vaadin UI Service
- **5432** - PostgreSQL
- **6379** - Redis
- **15672** - RabbitMQ Management (guest/guest)
- **5672** - RabbitMQ

### Common Environment Variables
```bash
# Database
POSTGRES_USER=dating_user
POSTGRES_PASSWORD=dating_password
POSTGRES_DB=dating_db

# JWT
JWT_SECRET=your-secret-key
JWT_EXPIRATION_MS=900000  # 15 minutes

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# RabbitMQ
RABBITMQ_HOST=rabbitmq
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
```

### Essential Bash Aliases
```bash
# Add to ~/.bashrc or ~/.zshrc

alias dc='docker-compose'
alias dps='docker ps'
alias dlogs='docker-compose logs -f'
alias dhealth='docker-compose ps && for port in 8080 8081 8082 8083 8084 8090; do echo "Port $port: $(curl -s http://localhost:$port/actuator/health 2>/dev/null | jq -r .status)"; done'
alias dpg='docker exec -it dating_postgres psql -U dating_user -d dating_db'
alias dredis='docker exec -it dating_redis redis-cli'
alias dbuild='docker-compose build --no-cache'
alias dclean='docker-compose down -v && docker volume prune'
```

---

**Last Updated:** 2025-11-15
**Version:** 1.0.0
