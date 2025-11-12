package com.dating.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Chat Service - Main Application Class
 *
 * Real-time messaging service for matched users
 * - WebSocket support with STOMP protocol
 * - Message persistence in PostgreSQL
 * - Redis for session management and caching
 * - RabbitMQ for distributed WebSocket messaging
 * - Feign clients for user-service and match-service communication
 */
@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing
public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}
