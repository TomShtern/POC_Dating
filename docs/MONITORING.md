# Monitoring & Observability Guide

This document describes the monitoring and observability setup for the POC Dating Application.

## Overview

The application uses Spring Boot Actuator with Micrometer for metrics collection and Prometheus export. Each service exposes health checks, metrics, and structured logging.

## Quick Start

### Check Service Health
```bash
# User Service
curl http://localhost:8081/api/actuator/health | jq

# Match Service
curl http://localhost:8082/actuator/health | jq

# Chat Service
curl http://localhost:8083/actuator/health | jq

# Recommendation Service
curl http://localhost:8084/actuator/health | jq

# Vaadin UI
curl http://localhost:8090/actuator/health | jq
```

### View Metrics
```bash
# List all available metrics
curl http://localhost:8084/actuator/metrics | jq

# Get specific metric
curl http://localhost:8084/actuator/metrics/recommendations.generated.total | jq

# Prometheus format (for Grafana)
curl http://localhost:8084/actuator/prometheus
```

## Actuator Endpoints

All services expose these endpoints:

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Service health status with component details |
| `/actuator/info` | Application information (name, version, description) |
| `/actuator/metrics` | All available metrics |
| `/actuator/prometheus` | Prometheus-formatted metrics for scraping |

### Health Check Components

Each service checks different components:

| Service | Health Checks |
|---------|---------------|
| **User Service** | Database, Redis, RabbitMQ |
| **Match Service** | Database, Redis |
| **Chat Service** | Database, Redis, RabbitMQ |
| **Recommendation Service** | Database, Redis, Custom scorer health |
| **Vaadin UI** | Redis (session storage) |

## Custom Metrics

### Recommendation Service

| Metric | Type | Description |
|--------|------|-------------|
| `recommendations.generated.total` | Counter | Total recommendation batches generated |
| `recommendations.generation.time` | Timer | Time taken to generate recommendations |

### Vaadin UI Service

| Metric | Type | Description |
|--------|------|-------------|
| `ui.logins.total` | Counter | Total successful logins |
| `ui.registrations.total` | Counter | Total successful registrations |
| `ui.swipes.total` | Counter | Total swipe actions |
| `ui.matches.total` | Counter | Total matches created |
| `ui.messages.sent.total` | Counter | Total messages sent |
| `ui.page.views.total` | Counter | Page views (tagged by page name) |
| `ui.api.call.time` | Timer | API call duration (tagged by service) |

### User Service (When Implemented)

These metrics should be added when the service is implemented:

| Metric | Type | Description |
|--------|------|-------------|
| `users.registrations.total` | Counter | Total user registrations |
| `auth.login.attempts.total` | Counter | Total login attempts |
| `auth.login.failures.total` | Counter | Failed login attempts |

## Structured Logging

### Log Format

**Development (Console):**
```
14:32:15.123 [http-nio-8084-exec-1] INFO  c.d.r.s.RecommendationService - Request completed: method=GET, path=/api/recommendations, status=200, duration=45ms
```

**Production (JSON):**
```json
{
  "timestamp": "2024-01-15T14:32:15.123Z",
  "level": "INFO",
  "service": "recommendation-service",
  "thread": "http-nio-8084-exec-1",
  "logger": "c.d.r.s.RecommendationService",
  "message": "Request completed: method=GET, path=/api/recommendations, status=200, duration=45ms",
  "requestId": "a1b2c3d4",
  "userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### MDC Context

Each request automatically includes:
- `requestId` - Unique identifier for request tracing
- `userId` - User ID from JWT token (if authenticated)

### Log Levels

```yaml
# Production
root: INFO
com.dating: INFO
org.springframework: WARN
org.hibernate: WARN

# Development
root: INFO
com.dating: DEBUG
com.dating.recommendation.scorer: DEBUG
```

## Request Tracing

### Headers

| Header | Description |
|--------|-------------|
| `X-Request-Id` | Unique request identifier (auto-generated if not provided) |
| `X-User-Id` | User ID extracted from JWT |

These headers are:
1. Extracted from incoming requests (or generated)
2. Added to MDC for logging
3. Returned in response headers
4. Can be used to trace requests across services

### Example: Tracing a Request

```bash
# Make request with custom request ID
curl -H "X-Request-Id: my-trace-123" http://localhost:8084/api/recommendations

# Search logs for this request
grep "my-trace-123" logs/*.log
```

## Monitoring Best Practices

### What to Monitor

1. **Health** - Are all services running?
2. **Errors** - Are there failures in logs?
3. **Latency** - Are responses fast enough?
4. **Business Metrics** - Are users engaging?

### Key Alerts (Future)

| Alert | Condition | Severity |
|-------|-----------|----------|
| Service Down | health != UP | Critical |
| High Error Rate | errors > 5% | High |
| Slow Response | p95 latency > 500ms | Medium |
| Login Failures | failures > 10/min | High |

## Prometheus Integration (Future)

### Scrape Configuration

Add to `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'dating-services'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
        - 'user-service:8081'
        - 'match-service:8082'
        - 'chat-service:8083'
        - 'recommendation-service:8084'
        - 'vaadin-ui:8090'
```

### Useful PromQL Queries

```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# 95th percentile latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Recommendations generated
rate(recommendations_generated_total[5m])

# Active users (logins per minute)
rate(ui_logins_total[1m])
```

## Troubleshooting

### Service Not Responding

```bash
# Check if service is running
docker-compose ps

# Check service logs
docker-compose logs -f recommendation-service

# Check health endpoint
curl http://localhost:8084/actuator/health
```

### Database Connection Issues

```bash
# Check database health
curl http://localhost:8084/actuator/health/db

# Check connection pool metrics
curl http://localhost:8084/actuator/metrics/hikaricp.connections.active
```

### Redis Connection Issues

```bash
# Check Redis health
curl http://localhost:8084/actuator/health/redis

# Test Redis directly
docker exec -it dating_redis redis-cli ping
```

### High Memory Usage

```bash
# Check JVM metrics
curl http://localhost:8084/actuator/metrics/jvm.memory.used
curl http://localhost:8084/actuator/metrics/jvm.gc.pause
```

## Files Reference

### Configuration Files

| Service | Config | Logback |
|---------|--------|---------|
| User Service | `backend/user-service/src/main/resources/application.yml` | `backend/user-service/src/main/resources/logback-spring.xml` |
| Recommendation Service | `backend/recommendation-service/src/main/resources/application.yml` | `backend/recommendation-service/src/main/resources/logback-spring.xml` |
| Vaadin UI | `backend/vaadin-ui-service/src/main/resources/application.yml` | `backend/vaadin-ui-service/src/main/resources/logback-spring.xml` |

### Metrics Configuration Classes

| Service | Path |
|---------|------|
| Recommendation Service | `backend/recommendation-service/src/main/java/com/dating/recommendation/config/MetricsConfig.java` |
| Vaadin UI | `backend/vaadin-ui-service/src/main/java/com/dating/ui/config/MetricsConfig.java` |

### Request Logging Filters

| Service | Path |
|---------|------|
| Recommendation Service | `backend/recommendation-service/src/main/java/com/dating/recommendation/config/RequestLoggingFilter.java` |
| Vaadin UI | `backend/vaadin-ui-service/src/main/java/com/dating/ui/config/RequestLoggingFilter.java` |

## Future Enhancements

1. **Grafana Dashboards** - Pre-built dashboards for each service
2. **Alerting** - PagerDuty/Slack integration for critical alerts
3. **Distributed Tracing** - Zipkin/Jaeger for cross-service tracing
4. **Log Aggregation** - ELK stack for centralized logging
5. **Spring Boot Admin** - Web UI for monitoring all services

---

**Last Updated:** 2024-01-15
**Version:** 1.0
