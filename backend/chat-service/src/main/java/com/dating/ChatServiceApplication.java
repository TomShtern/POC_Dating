package com.dating;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * Chat Service Application Entry Point
 *
 * PURPOSE: Bootstrap the Chat Service microservice
 *
 * RESPONSIBILITIES:
 * - Initialize Spring Boot WebSocket server
 * - Enable dependency injection
 * - Configure WebSocket handlers
 * - Initialize RabbitMQ STOMP relay for multi-instance distribution
 *
 * KEY CONFIGURATION:
 * - WebSocket endpoint: /api/chat/ws
 * - STOMP message broker: RabbitMQ (for distributed messaging)
 * - Application destination prefix: /app (client-to-server messages)
 *
 * PACKAGES SCANNED:
 * - com.dating.chat.* (all service subpackages)
 * - WebSocketConfig (WebSocket configuration)
 * - MessageHandler (message processing)
 * - Services (chat business logic)
 * - Repositories (message persistence)
 * - Events (message delivery tracking)
 *
 * SPECIAL CONSIDERATIONS:
 * - Maintains persistent WebSocket connections
 * - Manages session state in Redis
 * - Publishes messages to RabbitMQ for relay to other instances
 * - Requires sticky sessions if load balanced (or Redis sessions)
 */
@SpringBootApplication
@EnableWebSocket
public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}
