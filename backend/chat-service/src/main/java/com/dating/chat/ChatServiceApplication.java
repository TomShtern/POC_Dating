package com.dating.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Chat Service Application
 *
 * Microservice for real-time messaging via WebSockets.
 * Handles message delivery, typing indicators, read receipts,
 * and user presence tracking.
 *
 * Port: 8083
 */
@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableAsync
public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}
