# Vaadin UI Service

**Pure Java Web UI for POC Dating Application**

## Overview

This service provides the web user interface for the POC Dating Application, built entirely in Java using Vaadin Framework. No JavaScript, TypeScript, or React knowledge required!

### Technology Stack

- **Framework:** Vaadin 24.3 (Vaadin Flow)
- **Language:** Java 21
- **Build Tool:** Maven 3.8+
- **Backend Integration:** Spring Cloud OpenFeign
- **Session Management:** Redis
- **Security:** Spring Security
- **Real-time:** Vaadin @Push (WebSocket/SSE)

## Project Structure

```
vaadin-ui-service/
├── src/
│   ├── main/
│   │   ├── java/com/dating/ui/
│   │   │   ├── VaadinUIApplication.java    # Main application class
│   │   │   ├── views/                      # UI Views
│   │   │   │   ├── LoginView.java          # Login page
│   │   │   │   ├── RegisterView.java       # Registration
│   │   │   │   ├── SwipeView.java          # Main discovery
│   │   │   │   ├── MatchesView.java        # Matches list
│   │   │   │   ├── MessagesView.java       # Conversations
│   │   │   │   ├── ProfileView.java        # User profile
│   │   │   │   └── MainLayout.java         # Navigation
│   │   │   ├── components/                 # Reusable components
│   │   │   │   └── ProfileCard.java        # Profile card
│   │   │   ├── service/                    # Business logic
│   │   │   │   ├── UserService.java
│   │   │   │   ├── MatchService.java
│   │   │   │   └── ChatService.java
│   │   │   ├── client/                     # Feign clients
│   │   │   │   ├── UserServiceClient.java
│   │   │   │   ├── MatchServiceClient.java
│   │   │   │   ├── ChatServiceClient.java
│   │   │   │   └── RecommendationServiceClient.java
│   │   │   ├── security/                   # Security config
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── SecurityUtils.java
│   │   │   └── dto/                        # Data transfer objects
│   │   │       ├── User.java
│   │   │       ├── Match.java
│   │   │       ├── Message.java
│   │   │       └── ...
│   │   └── resources/
│   │       ├── application.yml             # Configuration
│   │       └── static/                     # Static assets
│   └── test/                               # Tests
├── frontend/
│   └── themes/
│       └── dating-theme/                   # Custom theme
│           ├── styles.css
│           └── theme.json
├── Dockerfile                              # Docker configuration
├── pom.xml                                 # Maven dependencies
└── README.md                               # This file
```

## Quick Start

### Prerequisites

#### Required
- **Java 21+**
- **Maven 3.8+**
- **Backend services running** - User, Match, Chat, Recommendation services on localhost

#### Optional
- **Redis** - Only needed for production (session storage)
- **Docker** - Only for production deployment

**Note**: For development, Redis is NOT required. Sessions are stored in-memory.

### Running Locally (Development)

**Step 1**: Ensure backend services are running first

```bash
# In separate terminals, start each service:
cd backend/user-service && mvn spring-boot:run
cd backend/match-service && mvn spring-boot:run
cd backend/chat-service && mvn spring-boot:run
cd backend/recommendation-service && mvn spring-boot:run
```

See [backend/QUICKSTART.md](../QUICKSTART.md) for complete backend setup instructions.

**Step 2**: Start Vaadin UI

```bash
# Start Vaadin UI service
cd backend/vaadin-ui-service
mvn spring-boot:run

# Access the UI
# Open browser: http://localhost:8090
```

### Running with Docker (Production Only)

```bash
# Build
mvn clean package
docker build -t vaadin-ui-service .

# Run
docker run -p 8090:8090 \
  -e SESSION_STORE=redis \
  -e REDIS_HOST=redis \
  -e USER_SERVICE_HOST=user-service \
  -e MATCH_SERVICE_HOST=match-service \
  -e CHAT_SERVICE_HOST=chat-service \
  -e RECOMMENDATION_SERVICE_HOST=recommendation-service \
  -e JWT_SECRET=your-secret \
  vaadin-ui-service
```

### Running with Docker Compose (Production)

```bash
# From project root
cd ../..
docker-compose up vaadin-ui

# See docker-compose.yml for full configuration
```

**Note**: Docker Compose is for production deployment. For development, use `mvn spring-boot:run` as shown above.

## Configuration

### application.yml

Key configuration properties:

```yaml
server:
  port: 8090

# Backend service URLs (all on localhost for development)
services:
  user-service:
    url: http://localhost:8081
  match-service:
    url: http://localhost:8082
  chat-service:
    url: http://localhost:8083
  recommendation-service:
    url: http://localhost:8084

spring:
  # Session storage (in-memory for dev, Redis for production)
  session:
    store-type: ${SESSION_STORE:none}  # 'none' for dev, 'redis' for prod

  redis:
    host: localhost
    port: 6379
```

### Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SERVER_PORT` | Service port | 8090 | No |
| `SESSION_STORE` | Session storage type | none (in-memory) | No (dev), Yes (prod: redis) |
| `REDIS_HOST` | Redis hostname (prod only) | localhost | No (dev), Yes (prod) |
| `USER_SERVICE_HOST` | User service host | localhost | Yes |
| `MATCH_SERVICE_HOST` | Match service host | localhost | Yes |
| `CHAT_SERVICE_HOST` | Chat service host | localhost | Yes |
| `RECOMMENDATION_SERVICE_HOST` | Recommendation service host | localhost | Yes |
| `JWT_SECRET` | JWT secret for validation | - | Yes |

**Development Note**: For local development, only `JWT_SECRET` needs to be set. All other variables have sensible defaults for localhost.

## Development

### Creating a New View

```java
package com.dating.ui.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "myview", layout = MainLayout.class)
@PageTitle("My View | POC Dating")
@PermitAll
public class MyView extends VerticalLayout {

    public MyView() {
        H1 title = new H1("My View");
        add(title);
    }
}
```

### Calling Backend Services

```java
@Service
public class MyService {
    @Autowired
    private MatchServiceClient matchClient;

    public User getProfile() {
        String token = SecurityUtils.getCurrentToken();
        return matchClient.getNextProfile("Bearer " + token);
    }
}
```

### Adding Real-time Updates

```java
@Route("chat")
@Push // Enables WebSocket
public class ChatView extends VerticalLayout {

    private void onNewMessage(Message msg) {
        getUI().ifPresent(ui -> ui.access(() -> {
            messageList.add(msg);
        }));
    }
}
```

## Views

### LoginView (Anonymous)
- Route: `/login`
- Authentication page
- Email + password form
- Navigate to RegisterView or SwipeView

### RegisterView (Anonymous)
- Route: `/register`
- New user registration
- Full registration form
- Auto-login after success

### SwipeView (Authenticated)
- Route: `/` (default)
- Main discovery interface
- Profile cards with swipe actions
- Like / Pass / Super Like buttons

### MatchesView (Authenticated)
- Route: `/matches`
- List of all matches
- Click to view conversation

### MessagesView (Authenticated)
- Route: `/messages`
- List of conversations
- Unread message counts
- Click to open chat

### ProfileView (Authenticated)
- Route: `/profile`
- Edit user profile
- Update bio, photos, preferences

## Security

### Authentication Flow

1. User logs in via LoginView
2. UserService calls backend API
3. JWT token stored in session (Redis)
4. SecurityUtils provides token for API calls
5. Session expires after 30 minutes

### Securing Views

```java
@PermitAll              // Authenticated users
@AnonymousAllowed       // Anyone
@RolesAllowed("ADMIN")  // Specific roles
```

## Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=SwipeViewTest

# UI tests (TestBench)
mvn verify -Pit
```

## Building

### Development Build

```bash
mvn clean package
```

### Production Build

```bash
mvn clean package -Pproduction
```

Production build:
- Optimizes frontend assets
- Minifies JavaScript/CSS
- Creates single JAR

## Deployment

### Docker

See [Dockerfile](Dockerfile)

### Cloud

Deploy JAR to:
- AWS Elastic Beanstalk
- Google Cloud Run
- Azure App Service
- Heroku

## Troubleshooting

### Backend Service Unreachable

**Problem**: Vaadin UI can't connect to backend services

**Solution**:
```bash
# Check if all backend services are running
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8082/actuator/health  # Match Service
curl http://localhost:8083/actuator/health  # Chat Service
curl http://localhost:8084/actuator/health  # Recommendation Service

# If any service is down, start it:
cd backend/user-service && mvn spring-boot:run
```

### Vaadin npm/pnpm Issues

**Problem**: Vaadin build fails with npm/pnpm errors

**Solution**:
```bash
# Clear Vaadin cache
rm -rf node_modules frontend/generated

# Rebuild
mvn clean install
```

### Session Issues (Development)

**Problem**: Sessions not persisting between requests

**Solution**:
In development, sessions are in-memory (no Redis needed). This is expected behavior.

If you need persistent sessions for testing:
1. Install Redis locally
2. Set environment variable: `export SESSION_STORE=redis`
3. Start Redis: `redis-server`
4. Restart Vaadin UI service

### Session Issues (Production)

**Problem**: Sessions not persisting in production

**Solution**:
```bash
# Verify Redis is running and accessible
redis-cli ping

# Check environment variables
echo $SESSION_STORE  # Should be 'redis'
echo $REDIS_HOST     # Should point to Redis

# Clear Redis sessions if needed
redis-cli FLUSHDB
```

### JWT Authentication Fails

**Problem**: Authentication not working across services

**Solution**:
Ensure JWT_SECRET is the same across ALL services:
```bash
# Set JWT_SECRET environment variable
export JWT_SECRET=your-secret-key-at-least-32-characters-long

# Restart all services with the same JWT_SECRET
```

## Performance

### Metrics

- Initial page load: 1-2s
- Subsequent interactions: 100-200ms
- WebSocket latency: <100ms
- Memory per session: 50-100KB
- Concurrent users: ~5,000 per instance

### Optimization Tips

1. **Enable production mode**
   ```yaml
   vaadin:
     productionMode: true
   ```

2. **Use Redis connection pooling**
3. **Enable GZip compression**
4. **Scale horizontally** (Redis handles sessions)

## Resources

- [Vaadin Documentation](https://vaadin.com/docs)
- [Vaadin Components](https://vaadin.com/components)
- [Vaadin Forum](https://vaadin.com/forum)
- [Spring Boot Docs](https://spring.io/projects/spring-boot)

## License

See parent project LICENSE

## Contributors

- POC Dating Team

---

**Last Updated:** 2025-11-11
**Status:** ✅ Active Development
