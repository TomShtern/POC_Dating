# Development Guide - POC Dating Application

**Document Status:** ✅ **ACTIVE** - Updated for Vaadin approach
**Last Updated:** 2025-11-11
**Version:** 2.0

## Getting Started

### Prerequisites

- Java 21 JDK (https://adoptopenjdk.net/)
- Maven 3.8+ (https://maven.apache.org/)
- Docker & Docker Compose (https://www.docker.com/)
- ~~Node.js 18+~~ ⚠️ NOT NEEDED (using Vaadin, not React)
- Git
- IDE: IntelliJ IDEA (recommended) or VS Code with Java extensions

### Initial Setup

```bash
# 1. Clone the repository
git clone <repository-url>
cd POC_Dating

# 2. Copy environment configuration
cp .env.example .env

# 3. Build all services (including Vaadin UI)
cd backend
mvn clean install

# 4. Start all services with Docker Compose
cd ..
docker-compose up -d

# 5. Verify services are running
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8090/                 # Vaadin UI

# 6. Access the application
# Open browser: http://localhost:8090
```

### Quick Start (Development Mode)

```bash
# Terminal 1: Start infrastructure only
docker-compose up postgres redis rabbitmq

# Terminal 2: Run backend service
cd backend/user-service
mvn spring-boot:run

# Terminal 3: Run Vaadin UI
cd backend/vaadin-ui-service
mvn spring-boot:run

# Access: http://localhost:8090
```

### Service Health Checks

```bash
# Vaadin UI Service
curl http://localhost:8090/

# API Gateway
curl http://localhost:8080/actuator/health

# User Service
curl http://localhost:8081/actuator/health

# Match Service
curl http://localhost:8082/actuator/health

# Chat Service
curl http://localhost:8083/actuator/health

# Recommendation Service
curl http://localhost:8084/actuator/health
```

---

## Project Structure

```
POC_Dating/
├── backend/                    # All Java microservices
│   ├── pom.xml                # Parent POM
│   ├── common-library/        # Shared code
│   ├── user-service/
│   ├── match-service/
│   ├── chat-service/
│   ├── recommendation-service/
│   └── api-gateway/
│
├── frontend/                  # React web app
│   ├── src/
│   ├── package.json
│   └── ...
│
├── db/                        # Database files
│   └── init/
│       └── 01-schema.sql
│
├── docs/                      # Documentation
│
├── docker-compose.yml         # Local dev orchestration
└── .env.example              # Configuration template
```

---

## Backend Development

### Build All Services

```bash
cd backend
mvn clean install
```

### Build Specific Service

```bash
cd backend/user-service
mvn clean package
```

### Run Service Locally (without Docker)

```bash
cd backend/user-service

# Set environment variables (or modify application.yml)
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_USER=dating_user
export POSTGRES_PASSWORD=changeme123

# Run Spring Boot app
mvn spring-boot:run
```

### Database Migrations

```bash
# Currently using manual SQL scripts in db/init/

# View schema
cat db/init/01-schema.sql

# To connect to running PostgreSQL:
psql -h localhost -U dating_user -d dating_db

# Run custom migration:
psql -h localhost -U dating_user -d dating_db -f migration-file.sql
```

### IDE Setup

#### IntelliJ IDEA
```
1. File → Open → select POC_Dating directory
2. Choose "Open as Project"
3. Set Project SDK to Java 21
4. Maven should auto-detect and index
5. Right-click backend/pom.xml → "Add as Maven Project"
6. Services → Docker → configure Docker daemon
```

#### VS Code
```
1. Open folder: POC_Dating
2. Install extensions:
   - Extension Pack for Java (Microsoft)
   - Spring Boot Extension Pack
   - Docker
   - REST Client
3. Open backend/pom.xml
4. Maven will auto-detect
```

### Running Tests

```bash
# All tests in project
mvn test

# Single module
cd backend/user-service
mvn test

# Specific test class
mvn test -Dtest=UserServiceTest

# With coverage
mvn clean test jacoco:report
# View: target/site/jacoco/index.html
```

### Debugging

#### Remote Debug in IDE

```bash
# 1. Start service with debug flag
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
     -jar target/user-service.jar

# 2. In IDE, Run → Debug Configurations
#    Add "Remote JVM Debug", localhost:5005
```

#### Logs

```bash
# View logs from Docker container
docker-compose logs -f user-service

# View specific lines
docker-compose logs -f --tail=100 user-service
```

---

## Frontend Development (Vaadin UI)

### Development Server

```bash
cd backend/vaadin-ui-service
mvn spring-boot:run

# Open http://localhost:8090
# Vaadin auto-reloads on Java file changes
```

### View Development

```bash
# All views in src/main/java/com/dating/ui/views/
views/
├── LoginView.java         # User authentication
├── RegisterView.java      # New user registration
├── SwipeView.java         # Profile browsing
├── ChatView.java          # Real-time messaging
├── ProfileView.java       # User profile
└── SettingsView.java      # User preferences
```

### Creating a New View

```java
package com.dating.ui.views;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.Route;

@Route(value = "myview", layout = MainLayout.class)
@PageTitle("My View | POC Dating")
public class MyView extends VerticalLayout {

    public MyView() {
        H1 title = new H1("My View");
        Button button = new Button("Click me", e -> handleClick());

        add(title, button);
    }

    private void handleClick() {
        Notification.show("Button clicked!");
    }
}
```

### Service Integration

```java
// Create Feign client
@FeignClient(name = "match-service", url = "${services.match-service.url}")
public interface MatchServiceClient {
    @GetMapping("/api/matches/next-profile")
    User getNextProfile(@RequestHeader("Authorization") String token);
}

// Use in view
@Service
public class MatchService {
    @Autowired
    private MatchServiceClient matchClient;

    public User getNextProfile() {
        String token = SecurityUtils.getCurrentToken();
        return matchClient.getNextProfile("Bearer " + token);
    }
}
```

### Testing Vaadin Views

```java
@SpringBootTest
class SwipeViewTest {

    @Autowired
    private MatchService matchService;

    @Test
    void testLoadProfile() {
        when(matchService.getNextProfile()).thenReturn(createTestUser());

        SwipeView view = new SwipeView(matchService);

        assertNotNull(view.getCurrentUser());
    }
}
```

### Hot Reload (Vaadin)

Vaadin supports hot-reload during development:

1. Run with Spring Boot DevTools enabled
2. Make changes to Java files
3. Save file → automatic recompilation
4. Browser refreshes automatically

```bash
# Enable DevTools in pom.xml (already included)
mvn spring-boot:run

# Or run from IDE with "Update classes and resources" on save
```

---

## Database Management

### Connect to PostgreSQL

```bash
# Via Docker
docker exec -it dating_postgres psql -U dating_user -d dating_db

# Via local psql
psql -h localhost -U dating_user -d dating_db

# Common commands:
\dt                 # List tables
\d table_name       # Describe table
SELECT * FROM users LIMIT 5;
```

### Reset Database (Development Only)

```bash
# Stop containers
docker-compose down -v

# Remove volumes (will delete all data)
docker volume rm poc_dating_postgres_data

# Restart
docker-compose up -d
```

### Create a Migration

```sql
-- db/init/02-add-column.sql

ALTER TABLE users ADD COLUMN location_latitude FLOAT;
ALTER TABLE users ADD COLUMN location_longitude FLOAT;

-- Remember to version your migrations!
```

---

## API Testing

### Using REST Client Extension (VS Code)

Create `requests.http`:

```http
### Get all users
GET http://localhost:8080/api/users
Authorization: Bearer eyJhbGc...

### Create user
POST http://localhost:8080/api/users/auth/register
Content-Type: application/json

{
  "email": "test@example.com",
  "username": "testuser",
  "password": "Test123!",
  "firstName": "Test",
  "lastName": "User"
}

### Get specific user
GET http://localhost:8080/api/users/uuid-1234
Authorization: Bearer eyJhbGc...
```

### Using Postman

Import the API from: http://localhost:8080/v3/api-docs (when Swagger is enabled)

Or create collection manually with environment variables:
```
{{base_url}} = http://localhost:8080
{{token}} = (copy from login response)
```

---

## Git Workflow

### Creating Feature Branches

```bash
# Start from develop
git checkout develop
git pull origin develop

# Create feature branch
git checkout -b feature/my-feature

# Make commits with clear messages
git commit -m "feat: add user registration endpoint"

# Push branch
git push -u origin feature/my-feature

# Create Pull Request on GitHub
```

### Commit Message Format

```
feat: add new feature
fix: resolve bug
docs: update documentation
style: code formatting
refactor: restructure code
test: add tests
chore: update dependencies
```

### Code Review

1. Push feature branch
2. Create Pull Request
3. Request review from teammates
4. Address feedback
5. Merge to develop
6. Delete feature branch

---

## Common Tasks

### Add a New Endpoint

```java
// 1. Create controller method
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping("/test")
    public ResponseEntity<TestResponse> testEndpoint() {
        // Implementation
        return ResponseEntity.ok(response);
    }
}

// 2. Test it
curl -X POST http://localhost:8080/api/users/test

// 3. Commit
git commit -m "feat: add test endpoint"
```

### Add a Database Migration

```bash
# 1. Create SQL file in db/init/
# 2. Version it: db/init/02-add-feature.sql
# 3. Add ALTER/CREATE statements
# 4. Run: psql -h localhost ... -f db/init/02-add-feature.sql
# 5. Commit
```

### Add New npm Dependency

```bash
cd frontend
npm install new-package
git commit -m "chore: add new-package"
```

### Update Java Dependency

```bash
cd backend
# Edit pom.xml in dependencyManagement or dependencies
git commit -m "chore: update spring-boot to 3.2.1"
```

---

## Performance & Debugging

### Database Query Optimization

```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
    show-sql: true  # Enable in dev
```

View generated SQL in logs.

### Memory Profiling

```bash
# Start service with profiling
java -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=./heap.bin \
     -jar service.jar

# Analyze heap dump with jvisualvm or Eclipse MAT
```

### Redis Inspection

```bash
# Connect to Redis CLI
docker exec -it dating_redis redis-cli

# View keys
KEYS *
GET user:1234:profile
TTL user:1234:profile
```

### RabbitMQ Management

Visit: http://localhost:15672
- Username: guest
- Password: guest

View queues, messages, connections.

---

## Troubleshooting

### Service won't start

```bash
# Check logs
docker-compose logs user-service

# Common issues:
# - Port already in use: change port in docker-compose.yml
# - Database connection: verify POSTGRES_HOST, POSTGRES_PASSWORD
# - Out of memory: increase Xmx in Dockerfile or JVM args
```

### Frontend won't load

```bash
# Check webpack
npm run dev

# Clear cache
rm -rf node_modules/.cache
npm install
```

### Database connection error

```bash
# Verify PostgreSQL is running
docker-compose ps

# Check credentials in .env
cat .env | grep POSTGRES

# Test connection
docker exec dating_postgres psql -U dating_user -c "SELECT 1;"
```

### WebSocket connection fails

```bash
# Check Chat Service logs
docker-compose logs chat-service

# Verify JWT token is valid
# Check browser console for WebSocket errors
# Ensure RabbitMQ is running
```

---

## Performance Targets

| Metric | Target | Notes |
|--------|--------|-------|
| API response time | < 500ms | P95 |
| Feed generation | < 1s | For 100 profiles |
| Message delivery | < 100ms | Via WebSocket |
| Database query | < 100ms | Including network |
| Frontend load | < 2s | Full page load |

---

## Security Best Practices

- Never commit `.env` file
- Rotate JWT secrets regularly
- Use HTTPS in production
- Validate all inputs
- Sanitize database queries (use JPA)
- Log security events
- Keep dependencies updated
- Use strong passwords in dev
- Restrict database access

---

## Resources

- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [React Docs](https://react.dev)
- [Docker Docs](https://docs.docker.com)
- [PostgreSQL Docs](https://www.postgresql.org/docs/)
- [Maven Docs](https://maven.apache.org/guides/)

---

## Getting Help

1. Check documentation in `/docs/`
2. Review related service's README
3. Ask team members
4. Search GitHub issues
5. Consult official documentation

---

Document Version: 1.0
Last Updated: 2025-11-11
