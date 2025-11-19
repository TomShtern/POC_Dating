package com.dating.ui.service.admin;

import com.dating.ui.dto.admin.ServiceHealthDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Service for system monitoring and health checks
 */
@Service
@Slf4j
public class AdminSystemService {

    @Value("${services.user-service.url:http://localhost:8081}")
    private String userServiceUrl;

    @Value("${services.match-service.url:http://localhost:8082}")
    private String matchServiceUrl;

    @Value("${services.chat-service.url:http://localhost:8083}")
    private String chatServiceUrl;

    @Value("${services.recommendation-service.url:http://localhost:8084}")
    private String recommendationServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();

    /**
     * Get health status of all microservices
     */
    public List<ServiceHealthDTO> getAllServiceHealth() {
        List<ServiceHealthDTO> healthList = new ArrayList<>();

        healthList.add(checkServiceHealth("User Service", userServiceUrl, "8081"));
        healthList.add(checkServiceHealth("Match Service", matchServiceUrl, "8082"));
        healthList.add(checkServiceHealth("Chat Service", chatServiceUrl, "8083"));
        healthList.add(checkServiceHealth("Recommendation Service", recommendationServiceUrl, "8084"));
        healthList.add(checkInfrastructureHealth("PostgreSQL", "localhost:5432"));
        healthList.add(checkInfrastructureHealth("Redis", "localhost:6379"));
        healthList.add(checkInfrastructureHealth("RabbitMQ", "localhost:5672"));

        return healthList;
    }

    /**
     * Check health of a specific service
     */
    private ServiceHealthDTO checkServiceHealth(String serviceName, String url, String port) {
        long startTime = System.currentTimeMillis();
        String healthUrl = url + "/actuator/health";

        try {
            // In POC, we'll simulate the health check
            // In production, would actually call the health endpoint
            boolean isUp = random.nextDouble() > 0.1; // 90% chance of being up

            long responseTime = 20 + random.nextInt(100); // Simulated response time

            return ServiceHealthDTO.builder()
                    .serviceName(serviceName)
                    .status(isUp ? "UP" : "DOWN")
                    .url(url)
                    .responseTimeMs(responseTime)
                    .lastChecked(LocalDateTime.now())
                    .version("1.0.0-SNAPSHOT")
                    .errorMessage(isUp ? null : "Connection refused")
                    .build();

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.warn("Health check failed for {}: {}", serviceName, e.getMessage());

            return ServiceHealthDTO.builder()
                    .serviceName(serviceName)
                    .status("DOWN")
                    .url(url)
                    .responseTimeMs(responseTime)
                    .lastChecked(LocalDateTime.now())
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * Check infrastructure component health
     */
    private ServiceHealthDTO checkInfrastructureHealth(String name, String host) {
        // Simulated check for POC
        boolean isUp = random.nextDouble() > 0.05; // 95% uptime

        return ServiceHealthDTO.builder()
                .serviceName(name)
                .status(isUp ? "UP" : "DOWN")
                .url(host)
                .responseTimeMs(5 + random.nextInt(20))
                .lastChecked(LocalDateTime.now())
                .version(name.equals("PostgreSQL") ? "15.0" :
                        name.equals("Redis") ? "7.0" : "3.12")
                .errorMessage(isUp ? null : "Connection timeout")
                .build();
    }

    /**
     * Get database metrics
     */
    public DatabaseMetrics getDatabaseMetrics() {
        // Mock metrics for POC
        return new DatabaseMetrics(
                45,           // Active connections
                100,          // Max connections
                12.5,         // Average query time (ms)
                1547823,      // Total queries executed
                2.3,          // Cache hit ratio percentage
                "1.2 GB",     // Database size
                "256 MB"      // Index size
        );
    }

    /**
     * Get cache (Redis) metrics
     */
    public CacheMetrics getCacheMetrics() {
        // Mock metrics for POC
        return new CacheMetrics(
                94.7,         // Hit rate percentage
                1234567,      // Total hits
                68432,        // Total misses
                15234,        // Keys in cache
                "512 MB",     // Memory used
                "1 GB",       // Max memory
                12            // Connected clients
        );
    }

    /**
     * Get message queue metrics
     */
    public QueueMetrics getQueueMetrics() {
        // Mock metrics for POC
        return new QueueMetrics(
                5,            // Total queues
                127,          // Messages in queues
                45623,        // Messages published (last hour)
                45598,        // Messages consumed (last hour)
                3,            // Active consumers
                0.2           // Average delivery time (ms)
        );
    }

    /**
     * Get recent error logs
     */
    public List<ErrorLogEntry> getRecentErrors(int limit) {
        List<ErrorLogEntry> errors = new ArrayList<>();

        // Mock error logs for POC
        errors.add(new ErrorLogEntry(
                LocalDateTime.now().minusMinutes(15),
                "ERROR",
                "UserService",
                "Connection timeout to database",
                "com.dating.user.service.UserService.findById(UserService.java:45)"
        ));

        errors.add(new ErrorLogEntry(
                LocalDateTime.now().minusHours(2),
                "WARN",
                "MatchService",
                "Cache miss for user feed - regenerating",
                "com.dating.match.service.FeedService.generateFeed(FeedService.java:123)"
        ));

        errors.add(new ErrorLogEntry(
                LocalDateTime.now().minusHours(5),
                "ERROR",
                "ChatService",
                "WebSocket connection dropped unexpectedly",
                "com.dating.chat.websocket.ChatHandler.afterConnectionClosed(ChatHandler.java:87)"
        ));

        return errors.stream().limit(limit).toList();
    }

    // Record classes for structured return types
    public record DatabaseMetrics(
            int activeConnections,
            int maxConnections,
            double avgQueryTime,
            long totalQueries,
            double cacheHitRatio,
            String databaseSize,
            String indexSize
    ) {}

    public record CacheMetrics(
            double hitRate,
            long totalHits,
            long totalMisses,
            long keysInCache,
            String memoryUsed,
            String maxMemory,
            int connectedClients
    ) {}

    public record QueueMetrics(
            int totalQueues,
            int messagesInQueue,
            long messagesPublished,
            long messagesConsumed,
            int activeConsumers,
            double avgDeliveryTime
    ) {}

    public record ErrorLogEntry(
            LocalDateTime timestamp,
            String level,
            String service,
            String message,
            String stackTrace
    ) {}
}
