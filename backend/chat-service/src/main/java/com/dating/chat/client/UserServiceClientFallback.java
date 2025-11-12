package com.dating.chat.client;

import com.dating.chat.dto.UserSummaryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Fallback implementation for UserServiceClient
 *
 * Provides default responses when user-service is unavailable
 */
@Component
@Slf4j
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public UserSummaryDTO getUserById(UUID userId) {
        log.warn("UserService unavailable, returning default user summary for userId: {}", userId);
        return UserSummaryDTO.builder()
                .id(userId)
                .name("Unknown User")
                .email("unknown@example.com")
                .build();
    }

    @Override
    public Boolean userExists(UUID userId) {
        log.warn("UserService unavailable, assuming user exists for userId: {}", userId);
        return true; // Assume user exists to avoid blocking operations
    }
}
