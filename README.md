# POC Dating Application

## 📋 Project Overview

A proof-of-concept dating application built with **Java Spring Boot microservices** architecture. This project demonstrates enterprise-level design patterns for modern dating platforms.

### Core Features
- User authentication and profile management
- Real-time matching algorithm
- WebSocket-based instant messaging
- Recommendation engine
- Location-based services (future)
- Scalable microservices architecture

---

## 🏗️ Architecture

### Microservices Structure
```
┌─────────────────────────────────────────┐
│          API Gateway (Port 8080)        │
│    - Request routing & load balancing   │
│    - Authentication enforcement         │
└─────────────┬───────────────────────────┘
              │
    ┌─────────┼─────────┬────────────┐
    │         │         │            │
┌───▼──┐  ┌──▼──┐  ┌───▼───┐  ┌────▼─────┐
│User  │  │Match│  │Chat   │  │Recommend-│
│Svc   │  │Svc  │  │Svc    │  │ation Svc │
│8081  │  │8082 │  │8083   │  │8084      │
└──────┘  └─────┘  └───────┘  └──────────┘
     │       │         │           │
     └───────┴─────────┴───────────┘
              │
    ┌─────────┼──────────┬──────────┐
    │         │          │          │
┌───▼──┐  ┌──▼──┐  ┌────▼──┐  ┌───▼───┐
│PgSQL │  │Redis│  │RabbitMQ│  │Cassandra
│      │  │Cache │  │EventBus│  │(optional)
└──────┘  └──────┘  └───────┘  └────────┘
```

### Technology Stack
- **Language:** Java 21+
- **Framework:** Spring Boot 3.x
- **Build:** Maven
- **Database:** PostgreSQL (primary), Redis (cache), RabbitMQ (message broker)
- **Frontend:** React + TypeScript (separate repo structure)
- **Containerization:** Docker & Docker Compose
- **Real-time:** WebSockets (Spring WebSocket)

---

## 📁 Project Structure

```
POC_Dating/
├── backend/                          # All Java microservices
│   ├── pom.xml                      # Parent POM for dependency management
│   ├── common-library/              # Shared code across services
│   ├── user-service/                # User auth, profiles, preferences
│   ├── match-service/               # Swiping, matching logic
│   ├── chat-service/                # Real-time messaging
│   ├── recommendation-service/      # ML/algorithm-based recommendations
│   ├── api-gateway/                 # Routing, load balancing, auth
│   └── docker/                      # Microservice-specific Docker configs
│
├── frontend/                         # React web application
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── docker/                      # Frontend Docker config
│
├── docker/                           # Docker Compose & orchestration
│   ├── docker-compose.yml           # Local development
│   ├── docker-compose.prod.yml      # Production
│   └── dockerignore
│
├── db/                               # Database files
│   ├── init/                        # Initial DB setup scripts
│   ├── migrations/                  # Liquibase/Flyway migrations
│   └── schemas/
│
├── docs/                             # Architecture & technical documentation
│   ├── ARCHITECTURE.md              # System design document
│   ├── API-SPECIFICATION.md         # REST API contracts
│   ├── DATABASE-SCHEMA.md           # Database design
│   ├── DEPLOYMENT.md                # Deployment guide
│   └── DEVELOPMENT.md               # Development setup guide
│
├── scripts/                          # Automation scripts
│   ├── setup.sh                     # Local development setup
│   ├── build-all.sh                 # Build all services
│   └── deploy.sh                    # Deployment automation
│
├── .env.example                      # Environment variables template
├── .gitignore                        # Git ignore rules
├── docker-compose.yml                # Root docker-compose for local dev
└── README.md                         # This file
```

---

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose
- Git

### Local Development
```bash
# Clone the repository
git clone <repo-url>
cd POC_Dating

# Setup environment
cp .env.example .env

# Start all services with Docker Compose
docker-compose up -d

# Frontend development
cd frontend
npm install
npm start
```

### Service Endpoints
- **API Gateway:** http://localhost:8080
- **User Service:** http://localhost:8081
- **Match Service:** http://localhost:8082
- **Chat Service:** http://localhost:8083
- **Recommendation Service:** http://localhost:8084

---

## 🧪 Testing Strategy

- **Unit Tests:** JUnit 5 + Mockito in each service
- **Integration Tests:** TestContainers for Docker integration
- **API Tests:** REST Assured
- **Frontend Tests:** Jest + React Testing Library

---

## 📚 Documentation

See `/docs/` directory for:
- System architecture design
- API specifications
- Database schema
- Development guidelines
- Deployment procedures

---

## 📝 Git Workflow

- **Main Branch:** `main` (production-ready)
- **Development:** `develop` (integration branch)
- **Feature Branches:** `feature/feature-name`
- **Bug Fixes:** `bugfix/bug-name`
- **Releases:** `release/version`

---

## 🔒 Security Considerations

- JWT-based authentication
- Spring Security for authorization
- HTTPS enforced (production)
- API rate limiting
- Input validation & sanitization
- CORS configuration
- Database encryption (production)

---

## 📊 Monitoring & Logging

- Spring Cloud Sleuth (distributed tracing)
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Prometheus metrics
- Spring Boot Actuator endpoints

---

## 📄 License

[Add your license here]

---

## 👥 Contributing

[Add contribution guidelines]

---

**Last Updated:** 2025-11-11
**Status:** Architecture Planning Phase
