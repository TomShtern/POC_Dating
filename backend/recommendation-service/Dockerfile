# Recommendation Service Dockerfile - Multi-stage build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml /build/
COPY ../pom.xml /build/../
COPY ../common-library /build/../common-library
RUN mvn clean package -DskipTests -q 2>/dev/null

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/recommendation-service-*.jar /app/recommendation-service.jar
RUN addgroup -g 1000 appuser && adduser -D -u 1000 -G appuser appuser
USER appuser
EXPOSE 8084
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD sh -c 'curl -f http://localhost:8084/actuator/health || exit 1'
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "recommendation-service.jar"]
