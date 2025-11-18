package com.dating.user.event;

import com.dating.user.config.RabbitMQConfig;
import com.dating.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event publisher for user-related events.
 * Publishes events to RabbitMQ for other services to consume.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish user registered event.
     *
     * @param user Registered user
     */
    public void publishUserRegistered(User user) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "USER_REGISTERED");
        event.put("userId", user.getId().toString());
        event.put("email", user.getEmail());
        event.put("username", user.getUsername());
        event.put("timestamp", Instant.now().toString());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.USER_EXCHANGE,
                    RabbitMQConfig.USER_REGISTERED_KEY,
                    event);
            log.info("Published USER_REGISTERED event for user: {}", user.getId());
        } catch (Exception ex) {
            log.error("Failed to publish USER_REGISTERED event for user: {}", user.getId(), ex);
        }
    }

    /**
     * Publish user updated event.
     *
     * @param user Updated user
     */
    public void publishUserUpdated(User user) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "USER_UPDATED");
        event.put("userId", user.getId().toString());
        event.put("email", user.getEmail());
        event.put("username", user.getUsername());
        event.put("firstName", user.getFirstName());
        event.put("lastName", user.getLastName());
        event.put("timestamp", Instant.now().toString());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.USER_EXCHANGE,
                    RabbitMQConfig.USER_UPDATED_KEY,
                    event);
            log.info("Published USER_UPDATED event for user: {}", user.getId());
        } catch (Exception ex) {
            log.error("Failed to publish USER_UPDATED event for user: {}", user.getId(), ex);
        }
    }

    /**
     * Publish user deleted event.
     *
     * @param userId Deleted user ID
     */
    public void publishUserDeleted(UUID userId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "USER_DELETED");
        event.put("userId", userId.toString());
        event.put("timestamp", Instant.now().toString());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.USER_EXCHANGE,
                    RabbitMQConfig.USER_DELETED_KEY,
                    event);
            log.info("Published USER_DELETED event for user: {}", userId);
        } catch (Exception ex) {
            log.error("Failed to publish USER_DELETED event for user: {}", userId, ex);
        }
    }
}
