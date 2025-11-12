# POC Dating - Quick Start Guide

This guide will help you get all microservices up and running with PostgreSQL as the database.

## Prerequisites

### Required Software

1. **Java 21+**
   ```bash
   java -version
   # Should show Java 21 or higher
   ```

2. **Maven 3.8+**
   ```bash
   mvn -version
   # Should show Maven 3.8 or higher
   ```

3. **PostgreSQL 14+**
   - See installation instructions below

### PostgreSQL Installation

#### Linux (Ubuntu/Debian)
```bash
# Install PostgreSQL
sudo apt-get update
sudo apt-get install postgresql postgresql-contrib

# Start PostgreSQL service
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Verify installation
sudo systemctl status postgresql
```

#### macOS (Homebrew)
```bash
# Install PostgreSQL
brew install postgresql@14

# Start PostgreSQL service
brew services start postgresql@14

# Verify installation
brew services list | grep postgresql
```

#### Windows
1. Download installer from [postgresql.org](https://www.postgresql.org/download/windows/)
2. Run installer and follow setup wizard
3. Remember the password you set for the postgres user
4. PostgreSQL service should start automatically

## Database Setup

### Option 1: Quick Setup (Recommended)

Run the provided SQL script to create all databases:

```bash
# On Linux/Mac
sudo -u postgres psql -f /home/user/POC_Dating/backend/setup-databases.sql

# On Windows (PowerShell/CMD)
psql -U postgres -f C:\path\to\POC_Dating\backend\setup-databases.sql
```

When prompted, enter the postgres user password.

### Option 2: Manual Setup

Connect to PostgreSQL and create databases manually:

```bash
# Connect to PostgreSQL
sudo -u postgres psql

# Or on Windows/Mac:
psql -U postgres
```

Then run these commands:

```sql
CREATE DATABASE dating_users;
CREATE DATABASE dating_matches;
CREATE DATABASE dating_chat;
CREATE DATABASE dating_recommendations;

-- Verify databases were created
\l

-- Exit
\q
```

## Environment Variables (Optional)

By default, services use:
- **Username**: `postgres`
- **Password**: `postgres`
- **Host**: `localhost`
- **Port**: `5432`

If your PostgreSQL setup is different, set these environment variables:

```bash
# Linux/Mac
export DB_PASSWORD=your_postgres_password
export JWT_SECRET=your-secret-key-at-least-32-characters-long

# Windows (PowerShell)
$env:DB_PASSWORD="your_postgres_password"
$env:JWT_SECRET="your-secret-key-at-least-32-characters-long"

# Windows (CMD)
set DB_PASSWORD=your_postgres_password
set JWT_SECRET=your-secret-key-at-least-32-characters-long
```

**Important**: All services must use the **same JWT_SECRET** for authentication to work across services.

## Starting the Services

### 1. User Service (Port 8081)

```bash
cd /home/user/POC_Dating/backend/user-service
mvn spring-boot:run
```

Wait for the service to start (look for "Started UserServiceApplication").

**Test it:**
```bash
curl http://localhost:8081/actuator/health
# Should return: {"status":"UP"}
```

### 2. Match Service (Port 8082)

Open a new terminal:

```bash
cd /home/user/POC_Dating/backend/match-service
mvn spring-boot:run
```

**Test it:**
```bash
curl http://localhost:8082/actuator/health
```

### 3. Chat Service (Port 8083)

Open a new terminal:

```bash
cd /home/user/POC_Dating/backend/chat-service
mvn spring-boot:run
```

**Test it:**
```bash
curl http://localhost:8083/actuator/health
```

### 4. Recommendation Service (Port 8084)

Open a new terminal:

```bash
cd /home/user/POC_Dating/backend/recommendation-service
mvn spring-boot:run
```

**Test it:**
```bash
curl http://localhost:8084/actuator/health
```

## Testing the Application

### 1. Register a User

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

This will return a JWT token. Save it for subsequent requests.

### 2. Login

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!"
  }'
```

Save the `accessToken` from the response.

### 3. Get User Profile

```bash
TOKEN="your_access_token_here"

curl http://localhost:8081/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Create a Match (requires 2 users)

First, register a second user, then:

```bash
curl -X POST http://localhost:8082/api/matches/swipe \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "targetUserId": 2,
    "swipeType": "LIKE"
  }'
```

### 5. Get Recommendations

```bash
curl http://localhost:8084/api/recommendations \
  -H "Authorization: Bearer $TOKEN"
```

## Troubleshooting

### PostgreSQL Connection Issues

**Problem**: `Connection refused` or `could not connect to server`

**Solution**:
```bash
# Check if PostgreSQL is running
sudo systemctl status postgresql  # Linux
brew services list                # Mac

# Restart PostgreSQL if needed
sudo systemctl restart postgresql  # Linux
brew services restart postgresql@14  # Mac
```

**Problem**: `password authentication failed for user "postgres"`

**Solution**:
Set the correct password via environment variable:
```bash
export DB_PASSWORD=your_actual_password
```

### Database Not Found

**Problem**: `database "dating_users" does not exist`

**Solution**:
Run the database setup script again (see Database Setup section above).

### Service Won't Start

**Problem**: `Port 8081 already in use`

**Solution**:
```bash
# Find process using the port
lsof -i :8081  # Mac/Linux
netstat -ano | findstr :8081  # Windows

# Kill the process or use a different port
kill -9 <PID>  # Mac/Linux
```

### Schema Issues

**Problem**: `Table doesn't exist` or schema errors

**Solution**:
The application uses `ddl-auto: update`, which should auto-create tables. If this fails:

1. Stop the service
2. Drop and recreate the database:
   ```sql
   DROP DATABASE dating_users;
   CREATE DATABASE dating_users;
   ```
3. Restart the service

## Using H2 Instead of PostgreSQL

If you want to quickly test without PostgreSQL, use the dev profile:

```bash
cd /home/user/POC_Dating/backend/user-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

This uses an in-memory H2 database (data is lost when service stops).

**H2 Console**: http://localhost:8081/h2-console
- JDBC URL: `jdbc:h2:mem:dating_users_dev`
- Username: `sa`
- Password: (leave empty)

## Optional Services (Not Required)

### Redis (for advanced caching)

```bash
# Install
sudo apt-get install redis-server  # Linux
brew install redis                  # Mac

# Start
sudo systemctl start redis          # Linux
brew services start redis           # Mac

# Enable Redis in services
export CACHE_TYPE=redis
```

### RabbitMQ (for inter-service messaging)

```bash
# Install
sudo apt-get install rabbitmq-server  # Linux
brew install rabbitmq                 # Mac

# Start
sudo systemctl start rabbitmq-server  # Linux
brew services start rabbitmq          # Mac
```

## Service Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    POC Dating Backend                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  User Service (8081)         Match Service (8082)           │
│  ├── Authentication          ├── Swipe handling             │
│  ├── User profiles           └── Match detection            │
│  └── Preferences                                             │
│                                                              │
│  Chat Service (8083)         Recommendation Service (8084)  │
│  ├── Real-time messaging     ├── User preferences           │
│  ├── WebSocket support       └── Recommendation algorithm   │
│  └── Conversation mgmt                                       │
│                                                              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  PostgreSQL (localhost:5432)                 │
├─────────────────────────────────────────────────────────────┤
│  dating_users | dating_matches | dating_chat | dating_recs  │
└─────────────────────────────────────────────────────────────┘
```

## Database Schema Auto-Creation

All services use `spring.jpa.hibernate.ddl-auto: update`, which means:

- Tables are **automatically created** on first startup
- Schema is **automatically updated** when you add new fields
- Existing data is **preserved** during updates
- No manual migrations needed for development

## Production Profile

For production deployment with stricter settings (validate schema, enable Redis):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

**Note**: Production profile uses `ddl-auto: validate`, which requires manual schema management.

## Next Steps

1. **Explore the APIs**: Check each service's README for detailed API documentation
2. **Add more features**: Modify the services to add new functionality
3. **Deploy**: Containerize with Docker for deployment
4. **Monitor**: Use Spring Boot Actuator endpoints for monitoring

## Additional Resources

- User Service: `/home/user/POC_Dating/backend/user-service/README.md`
- Match Service: `/home/user/POC_Dating/backend/match-service/README.md`
- Chat Service: `/home/user/POC_Dating/backend/chat-service/README.md`
- Recommendation Service: `/home/user/POC_Dating/backend/recommendation-service/README.md`

## Need Help?

1. Check service logs for error messages
2. Verify PostgreSQL is running and accessible
3. Ensure all databases are created
4. Confirm JWT_SECRET is the same across all services
5. Check that ports 8081-8084 are not in use

## Common Port Usage

| Service               | Port | Database                  |
|-----------------------|------|---------------------------|
| User Service          | 8081 | dating_users              |
| Match Service         | 8082 | dating_matches            |
| Chat Service          | 8083 | dating_chat               |
| Recommendation Service| 8084 | dating_recommendations    |
| PostgreSQL            | 5432 | (all databases)           |
| Redis (optional)      | 6379 | -                         |
| RabbitMQ (optional)   | 5672 | -                         |
