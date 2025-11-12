# Chat Service Dockerfile - Multi-stage build

# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy parent POM and common library first
COPY ../pom.xml /build/../pom.xml
COPY ../common-library /build/../common-library

# Copy chat-service specific files
COPY pom.xml /build/
COPY src /build/src

# Build the application
RUN mvn clean package -DskipTests -q

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/target/chat-service-*.jar /app/chat-service.jar

# Create non-root user for security
RUN addgroup -g 1000 appuser && adduser -D -u 1000 -G appuser appuser

# Switch to non-root user
USER appuser

# Expose service port
EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD sh -c 'wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1'

# Set JVM options and start the application
ENTRYPOINT ["java", \
    "-Xmx512m", \
    "-Xms256m", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "chat-service.jar"]
