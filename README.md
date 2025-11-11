# POC Dating Application

**Document Status:** ✅ **ACTIVE** - Current implementation using Vaadin
**Last Updated:** 2025-11-11

## 📋 Project Overview

A proof-of-concept dating application built with **Java Spring Boot microservices** architecture and **Vaadin full-stack UI**. This project demonstrates enterprise-level design patterns for modern dating platforms with 100% Java implementation.

### Core Features
- User authentication and profile management
- Real-time matching algorithm
- WebSocket-based instant messaging
- Recommendation engine
- Location-based services (future)
- Scalable microservices architecture

### Technology Decision: Vaadin UI
We chose **Vaadin** (pure Java UI framework) over React/TypeScript to:
- ✅ Leverage team's Java expertise
- ✅ Achieve 3-week MVP timeline
- ✅ Maintain type safety throughout the stack
- ✅ Understand every line of code
- 📋 See [docs/FRONTEND_OPTIONS_ANALYSIS.md](docs/FRONTEND_OPTIONS_ANALYSIS.md) for detailed comparison

---

## 🏗️ Architecture

### Microservices Structure
```
┌─────────────────────────────────────────┐
│     Vaadin UI Service (Port 8090)       │
│    - Pure Java web interface            │
│    - Calls backend via Feign/REST       │
│    - WebSocket integration (@Push)      │
└─────────────┬───────────────────────────┘
              │ REST/Feign
┌─────────────▼───────────────────────────┐
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

#### Backend
- **Language:** Java 21+
- **Framework:** Spring Boot 3.x
- **Build:** Maven
- **Database:** PostgreSQL (primary), Redis (cache), RabbitMQ (message broker)
- **Containerization:** Docker & Docker Compose
- **Real-time:** WebSockets (Spring WebSocket)

#### Frontend
- **Framework:** Vaadin 24.3 (Pure Java)
- **UI Components:** Vaadin Flow Components
- **Styling:** Lumo Theme (customizable)
- **Real-time:** Vaadin @Push (WebSocket/SSE)
- **Security:** Spring Security integration

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
│   ├── vaadin-ui-service/           # 🆕 Vaadin web UI (Pure Java!)
│   └── docker/                      # Microservice-specific Docker configs
│
├── frontend/                         # ⚠️ DEPRECATED - See vaadin-ui-service
│   └── [React files marked as reference only]
│
├── docker/                           # Docker Compose & orchestration
│   ├── docker-compose.yml           # Local development (updated for Vaadin)
│   ├── docker-compose.prod.yml      # Production
│   └── dockerignore
│
├── db/                               # Database files
│   ├── init/                        # Initial DB setup scripts
│   ├── migrations/                  # Liquibase/Flyway migrations
│   └── schemas/
│
├── docs/                             # Architecture & technical documentation
│   ├── ✅ ARCHITECTURE.md           # System design (updated for Vaadin)
│   ├── ✅ VAADIN_IMPLEMENTATION.md  # Vaadin setup and implementation guide
│   ├── 📋 FRONTEND_OPTIONS_ANALYSIS.md # Why Vaadin was chosen
│   ├── ✅ DEVELOPMENT.md            # Development guide (updated for Vaadin)
│   ├── ✅ DOCUMENT_INDEX.md         # Documentation organization
│   ├── API-SPECIFICATION.md         # REST API contracts
│   ├── DATABASE-SCHEMA.md           # Database design
│   └── DEPLOYMENT.md                # Deployment guide
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

### Key Changes for Vaadin Approach
- ✅ **Added:** `backend/vaadin-ui-service/` - Pure Java web UI
- ⚠️ **Deprecated:** `frontend/` directory (React/TypeScript) - kept for reference
- ✅ **Updated:** Documentation to reflect Vaadin architecture
- 📋 **New Docs:** Vaadin implementation guide and options analysis

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

# Build all services (including Vaadin UI)
cd backend
mvn clean install

# Start all services with Docker Compose
cd ..
docker-compose up -d

# Access the application
# Vaadin UI: http://localhost:8090
```

### Service Endpoints
- **Vaadin UI:** http://localhost:8090 (Main application)
- **API Gateway:** http://localhost:8080
- **User Service:** http://localhost:8081
- **Match Service:** http://localhost:8082
- **Chat Service:** http://localhost:8083
- **Recommendation Service:** http://localhost:8084

### Development Mode (without Docker)

```bash
# Terminal 1: Start databases
docker-compose up postgres redis rabbitmq

# Terminal 2: Start backend services
cd backend/user-service && mvn spring-boot:run

# Terminal 3: Start Vaadin UI
cd backend/vaadin-ui-service && mvn spring-boot:run

# Access: http://localhost:8090
```

---

## 🧪 Testing Strategy

- **Unit Tests:** JUnit 5 + Mockito in each service
- **Integration Tests:** TestContainers for Docker integration
- **API Tests:** REST Assured
- **UI Tests:** Vaadin TestBench (Java-based UI testing)
- **End-to-End:** Selenium WebDriver integration

---

## 📚 Documentation

### ✅ Active Documents (Vaadin Approach)
- **[docs/DOCUMENT_INDEX.md](docs/DOCUMENT_INDEX.md)** - Complete documentation index
- **[docs/VAADIN_IMPLEMENTATION.md](docs/VAADIN_IMPLEMENTATION.md)** - Vaadin setup guide
- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** - System architecture (updated for Vaadin)
- **[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)** - Development guide (updated for Vaadin)
- **[docs/DATABASE-SCHEMA.md](docs/DATABASE-SCHEMA.md)** - Database design
- **[docs/API-SPECIFICATION.md](docs/API-SPECIFICATION.md)** - REST API contracts
- **[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)** - Deployment procedures

### 📋 Reference Documents
- **[docs/FRONTEND_OPTIONS_ANALYSIS.md](docs/FRONTEND_OPTIONS_ANALYSIS.md)** - Frontend technology comparison

See `/docs/` directory for complete documentation.

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
