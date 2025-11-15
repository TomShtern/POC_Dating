# Development Guide - POC Dating Application

A practical guide for setting up, building, and developing the POC Dating microservices application.

**Last Updated:** 2025-11-15
**Technology Stack:** Java 21 | Spring Boot 3.2.0 | Maven | PostgreSQL | Redis | RabbitMQ | Vaadin 24.3 | Docker

---

## Table of Contents

1. [Build & Setup](#build--setup)
2. [Development Workflows](#development-workflows)
3. [Database Management](#database-management)
4. [Git Workflow](#git-workflow)
5. [Debugging](#debugging)

---

## Build & Setup

### Prerequisites

Before starting development, ensure you have:

- Java 21 (or compatible JDK)
- Maven 3.8+
- Docker & Docker Compose
- Git
- IDE: IntelliJ IDEA, VS Code, or equivalent

### Initial Setup

```bash
# 1. Clone the repository
git clone <repo-url>
cd POC_Dating

# 2. Copy environment configuration template
cp .env.example .env

# 3. Verify .env contains required variables
# Check for: POSTGRES_USER, POSTGRES_PASSWORD, REDIS_HOST, etc.
cat .env
```

### Environment Configuration

Edit `.env` with your local settings:

```properties
# PostgreSQL
POSTGRES_USER=dating_user
POSTGRES_PASSWORD=dating_password
POSTGRES_DB=dating_db
POSTGRES_PORT=5432

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# RabbitMQ
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_PORT=5672
RABBITMQ_MANAGEMENT_PORT=15672

# Services
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000
```

### Maven Build Commands

```bash
# Build all services (parent module)
cd backend
mvn clean install

# Build specific service
cd backend/user-service
mvn clean package

# Skip tests for faster build
mvn clean install -DskipTests

# Build with verbose output (debug issues)
mvn clean install -X

# Update dependencies
mvn dependency:resolve

# Check for dependency updates
mvn versions:display-dependency-updates

# Enforce version consistency
mvn dependency:check
```

### Build Options & Profiles

```bash
# Build with all profiles
mvn clean install -P dev,test

# Build for production
mvn clean install -P production -DskipTests

# Skip integration tests (faster local builds)
mvn clean install -DskipIntegrationTests

# Generate test coverage report
mvn clean test jacoco:report
# View report: target/site/jacoco/index.html

# Generate project documentation
mvn site
# View: target/site/index.html
```

### IDE Setup

#### IntelliJ IDEA

```
1. File → Open → Select POC_Dating directory
2. Mark backend/ as Sources Root
3. Mark backend/*/src/test as Test Sources Root
4. Preferences → Maven → Importing
   - Check "Automatically download sources and documentation"
   - Check "Create test directories for new modules"
5. Preferences → Compiler → Build process
   - Increase "Build process heap size" to 1024
6. Build → Make Project (Ctrl+F9)
```

**Recommended Plugins:**
- Spring Boot Helper
- Lombok
- SonarLint
- Database Navigator
- Docker

#### VS Code

```json
// .vscode/settings.json configuration
{
  "java.home": "/path/to/jdk21",
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "/path/to/jdk21",
      "default": true
    }
  ],
  "maven.view": "hierarchical",
  "maven.terminal.customEnv": [
    {
      "environmentVariableName": "MAVEN_OPTS",
      "environmentVariableValue": "-Xmx1024m"
    }
  ]
}
```

**Recommended Extensions:**
- Extension Pack for Java (Microsoft)
- Spring Boot Extension Pack
- Maven for Java
- Docker
- REST Client
- Lombok Annotations Support for VS Code

---

## Development Workflows

### Quick Start (All-in-One Docker Compose)

```bash
# 1. Build all services
cd /home/user/POC_Dating/backend
mvn clean install

# 2. Start all services with Docker Compose
cd /home/user/POC_Dating
docker-compose up -d

# 3. Verify services are running
docker-compose ps

# 4. Access the application
# Vaadin UI:    http://localhost:8090
# API Gateway:  http://localhost:8080
# RabbitMQ:     http://localhost:15672 (guest/guest)

# 5. Monitor logs
docker-compose logs -f

# 6. Stop all services
docker-compose down

# 7. Clean up volumes and start fresh
docker-compose down -v
mvn clean install
docker-compose up -d --build
```

### Development Mode (Without Docker Services)

Useful for local development with hot reload:

```bash
# Terminal 1: Start only infrastructure services (databases)
cd /home/user/POC_Dating
docker-compose up postgres redis rabbitmq

# Terminal 2: Start API Gateway
cd /home/user/POC_Dating/backend/api-gateway
mvn spring-boot:run
# Service available at: http://localhost:8080

# Terminal 3: Start User Service
cd /home/user/POC_Dating/backend/user-service
mvn spring-boot:run
# Service available at: http://localhost:8081

# Terminal 4: Start Match Service
cd /home/user/POC_Dating/backend/match-service
mvn spring-boot:run
# Service available at: http://localhost:8082

# Terminal 5: Start Chat Service
cd /home/user/POC_Dating/backend/chat-service
mvn spring-boot:run
# Service available at: http://localhost:8083

# Terminal 6: Start Vaadin UI
cd /home/user/POC_Dating/backend/vaadin-ui-service
mvn spring-boot:run
# UI available at: http://localhost:8090
```

### Hot Reload Development Setup

Enable automatic recompilation and reload:

```bash
# 1. Add spring-boot-devtools to pom.xml (already included)
# This enables:
# - Automatic restart on classpath changes
# - Live reload in browser (with LiveReload extension)
# - Configuration file reload (.yml, .properties)

# 2. Configure IDE for auto-compilation
# IntelliJ: Build → Compiler → "Make project automatically" checkbox
# VS Code: Enable format on save in settings

# 3. Install browser extension for live reload
# - LiveReload (Chrome/Firefox)

# 4. Run service with devtools
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.devtools.restart.enabled=true"

# 5. Edit code and save - application will restart automatically
```

### Running Specific Services

```bash
# Run single service JAR
java -jar backend/user-service/target/user-service-1.0.0.jar

# Run with specific profile
java -jar target/app.jar --spring.profiles.active=dev

# Run with custom JVM options
java -Xmx512m -Xms256m -Dspring.profiles.active=dev -jar app.jar

# Run with debug mode enabled
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar app.jar
```

### Building Docker Images

```bash
# Build all images
docker-compose build

# Build specific service image
docker-compose build user-service

# Build and push to registry
docker-compose build --push

# Build without cache
docker-compose build --no-cache user-service
```

### Service Port Reference

| Service | Port | URL | Purpose |
|---------|------|-----|---------|
| Vaadin UI | 8090 | http://localhost:8090 | Web interface |
| API Gateway | 8080 | http://localhost:8080 | Request routing |
| User Service | 8081 | http://localhost:8081 | Auth, profiles |
| Match Service | 8082 | http://localhost:8082 | Swipes, matching |
| Chat Service | 8083 | http://localhost:8083 | Messaging |
| Recommendation | 8084 | http://localhost:8084 | Recommendations |
| PostgreSQL | 5432 | postgres://localhost:5432 | Database |
| Redis | 6379 | localhost:6379 | Cache |
| RabbitMQ | 5672 | localhost:5672 | Message broker |
| RabbitMQ UI | 15672 | http://localhost:15672 | Management console |

---

## Database Management

### PostgreSQL Connection

```bash
# Connect to PostgreSQL inside Docker
docker exec -it dating_postgres psql -U dating_user -d dating_db

# Connect from host machine (requires psql client)
psql -h localhost -U dating_user -d dating_db -p 5432

# Connect and specify password
PGPASSWORD=dating_password psql -h localhost -U dating_user -d dating_db
```

### Common psql Commands

```sql
-- List all tables
\dt

-- Describe specific table
\d users

-- List all schemas
\dn

-- List all indexes
\di

-- Show table size
\dt+ users

-- Switch database
\c database_name

-- Execute SQL file
\i /path/to/schema.sql

-- Export query results to CSV
\copy (SELECT * FROM users) TO 'users.csv' WITH CSV HEADER

-- Show connection info
\conninfo

-- Exit psql
\q
```

### Database Queries

```sql
-- Check user records
SELECT id, email, username, status, created_at FROM users LIMIT 10;

-- View recent swipes
SELECT * FROM swipes ORDER BY created_at DESC LIMIT 20;

-- Find matches for user
SELECT * FROM matches WHERE user1_id = '<user-id>' OR user2_id = '<user-id>';

-- Message count by user
SELECT sender_id, COUNT(*) as message_count FROM messages GROUP BY sender_id;

-- Performance: View slow queries
SELECT query, calls, mean_exec_time FROM pg_stat_statements
ORDER BY mean_exec_time DESC LIMIT 10;

-- Performance: Reset query stats
SELECT pg_stat_statements_reset();
```

### Database Reset & Migrations

```bash
# Backup current database
docker exec dating_postgres pg_dump -U dating_user dating_db > backup.sql

# Reset entire database (WARNING: Deletes all data!)
docker-compose down -v

# Remove specific volume
docker volume rm poc_dating_postgres_data

# Recreate clean database
docker-compose up -d postgres
docker-compose exec postgres psql -U dating_user -d dating_db < db/init/01-schema.sql

# Restore from backup
docker exec -i dating_postgres psql -U dating_user dating_db < backup.sql
```

### Database Schema Initialization

```bash
# View schema location
ls -la db/init/

# Schema files are automatically executed on container startup
# To manually apply schema:
docker-compose exec postgres psql -U dating_user -d dating_db -f /docker-entrypoint-initdb.d/01-schema.sql

# Check if migrations have been applied
docker exec dating_postgres psql -U dating_user -d dating_db -c "\dt"
```

### Creating Database Migrations

```bash
# 1. Create new migration file
# Naming convention: NN-description.sql (where NN = sequence number)
touch db/init/02-add-location-columns.sql

# 2. Add migration content
cat > db/init/02-add-location-columns.sql << 'EOF'
-- Migration: Add location support to users table
ALTER TABLE users
ADD COLUMN location_latitude FLOAT,
ADD COLUMN location_longitude FLOAT;

CREATE INDEX idx_users_location ON users (location_latitude, location_longitude);
EOF

# 3. Test migration locally
docker-compose down -v
docker-compose up postgres
docker-compose exec postgres psql -U dating_user -d dating_db < db/init/01-schema.sql
docker-compose exec postgres psql -U dating_user -d dating_db < db/init/02-add-location-columns.sql

# 4. Verify migration
docker exec dating_postgres psql -U dating_user -d dating_db -c "\d users"

# 5. Commit migration with git
git add db/init/02-add-location-columns.sql
git commit -m "db: add location columns to users table"
```

### Querying Redis Cache

```bash
# Connect to Redis
docker exec -it dating_redis redis-cli

# Basic commands inside redis-cli
ping                          # Test connection
KEYS *                        # List all keys
KEYS user:*                   # List user-related keys
GET user:123:profile          # Get specific key
DEL user:123:profile          # Delete key
FLUSHALL                      # Clear all Redis data (careful!)
TTL user:123:profile          # Check key expiration

# View key patterns
SCAN 0 MATCH "match:*"

# Monitor real-time operations
MONITOR

# Get memory usage
INFO memory

# Exit redis-cli
exit
```

---

## Git Workflow

### Branching Strategy

```
main/
  ├─ Production-ready code
  └─ Tag releases: v1.0.0, v1.0.1, etc.

develop/
  ├─ Integration branch
  └─ Base for feature branches

feature/feature-name
  ├─ New features
  ├─ Branch from: develop
  └─ PR to: develop

bugfix/bug-name
  ├─ Bug fixes
  ├─ Branch from: develop
  └─ PR to: develop

hotfix/hotfix-name
  ├─ Critical production fixes
  ├─ Branch from: main
  └─ PR to: main (and develop)

claude/claude-md-*
  ├─ AI assistant work
  ├─ Branch from: develop
  └─ PR to: develop
```

### Branch Naming Conventions

```
Good:
  feature/user-registration
  feature/add-matching-algorithm
  bugfix/jwt-expiration-bug
  hotfix/database-connection-timeout
  refactor/extract-feed-service
  claude/claude-md-add-payment-system

Bad:
  feature/new-feature
  fix/issue-123
  myfeature
  Feature/UserRegistration
```

### Commit Message Format

Follow conventional commits specification:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type:**
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `style:` - Code style changes (formatting, semicolons, etc.)
- `refactor:` - Code refactoring (no feature/bug change)
- `perf:` - Performance improvements
- `test:` - Adding/updating tests
- `chore:` - Build, dependencies, tooling
- `ci:` - CI/CD configuration

**Scope:**
- `user:` - User service changes
- `match:` - Match service changes
- `chat:` - Chat service changes
- `gateway:` - API Gateway changes
- `ui:` - Vaadin UI changes
- `db:` - Database schema changes
- `docker:` - Docker configuration

**Examples:**

```bash
feat(user): add user registration endpoint
  - Implement POST /api/users/register
  - Add input validation
  - Add unit tests

fix(match): resolve swipe count bug
  - Fix incorrect count in feed generation
  - Add regression test
  - Fixes #123

docs: update API specification for auth endpoints

style(user): format UserService.java

refactor(match): extract score calculation into separate class

test(user): add integration tests for login flow

chore: update Spring Boot to 3.2.1

perf(match): optimize feed generation query
  - Add composite index on (user_id, created_at)
  - Reduce database queries from 5 to 1
  - Improves performance by 60%
```

### Typical Feature Development Workflow

```bash
# 1. Start from develop branch
git checkout develop
git pull origin develop

# 2. Create feature branch
git checkout -b feature/user-registration

# 3. Make changes and commit frequently
# Edit files...
git add .
git commit -m "feat(user): create registration request DTO"

# Edit more files...
git add .
git commit -m "feat(user): implement registration endpoint"

# Add tests
git add .
git commit -m "test(user): add registration endpoint tests"

# 4. Push branch to remote
git push -u origin feature/user-registration
# (or: git push -u origin HEAD)

# 5. Create Pull Request on GitHub
# Use GitHub CLI:
gh pr create --title "feat(user): user registration" \
  --body "Implements user registration with email validation"

# 6. Address code review feedback
# Make requested changes
git add .
git commit -m "feat(user): address review feedback"
git push

# 7. Merge PR
# After approval, merge via GitHub UI or:
gh pr merge 123 --squash  # Squash commits before merging

# 8. Cleanup local branch
git checkout develop
git pull origin develop
git branch -d feature/user-registration
```

### Advanced Git Commands

```bash
# View commit history
git log --oneline -10              # Last 10 commits
git log --oneline --graph --all    # Visual history
git log --stat                     # Changes per commit
git log -p -3                      # Last 3 commits with diff

# Stash changes temporarily
git stash                          # Save changes
git stash pop                      # Restore changes
git stash list                     # View stashed changes

# Squash commits before PR
git rebase -i HEAD~3               # Squash last 3 commits

# Cherry-pick commits
git cherry-pick <commit-hash>

# Revert commit
git revert <commit-hash>           # Creates new "revert" commit
git reset --soft HEAD~1            # Undo commit, keep changes

# View changes before commit
git diff                           # Unstaged changes
git diff --cached                  # Staged changes
git diff main..feature/branch      # Compare branches

# Tag releases
git tag v1.0.0
git push origin v1.0.0
```

### Code Review Best Practices

**Before submitting PR:**
```bash
# 1. Run all tests locally
cd backend
mvn test

# 2. Check code coverage
mvn jacoco:report
# Review target/site/jacoco/index.html

# 3. Run linting/formatting
mvn spotless:apply

# 4. Verify no security vulnerabilities
mvn dependency:check

# 5. Build Docker image
docker-compose build <service>

# 6. Verify commit messages are descriptive
git log --oneline origin/develop..HEAD
```

**PR Checklist:**
- All tests pass
- Code coverage maintained (70%+)
- No breaking changes to API
- Documentation updated
- Database migrations tested
- Environment variables in .env.example updated
- No secrets/credentials committed

---

## Debugging

### Service Health Checks

```bash
# Check individual service health
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8082/actuator/health  # Match Service
curl http://localhost:8083/actuator/health  # Chat Service
curl http://localhost:8084/actuator/health  # Recommendation Service

# Check all service health (bash script)
for port in 8080 8081 8082 8083 8084; do
  echo "Port $port: $(curl -s http://localhost:$port/actuator/health | jq '.status // "OFFLINE"')"
done

# Detailed health info
curl http://localhost:8081/actuator/health?include=db,redis,rabbit

# Application metrics
curl http://localhost:8081/actuator/metrics
curl http://localhost:8081/actuator/metrics/http.server.requests
```

### Viewing Service Logs

```bash
# View logs from all services
docker-compose logs

# Follow logs in real-time
docker-compose logs -f

# View logs from specific service
docker-compose logs -f user-service

# View last N lines
docker-compose logs --tail=100 user-service

# Filter logs by pattern
docker-compose logs user-service | grep ERROR
docker-compose logs match-service | grep userId=123

# View logs with timestamps
docker-compose logs --timestamps user-service

# View logs for multiple services
docker-compose logs -f user-service match-service chat-service

# Export logs to file
docker-compose logs user-service > user-service.log

# View logs from stopped container
docker logs <container-id>

# View logs with color highlighting
docker-compose logs --follow --no-log-prefix user-service
```

### Remote Debugging with JDWP

```bash
# 1. Start service with debug mode enabled
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  -jar backend/user-service/target/user-service-1.0.0.jar

# 2. Or update docker-compose.yml
services:
  user-service:
    environment:
      - JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
    ports:
      - "5005:5005"  # Debug port

# 3. Start service
docker-compose up user-service

# 4. Configure IDE debugger
# IntelliJ: Run → Edit Configurations → + Remote
#   - Host: localhost
#   - Port: 5005
# VS Code: .vscode/launch.json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Attach to User Service",
      "type": "java",
      "name": "Attach to Service",
      "request": "attach",
      "hostName": "localhost",
      "port": 5005,
      "preLaunchTask": "java: Start debugging"
    }
  ]
}

# 5. Set breakpoints in IDE and attach debugger
# IntelliJ: Run → Debug 'Attach to Service'
# VS Code: Click Debug icon in left sidebar

# 6. Application execution will pause at breakpoints
# View variables, step through code, etc.
```

### Common Debug Scenarios

#### Database Connection Issues

```bash
# Test PostgreSQL connectivity
docker-compose exec postgres psql -U dating_user -d dating_db -c "SELECT 1;"

# Check connection pool status
curl http://localhost:8081/actuator/health | jq '.components.db'

# View active connections in PostgreSQL
docker exec dating_postgres psql -U dating_user -d dating_db -c \
  "SELECT datname, count(*) FROM pg_stat_activity GROUP BY datname;"

# Verify connection string in logs
docker-compose logs user-service | grep "Establishing JDBC connection"

# Check PostgreSQL logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

#### Memory Issues

```bash
# Check container memory usage
docker stats dating_user-service

# View memory in logs
docker-compose logs postgres | grep "memory"

# Update container memory limit in docker-compose.yml
services:
  user-service:
    deploy:
      resources:
        limits:
          memory: 512M
        reservations:
          memory: 256M

# Monitor Java heap
jmap -heap <pid>
jstat -gc -h10 <pid> 1000  # Every 1 second
```

#### Slow Queries

```bash
# Enable slow query logging in PostgreSQL
docker exec dating_postgres psql -U dating_user -d dating_db -c \
  "ALTER SYSTEM SET log_min_duration_statement = 1000;" # 1 second

# Restart PostgreSQL
docker-compose restart postgres

# View slow queries
docker-compose logs postgres | grep "duration:"

# Analyze query performance
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'test@example.com';

# Find missing indexes
SELECT * FROM pg_stat_user_tables WHERE idx_scan = 0;

# View query execution plan
EXPLAIN SELECT * FROM swipes WHERE user_id = '123' ORDER BY created_at DESC;
```

#### Message Queue Issues

```bash
# Check RabbitMQ status
docker-compose logs rabbitmq

# Access RabbitMQ management console
# http://localhost:15672 (guest/guest)

# View queue details via HTTP API
curl -u guest:guest http://localhost:15672/api/queues

# View exchanges
curl -u guest:guest http://localhost:15672/api/exchanges

# View bindings
curl -u guest:guest http://localhost:15672/api/bindings

# Purge queue (delete all messages)
curl -u guest:guest -X DELETE http://localhost:15672/api/queues/%2F/match.user.registered.queue/contents

# Monitor message flow
# Log into RabbitMQ UI and watch queue depths
```

#### Cache Issues

```bash
# Check Redis connectivity
docker exec dating_redis redis-cli ping

# View all cache keys
docker exec dating_redis redis-cli KEYS "*"

# Check specific cache key
docker exec dating_redis redis-cli GET "user:123:profile"

# Clear all cache
docker exec dating_redis redis-cli FLUSHALL

# Monitor cache operations
docker exec dating_redis redis-cli MONITOR

# View memory usage
docker exec dating_redis redis-cli INFO memory

# Find large keys
docker exec dating_redis redis-cli --bigkeys
```

#### Port Conflicts

```bash
# Find process using port
lsof -i :8081

# Kill process using port
kill -9 <PID>

# Or temporarily change port in environment
docker-compose.yml:
services:
  user-service:
    ports:
      - "8091:8081"  # Use 8091 instead

# Restart services
docker-compose down
docker-compose up -d
```

### Troubleshooting Checklist

| Issue | Check |
|-------|-------|
| Service won't start | Logs, port conflicts, dependencies, .env file |
| Slow performance | Database queries, missing indexes, cache hit rate |
| Database errors | Connection pool exhausted, migrations not applied, schema issues |
| Authentication fails | JWT token expiry, password hashing, token in Redis blacklist |
| Message queue errors | RabbitMQ connectivity, queue bindings, dead letter queue |
| Cache misses | Redis running, key expiration, cache invalidation logic |
| Out of memory | Container resource limits, memory leaks, large datasets |

### Useful Debugging Commands

```bash
# Kill all running containers
docker-compose down

# Clean all dangling images/volumes
docker image prune -a
docker volume prune

# Inspect container
docker inspect <container-id>

# Execute command in running container
docker exec -it <container-id> <command>

# View container resource usage
docker stats

# Copy file from container
docker cp <container-id>:/path/to/file ./local-file

# View environment variables in container
docker exec <container-id> env

# Restart service
docker-compose restart user-service

# Recreate container (fresh start)
docker-compose up -d --force-recreate user-service

# View network
docker network ls
docker network inspect <network-id>

# Test connectivity between containers
docker exec <container-id> ping <service-name>
```

---

## Performance Monitoring

### Response Time Targets

- API Gateway: <100ms
- User Service: <200ms
- Match Service: <500ms (feed generation)
- Chat Service: <100ms
- Database queries: <100ms

### Key Metrics to Monitor

```bash
# Request metrics
curl http://localhost:8081/actuator/metrics/http.server.requests

# JVM metrics
curl http://localhost:8081/actuator/metrics/jvm.memory.used

# Database pool metrics
curl http://localhost:8081/actuator/metrics/hikaricp.connections

# Cache metrics (if available)
curl http://localhost:8081/actuator/metrics/cache.hits
```

---

## Summary

This guide covers:

1. **Build & Setup** - Maven commands, IDE configuration, environment setup
2. **Development Workflows** - Quick start, local development, hot reload
3. **Database Management** - PostgreSQL operations, migrations, Redis inspection
4. **Git Workflow** - Branching strategy, commit conventions, feature development
5. **Debugging** - Health checks, logging, remote debugging, troubleshooting

For additional information, refer to:
- `/home/user/POC_Dating/CLAUDE.md` - AI assistant guide with architecture details
- `/home/user/POC_Dating/docs/DEVELOPMENT.md` - Extended development documentation
- `/home/user/POC_Dating/docs/VAADIN_IMPLEMENTATION.md` - UI-specific details
- `/home/user/POC_Dating/backend/{service}/README.md` - Service-specific docs

---

**Remember:** Always run tests before committing, keep logs clean, and document your changes. Happy coding!
