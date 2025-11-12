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
│  Vaadin UI Service (Port 8090)          │
│  - Pure Java web interface (optional)   │
│  - Calls backend via Feign/REST         │
│  - WebSocket integration (@Push)        │
└─────────────┬───────────────────────────┘
              │ REST/Feign
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
    ┌─────────▼──────────┐
    │  PostgreSQL        │
    │  localhost:5432    │
    │  (4 databases)     │
    └────────────────────┘

Optional for Advanced Features:
┌────────┐  ┌──────────┐
│ Redis  │  │ RabbitMQ │
│ :6379  │  │ :5672    │
└────────┘  └──────────┘
```

### Technology Stack

#### Backend
- **Language:** Java 21+
- **Framework:** Spring Boot 3.x
- **Build:** Maven
- **Database:** PostgreSQL (primary, runs on localhost)
- **Cache:** Redis (optional, for advanced features)
- **Message Broker:** RabbitMQ (optional, for event-driven features)
- **Deployment:** Docker & Docker Compose (production only, not required for development)
- **Real-time:** WebSockets (Spring WebSocket)

#### Frontend
- **Framework:** Vaadin 24.3 (Pure Java)
- **UI Components:** Vaadin Flow Components
- **Styling:** Lumo Theme (customizable)
- **Real-time:** Vaadin @Push (WebSocket/SSE)
- **Security:** Spring Security integration

#### Development Setup
- **Local Development:** PostgreSQL on localhost (no Docker required)
- **Testing:** H2 in-memory database via `dev` profile
- **Production:** Docker Compose with all services containerized

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

#### Required
- **Java 21+** - [Download](https://adoptium.net/)
- **Maven 3.8+** - [Download](https://maven.apache.org/download.cgi)
- **PostgreSQL 14+** - [Installation Guide](#postgresql-installation)
- **Git** - Version control

#### Optional
- **Docker & Docker Compose** - Only for production deployment (not required for development)
- **Redis** - Optional caching (services work without it)
- **RabbitMQ** - Optional messaging (services work without it)

### PostgreSQL Installation

#### Linux (Ubuntu/Debian)
```bash
sudo apt-get update
sudo apt-get install postgresql postgresql-contrib
sudo systemctl start postgresql
```

#### macOS (Homebrew)
```bash
brew install postgresql@14
brew services start postgresql@14
```

#### Windows
Download installer from [postgresql.org](https://www.postgresql.org/download/windows/) and follow the setup wizard.

### Database Setup

Run the provided setup script to create all databases:

```bash
# Linux/Mac
sudo -u postgres psql -f backend/setup-databases.sql

# Windows
psql -U postgres -f backend/setup-databases.sql
```

This creates: `dating_users`, `dating_matches`, `dating_chat`, `dating_recommendations`

### Local Development (PostgreSQL-First)

**For detailed setup instructions, see [backend/QUICKSTART.md](backend/QUICKSTART.md)**

```bash
# Clone the repository
git clone <repo-url>
cd POC_Dating

# Build all services
cd backend
mvn clean install

# Start each service in a separate terminal
# Terminal 1: User Service
cd backend/user-service && mvn spring-boot:run

# Terminal 2: Match Service
cd backend/match-service && mvn spring-boot:run

# Terminal 3: Chat Service
cd backend/chat-service && mvn spring-boot:run

# Terminal 4: Recommendation Service
cd backend/recommendation-service && mvn spring-boot:run

# Terminal 5: Vaadin UI (Optional - if you need the web interface)
cd backend/vaadin-ui-service && mvn spring-boot:run
```

### Service Endpoints
- **Vaadin UI:** http://localhost:8090 (Web interface - optional)
- **User Service:** http://localhost:8081 (Core service)
- **Match Service:** http://localhost:8082
- **Chat Service:** http://localhost:8083
- **Recommendation Service:** http://localhost:8084

### Alternative: Quick Testing with H2

For quick testing without PostgreSQL setup, use the dev profile:

```bash
cd backend/user-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

This uses in-memory H2 database (data is lost on restart).

### Production Deployment (Docker)

For production deployment with all services containerized:

```bash
# Build all services
cd backend
mvn clean install

# Start with Docker Compose
cd ..
docker-compose up -d
```

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) for detailed deployment instructions.

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
