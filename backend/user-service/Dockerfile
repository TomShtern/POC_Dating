# User Service Dockerfile
#
# PURPOSE: Build containerized User Service for deployment
#
# MULTI-STAGE BUILD APPROACH:
# Stage 1: Build - Compile Java code, run tests, create JAR
# Stage 2: Runtime - Run JAR with minimal dependencies
#
# WHY MULTI-STAGE:
# - Reduces final image size (no Maven/compiler in runtime)
# - Build artifacts don't ship to production
# - Faster deployments
# - Security: No build tools exposed in production
#
# ALTERNATIVES:
# - Single-stage (not production-ready, larger images)
# - Buildpacks (convenient but less control)
# - Jib (good for Spring Boot, but harder to customize)
#
# RATIONALE:
# - Maven Stage: Builds JAR with all dependencies
# - Java Runtime Stage: Lightweight OpenJDK 21 slim
# - Health checks: Ensures container is responsive
# - User permissions: Non-root for security

# ========================================
# STAGE 1: BUILD
# ========================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy parent pom and common library first for better caching
COPY ../pom.xml /build/parent-pom.xml
COPY ../common-library /build/common-library

# Copy service pom and source
COPY pom.xml /build/
COPY src /build/src

# Build the service
RUN mvn -f parent-pom.xml install -DskipTests -pl common-library -am && \
    mvn clean package -DskipTests

# ========================================
# STAGE 2: RUNTIME
# ========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /build/target/user-service-*.jar /app/user-service.jar

# Non-root user for security
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser

USER appuser

# Expose service port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD sh -c 'curl -f http://localhost:8081/actuator/health || exit 1'

# Start application
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "user-service.jar"]

# Note: Application configuration comes from environment variables (.env or docker-compose.yml)
