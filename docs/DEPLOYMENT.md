# Deployment Guide - POC Dating Application

**Document Status:** ✅ **ACTIVE**
**Last Updated:** 2025-11-12
**Version:** 1.0

---

## Overview

This guide covers production deployment of the POC Dating Application using Docker Compose. For local development, see [backend/QUICKSTART.md](../backend/QUICKSTART.md) instead.

**Key Distinction:**
- **Development**: PostgreSQL on localhost, no Docker required
- **Production**: All services containerized with Docker Compose

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Docker Compose Deployment](#docker-compose-deployment)
3. [Environment Configuration](#environment-configuration)
4. [Building Services](#building-services)
5. [Starting Services](#starting-services)
6. [Verification](#verification)
7. [Monitoring](#monitoring)
8. [Troubleshooting](#troubleshooting)
9. [Cloud Deployment](#cloud-deployment-future)
10. [Scaling](#scaling)

---

## Prerequisites

### Required Software

- **Docker 20.10+** - [Install Docker](https://docs.docker.com/get-docker/)
- **Docker Compose 2.0+** - Usually included with Docker Desktop
- **Git** - To clone repository

### Verify Installation

```bash
# Check Docker
docker --version
# Output: Docker version 20.10.x or higher

# Check Docker Compose
docker compose version
# Output: Docker Compose version v2.x.x or higher

# Check Docker daemon is running
docker ps
# Should return list of containers (may be empty)
```

### System Requirements

**Minimum for POC:**
- CPU: 4 cores
- RAM: 8 GB
- Disk: 20 GB free space
- OS: Linux, macOS, or Windows with WSL2

**Recommended for Production:**
- CPU: 8+ cores
- RAM: 16+ GB
- Disk: 50+ GB SSD
- OS: Linux (Ubuntu 22.04 LTS recommended)

---

## Docker Compose Deployment

### Deployment Architecture

```
┌────────────────────────────────────────────┐
│  Docker Compose Network                    │
├────────────────────────────────────────────┤
│                                            │
│  ┌──────────────────────────────────────┐ │
│  │  Vaadin UI (8090)                    │ │
│  └──────────────┬───────────────────────┘ │
│                 │                          │
│  ┌──────────────▼───────────────────────┐ │
│  │  Backend Services                    │ │
│  │  - User Service (8081)               │ │
│  │  - Match Service (8082)              │ │
│  │  - Chat Service (8083)               │ │
│  │  - Recommendation Service (8084)     │ │
│  └──────────────┬───────────────────────┘ │
│                 │                          │
│  ┌──────────────▼───────────────────────┐ │
│  │  Data Layer                          │ │
│  │  - PostgreSQL (5432)                 │ │
│  │  - Redis (6379) [optional]           │ │
│  │  - RabbitMQ (5672) [optional]        │ │
│  └──────────────────────────────────────┘ │
│                                            │
└────────────────────────────────────────────┘
```

---

## Environment Configuration

### Step 1: Clone Repository

```bash
git clone <repository-url>
cd POC_Dating
```

### Step 2: Configure Environment Variables

Create `.env` file in project root:

```bash
cp .env.example .env
```

Edit `.env` file:

```bash
# Database Configuration
POSTGRES_DB=dating_db
POSTGRES_USER=dating_user
POSTGRES_PASSWORD=your-secure-password-here

# Redis Configuration (optional)
REDIS_HOST=redis
REDIS_PORT=6379

# RabbitMQ Configuration (optional)
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/

# Security
JWT_SECRET=your-secret-key-at-least-32-characters-long-change-in-production

# Logging
LOG_LEVEL=INFO
```

**Important Security Notes:**
- **Never commit `.env` to version control**
- Use strong passwords for production
- JWT_SECRET must be at least 32 characters
- Change all default passwords

---

## Building Services

### Build All Services

```bash
# Navigate to backend
cd backend

# Build all Maven projects
mvn clean install

# This will:
# - Compile all services
# - Run unit tests
# - Create JAR files
# - Build Docker images (if Dockerfile present)
```

### Build Individual Service

```bash
cd backend/user-service
mvn clean package
```

### Verify Build

```bash
# Check that JAR files exist
ls -lh backend/*/target/*.jar

# Should see:
# backend/user-service/target/user-service-1.0.0-SNAPSHOT.jar
# backend/match-service/target/match-service-1.0.0-SNAPSHOT.jar
# backend/chat-service/target/chat-service-1.0.0-SNAPSHOT.jar
# backend/recommendation-service/target/recommendation-service-1.0.0-SNAPSHOT.jar
# backend/vaadin-ui-service/target/vaadin-ui-service-1.0.0-SNAPSHOT.jar
```

---

## Starting Services

### Option 1: Start All Services

```bash
# Return to project root
cd /home/user/POC_Dating

# Start all services in background
docker-compose up -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f user-service
```

### Option 2: Start Specific Services

```bash
# Start only infrastructure
docker-compose up -d postgres redis rabbitmq

# Start specific microservice
docker-compose up -d user-service

# Start UI
docker-compose up -d vaadin-ui
```

### Option 3: Start in Foreground

```bash
# Start with logs in terminal (useful for debugging)
docker-compose up

# Stop with Ctrl+C
```

---

## Verification

### Check Running Containers

```bash
docker-compose ps

# Should show all services as "running"
```

### Health Checks

```bash
# PostgreSQL
docker exec dating_postgres psql -U dating_user -d dating_users -c "SELECT 1;"

# Redis (if enabled)
docker exec dating_redis redis-cli ping

# RabbitMQ (if enabled)
curl http://localhost:15672
# Login: guest/guest

# User Service
curl http://localhost:8081/actuator/health

# Match Service
curl http://localhost:8082/actuator/health

# Chat Service
curl http://localhost:8083/actuator/health

# Recommendation Service
curl http://localhost:8084/actuator/health

# Vaadin UI
curl http://localhost:8090
```

### Test Application

1. **Open Browser**: http://localhost:8090
2. **Register New User**: Create test account
3. **Login**: Verify authentication works
4. **Test Features**: Try swiping, matching, messaging

---

## Monitoring

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f user-service

# Last 100 lines
docker-compose logs --tail=100 user-service

# Logs since timestamp
docker-compose logs --since 2025-11-12T10:00:00 user-service
```

### Container Stats

```bash
# Real-time resource usage
docker stats

# Shows CPU, Memory, Network I/O for each container
```

### Database Inspection

```bash
# Connect to PostgreSQL
docker exec -it dating_postgres psql -U dating_user -d dating_users

# List databases
\l

# List tables
\dt

# Query users
SELECT * FROM users LIMIT 5;

# Exit
\q
```

### Redis Inspection (if enabled)

```bash
# Connect to Redis
docker exec -it dating_redis redis-cli

# List keys
KEYS *

# Get value
GET user:1234

# Exit
exit
```

---

## Stopping Services

### Stop All Services

```bash
# Stop containers (preserves data)
docker-compose stop

# Stop and remove containers (preserves data)
docker-compose down

# Stop and remove containers + volumes (DELETES ALL DATA)
docker-compose down -v
```

### Stop Specific Service

```bash
docker-compose stop user-service
```

### Restart Service

```bash
docker-compose restart user-service
```

---

## Troubleshooting

### Service Won't Start

**Problem**: Container exits immediately

**Solution**:
```bash
# Check logs
docker-compose logs user-service

# Common issues:
# - Port already in use
# - Database connection failed
# - Missing environment variables
# - Build failed
```

### Port Already in Use

**Problem**: `Bind for 0.0.0.0:8081 failed: port is already allocated`

**Solution**:
```bash
# Find process using port
lsof -i :8081  # Mac/Linux
netstat -ano | findstr :8081  # Windows

# Kill process or change port in docker-compose.yml
```

### Database Connection Failed

**Problem**: Services can't connect to PostgreSQL

**Solution**:
```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Check PostgreSQL logs
docker-compose logs postgres

# Verify credentials in .env match docker-compose.yml

# Test connection
docker exec dating_postgres psql -U dating_user -d dating_users -c "SELECT 1;"
```

### Out of Memory

**Problem**: Containers crash or slow performance

**Solution**:
```bash
# Increase Docker memory limit
# Docker Desktop → Settings → Resources → Memory → 8 GB or more

# Check memory usage
docker stats

# Reduce services running simultaneously
```

### Rebuild After Code Changes

```bash
# Rebuild specific service
cd backend/user-service
mvn clean package
docker-compose up -d --build user-service

# Rebuild all services
cd backend
mvn clean install
cd ..
docker-compose up -d --build
```

---

## Updating Deployment

### Update Code

```bash
# Pull latest code
git pull origin main

# Rebuild services
cd backend
mvn clean install

# Restart containers
cd ..
docker-compose down
docker-compose up -d
```

### Update Environment Variables

```bash
# Edit .env file
nano .env

# Restart affected services
docker-compose restart
```

### Database Migrations

```bash
# Stop services
docker-compose down

# Backup database
docker exec dating_postgres pg_dump -U dating_user dating_users > backup.sql

# Start services (schema will auto-update with ddl-auto: update)
docker-compose up -d

# Or run manual migration
docker exec -i dating_postgres psql -U dating_user -d dating_users < migration.sql
```

---

## Cloud Deployment (Future)

### AWS Deployment

**Option 1: EC2 Instance**
1. Launch EC2 instance (Ubuntu 22.04)
2. Install Docker and Docker Compose
3. Clone repository
4. Configure environment variables
5. Run `docker-compose up -d`
6. Configure security groups (ports 8081-8084, 8090)

**Option 2: ECS (Elastic Container Service)**
1. Push images to ECR
2. Create ECS cluster
3. Define task definitions
4. Create services
5. Configure load balancer
6. Set up RDS for PostgreSQL

**Option 3: EKS (Kubernetes)**
1. Create EKS cluster
2. Deploy services as Kubernetes deployments
3. Configure ingress
4. Set up managed PostgreSQL (RDS)
5. Configure auto-scaling

### Google Cloud Platform

**Option 1: Compute Engine VM**
Similar to AWS EC2 approach

**Option 2: Cloud Run**
1. Containerize services
2. Push to Container Registry
3. Deploy to Cloud Run
4. Use Cloud SQL for PostgreSQL
5. Configure Cloud Load Balancing

### Azure

**Option 1: Azure VM**
Similar to AWS EC2 approach

**Option 2: Azure Container Instances**
1. Push images to Azure Container Registry
2. Deploy to Container Instances
3. Use Azure Database for PostgreSQL
4. Configure Azure Front Door

---

## Scaling

### Vertical Scaling

**Increase resources for existing containers:**

Edit `docker-compose.yml`:

```yaml
services:
  user-service:
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2G
```

### Horizontal Scaling

**Run multiple instances of a service:**

```bash
# Scale user-service to 3 instances
docker-compose up -d --scale user-service=3

# View running instances
docker-compose ps
```

**Note**: Requires load balancer for production

### Database Scaling

**Read Replicas:**
1. Set up PostgreSQL streaming replication
2. Configure read-only replicas
3. Route read queries to replicas

**Connection Pooling:**
1. Use PgBouncer
2. Configure in docker-compose.yml
3. Point services to PgBouncer

---

## Backup and Recovery

### Database Backup

```bash
# Backup all databases
docker exec dating_postgres pg_dumpall -U postgres > backup_all.sql

# Backup specific database
docker exec dating_postgres pg_dump -U dating_user dating_users > backup_users.sql

# Automated daily backup
cat > backup.sh <<'EOF'
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
docker exec dating_postgres pg_dumpall -U postgres > backup_${DATE}.sql
find . -name "backup_*.sql" -mtime +7 -delete  # Keep 7 days
EOF

chmod +x backup.sh
# Add to crontab: 0 2 * * * /path/to/backup.sh
```

### Restore Database

```bash
# Stop services
docker-compose down

# Start only PostgreSQL
docker-compose up -d postgres

# Restore
docker exec -i dating_postgres psql -U postgres < backup_all.sql

# Start all services
docker-compose up -d
```

---

## Security Best Practices

### Production Checklist

- [ ] Change all default passwords
- [ ] Use strong JWT_SECRET (32+ characters)
- [ ] Enable HTTPS (use reverse proxy like Nginx)
- [ ] Configure firewall (allow only necessary ports)
- [ ] Enable PostgreSQL SSL
- [ ] Use secrets management (AWS Secrets Manager, Azure Key Vault)
- [ ] Regular security updates
- [ ] Enable audit logging
- [ ] Implement rate limiting
- [ ] Configure CORS properly
- [ ] Use non-root users in containers
- [ ] Scan images for vulnerabilities

### SSL/TLS Configuration

Use Nginx reverse proxy with Let's Encrypt:

```yaml
# Add to docker-compose.yml
nginx:
  image: nginx:alpine
  ports:
    - "80:80"
    - "443:443"
  volumes:
    - ./nginx.conf:/etc/nginx/nginx.conf
    - ./ssl:/etc/nginx/ssl
  depends_on:
    - vaadin-ui
```

---

## Performance Tuning

### JVM Tuning

Edit Dockerfiles:

```dockerfile
ENTRYPOINT ["java", \
  "-Xms512m", \
  "-Xmx2g", \
  "-XX:+UseG1GC", \
  "-jar", "app.jar"]
```

### PostgreSQL Tuning

```yaml
postgres:
  command: >
    postgres
    -c shared_buffers=256MB
    -c max_connections=200
    -c work_mem=4MB
```

---

## Support

### Documentation

- Development Setup: [backend/QUICKSTART.md](../backend/QUICKSTART.md)
- Architecture: [ARCHITECTURE.md](ARCHITECTURE.md)
- API Documentation: [API-SPECIFICATION.md](API-SPECIFICATION.md)

### Common Issues

See [DEVELOPMENT.md - Troubleshooting](DEVELOPMENT.md#troubleshooting)

---

**Document Version:** 1.0
**Last Updated:** 2025-11-12
**Status:** ✅ ACTIVE
