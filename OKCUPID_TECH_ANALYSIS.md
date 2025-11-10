# OkCupid - Technical Architecture & Stack Analysis

## Overview
OkCupid is one of the pioneering online dating platforms, known for its algorithm-driven matching system. The platform is owned by Match Group and has undergone significant technical evolution, particularly transitioning from a custom C++ web server to modern web technologies.

---

## Technology Stack

### Mobile Applications
- **iOS**: Native development
  - **Language**: Swift (migrated from Objective-C)
  - **Package Management**: Swift Package Manager (SPM)
  - **UI**: Exploring SwiftUI integration
- **Android**: Native development
  - **Language**: Java (transitioning to Kotlin)
  - **Previous Approach**: Hybrid app with web views (moved away from this)

### Frontend/Web
- **Framework**: React with Redux
- **Architecture**: Single-page applications (SPA) for desktop and mobile web
- **Legacy**: Multi-page apps (being upgraded feature-by-feature to React)
- **API**: GraphQL (migrated from REST)

### Backend Languages & Frameworks
- **Modern Stack**:
  - **Primary**: Node.js with Apollo Server
  - **Framework**: Express
  - **API**: GraphQL (greenfield project in separate repository)
- **Legacy Stack**:
  - **Language**: C++
  - **Web Server**: OKWS (OK Web Server) - custom-built
  - **Architecture**: Single-threaded, event-driven
  - **Languages**: JavaScript, Python, Git
- **Containerization**: Docker

### Database & Caching
- **Not explicitly documented** in public sources
- **Industry Standard Assumptions**: MySQL or PostgreSQL likely for relational data
- **Possible**: MongoDB for flexible document storage (based on similar apps)

### Cloud Infrastructure
- **Current**: Migrating to AWS
- **Approach**: Rewriting applications using Terraform (infrastructure-as-code)
- **Future Goal**: Operate entirely on AWS
- **Identity Management**: Okta with AWS SSO
- **Preference**: Linux infrastructure over Windows

### Data & Analytics Stack
- **Event Collection**: mParticle
- **Business Intelligence**: Looker
- **Product Intelligence**: Unnamed platform
- **Flow**: mParticle → stores customer event data → Looker for reporting

### API Architecture
- **Current**: GraphQL (official API)
- **Clients**: iOS app, Android app, desktop web, mobile web
- **Legacy**: REST API (deprecated)
- **Philosophy**: Greenfield GraphQL deployment separate from legacy backend

---

## Architecture History & Evolution

### Legacy: OKWS (The OK Web Server)
**What It Is:**
- Custom C++ web server built specifically for OkCupid
- Specialized for building fast and secure web services
- Open-sourced on GitHub

**Architecture Principles:**
- Each web service runs as a single process (e.g., 'search', 'newsletter-subscribe')
- Single-threaded, event-driven model
- Helper processes for: traffic routing, crash recovery, HTTP logging, template access
- **No synchronization issues** (avoids multi-threading complexity)

**Performance:**
- So efficient they supported millions of users on <50 machines (as of 2012)
- Documented in USENIX papers: "Building Secure High-Performance Web Services With OKWS" (2004)
- Commercial experience showed reduced hardware and system management costs

**Current Status:**
- OkCupid.com still runs OKWS (for legacy endpoints)
- Not widely adopted outside OkCupid
- Likely being phased out during AWS migration

### Modern: GraphQL Migration
**Why GraphQL:**
- Better performance than REST for complex queries
- Reduces over-fetching and under-fetching
- Single endpoint simplifies client development
- Type safety and introspection

**Implementation:**
- Built with Apollo Server (Node.js)
- Separate repository and deployment from backend
- Gradual adoption across all clients
- Feature-by-feature migration of React apps

---

## Technical Review & Assessment

### Strengths

#### 1. **OKWS: Brilliant Engineering (for its time)**
- Building a custom web server in C++ shows serious engineering chops
- Event-driven, single-threaded model avoided concurrency nightmares
- **Exceptional performance**: Millions of users on <50 machines
- Security-focused design from the ground up
- Academic validation through USENIX papers

**This was genuinely innovative** in the early 2000s when most dating sites used PHP/Apache.

#### 2. **GraphQL Migration**
- Moving from REST to GraphQL is a smart, modern choice
- Greenfield approach (separate repo) reduces migration risk
- Apollo Server is battle-tested and well-supported
- Improves frontend developer experience significantly

#### 3. **Data-Driven Culture**
- Extensive A/B testing using analytics platform
- mParticle + Looker stack is solid for product analytics
- Removes ambiguity and subjectivity from product decisions
- **Engineering excellence**: Decisions based on data, not opinions

#### 4. **Native Mobile Apps**
- Moved away from hybrid webview approach
- Native Swift/Kotlin provides better performance and UX
- Shows willingness to abandon technical debt

#### 5. **Infrastructure-as-Code (Terraform)**
- Using Terraform for AWS migration is best practice
- Enables reproducible infrastructure
- Version-controlled infrastructure changes
- Multi-cloud portable (even if not currently multi-cloud)

#### 6. **Progressive Modernization**
- Upgrading React feature-by-feature rather than big-bang rewrite
- Pragmatic approach to technical debt
- Allows continuous delivery during migration

### Weaknesses & Limitations

#### 1. **OKWS: Technical Debt Nightmare**
While innovative in 2004, OKWS is now a massive liability:

**Language**: C++ for web services is outdated
- Hard to hire C++ web developers (most have moved to backend languages)
- Slow development velocity compared to Python/Go/Node.js
- Higher bug risk (memory management, segfaults)
- **No modern framework ecosystem**

**Event-Driven, Single-Threaded Model**:
- Can't utilize multi-core CPUs effectively
- One blocking operation stalls the entire process
- **Performance ceiling**: Can't scale vertically on modern hardware

**Process-Per-Service Architecture**:
- Primitive compared to modern microservices
- Inter-process communication more complex than HTTP/gRPC
- Limited observability compared to modern service meshes
- **Operational complexity** without modern tooling

**Security**:
- OKWS papers are from 2004 - security landscape has changed
- Modern frameworks have built-in protections (CSRF, XSS, SQL injection)
- Custom C++ server requires custom security audits

**Verdict**: OKWS was a competitive advantage in 2004-2010. By 2025, it's 15-20 years of technical debt.

#### 2. **Dual Architecture During Migration**
- Running OKWS (C++) + GraphQL (Node.js) simultaneously
- **Two systems to maintain** during AWS migration
- Data synchronization challenges
- Increased operational complexity
- Risk of incomplete migration (10+ years is too long)

#### 3. **Database Strategy Unknown**
- No public documentation of database choices
- **Red flag**: Either they're embarrassed or it's fragmented
- Likely running multiple databases during migration
- Data migration from legacy to modern stack is high-risk

#### 4. **AWS Migration Timeline**
- Started in 2021-2022, still ongoing in 2025
- **4+ years for cloud migration is too slow**
- Indicates either:
  - Extremely complex legacy system
  - Insufficient engineering resources
  - Poor migration planning
- Extended dual-operation increases costs

#### 5. **Not Invented Here (NIH) Syndrome**
- Building OKWS instead of using nginx/Apache
- While it worked, **huge opportunity cost**
- Engineering effort on infrastructure instead of product
- **Maintenance burden** for decades

#### 6. **Match Group Ownership Effects**
- Share infrastructure with Tinder (TAG API Gateway)
- **Loss of technical independence**
- May be forced to adopt Match Group standards
- Could explain slow migration (coordinating across portfolio)

#### 7. **Limited Public Technical Content**
- Few engineering blog posts compared to Tinder or Bumble
- **Less transparency** about technical decisions
- Harder to attract top engineering talent
- May indicate less mature engineering culture

---

## What Could Be Better

### 1. **Accelerate OKWS Retirement**
- **Set aggressive deadline**: Complete C++ → Node.js/Go migration in 12 months
- Allocate 50% of backend team to migration
- Accept some technical debt to move faster
- **Cost of delay**: Every year on OKWS costs millions in engineering productivity

### 2. **Choose Modern Backend Language**
- **If staying with Node.js**: Acceptable for I/O-bound workloads
- **Better choice**: Go or Rust for backend services
  - Better performance than Node.js
  - Compiled languages = fewer runtime errors
  - Better CPU utilization for algorithmic matching
- **Avoid**: Staying on C++ (maintenance nightmare)

### 3. **Database Modernization**
- **PostgreSQL** as primary database:
  - Excellent for complex queries (matching algorithms)
  - ACID compliance for critical user data
  - JSON support for flexibility
  - Strong community and tooling
- **Redis** for caching and real-time features
- **Elasticsearch** for user search
- **S3** for media storage

### 4. **Complete AWS Migration**
- Finish migration in 2025 (not 2026+)
- Use managed services aggressively:
  - RDS for PostgreSQL
  - ElastiCache for Redis
  - ECS/Fargate for containers (easier than Kubernetes)
  - Lambda for event-driven workloads

### 5. **Service Architecture**
- Avoid Tinder's mistake of 500+ microservices
- **Target: 20-30 well-defined services**:
  - User service
  - Matching service
  - Messaging service
  - Profile service
  - Search service
  - etc.
- Use API Gateway (AWS API Gateway or Kong)
- Implement service mesh (Istio/Envoy) only if needed

### 6. **Embrace Match Group Shared Infrastructure**
- Using TAG (Tinder's API Gateway) could be a win
- Share monitoring, logging, security tooling
- **Reduce redundant engineering effort**
- Focus OkCupid engineers on product differentiation, not infrastructure

### 7. **Engineering Blog & Open Source**
- Document the OKWS → modern stack migration
- **Case study value**: "How we migrated from C++ to Node.js/Go"
- Open source migration tools
- Attract talent who want to work on interesting technical challenges

---

## Stack Choices: Honest Assessment

### What They Got Right
- ✅ GraphQL adoption for modern API
- ✅ React/Redux for web frontend
- ✅ Native mobile apps (Swift/Kotlin)
- ✅ Terraform for infrastructure-as-code
- ✅ Data-driven culture (A/B testing)

### What's Historical Baggage
- 🕰️ OKWS (C++ web server) - was brilliant, now liability
- 🕰️ Multi-page web apps - being migrated to React SPAs
- 🕰️ REST API - being replaced by GraphQL

### What's Problematic
- ❌ Slow AWS migration (4+ years and counting)
- ❌ Unclear database strategy
- ❌ Dual architecture complexity (legacy + modern)
- ❌ Limited public technical documentation

### What's Unknown
- ❓ Current database technology
- ❓ Real-time messaging architecture
- ❓ Service mesh / microservices approach
- ❓ CI/CD pipeline details
- ❓ Monitoring and observability stack

---

## Comparison to Competitors

| Aspect | OkCupid | Tinder | Bumble |
|--------|---------|--------|--------|
| Backend Language | Node.js (migrating from C++) | Node.js, Java, Scala | Java, Kotlin, Python, PHP, Node.js |
| Mobile | Swift, Kotlin | Swift, Kotlin | Swift, Kotlin |
| API | GraphQL | REST (via TAG) | Unknown |
| Database | Unknown | MongoDB/DynamoDB | DynamoDB, MongoDB, PostgreSQL |
| Cloud | AWS (migrating) | AWS (100%) | AWS (Bumble 2.0) |
| Microservices | Unknown | 500+ | Microservice-based |
| Custom Infra | OKWS (legacy) | TAG (API Gateway) | Minimal |

**OkCupid's Differentiator**: Legacy C++ server (OKWS) is unique but not an advantage.

---

## Conclusion

OkCupid represents a company **transitioning from pioneering technical innovation to modern industry standards**. OKWS was genuinely impressive in 2004, but 20+ years later, it's the anchor holding them back.

**Grade: C+**

**Strengths**: Data-driven culture, GraphQL migration, native mobile apps, infrastructure-as-code approach, historical innovation (OKWS).

**Critical Issues**: Extremely slow migration from legacy C++ stack, 4+ years to move to AWS, unclear database strategy, dual architecture complexity, limited technical transparency.

**Biggest Risk**: The OKWS → modern stack migration could take another 2-3 years, during which time Tinder and Bumble will continue innovating. Every year spent migrating is a year not spent on product differentiation.

**Key Takeaway**: OkCupid's engineering team was ahead of its time in 2004 (custom C++ server, event-driven architecture, security focus). But they **over-optimized for performance at the expense of maintainability**, and now they're paying the price. The migration to modern stack (GraphQL, Node.js, AWS) is the right move but should have started 5 years earlier.

**Lesson for Engineers**: Building custom infrastructure gives you an edge initially but becomes technical debt within 5-10 years. Unless you're Google-scale, use boring, proven technologies. OKWS is a cautionary tale about NIH syndrome.

**Recommendation**: Declare technical bankruptcy on OKWS. Set a hard deadline (end of 2025), freeze feature development if necessary, and get off C++ entirely. The longer this drags on, the more competitive advantage OkCupid loses to Hinge and Bumble.
