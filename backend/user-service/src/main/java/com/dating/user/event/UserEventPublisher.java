package com.dating.user.event;

import com.dating.common.config.RabbitMQConstants;
import com.dating.common.event.UserDeletedEvent;
import com.dating.common.event.UserRegisteredEvent;
import com.dating.common.event.UserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Event publisher for user-related events.
 * Publishes events to RabbitMQ for other services to consume.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish user registered event.
     *
     * @param userId User ID
     * @param email User email
     * @param username Username
     */
    public void publishUserRegistered(UUID userId, String email, String username) {
        UserRegisteredEvent event = UserRegisteredEvent.create(userId, email, username);
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.USER_EXCHANGE,
                RabbitMQConstants.USER_REGISTERED_KEY,
                event
        );
        log.info("Published UserRegisteredEvent: userId={}", userId);
    }

    /**
     * Publish user updated event.
     *
     * @param userId User ID
     * @param fieldUpdated Field that was updated
     */
    public void publishUserUpdated(UUID userId, String fieldUpdated) {
        UserUpdatedEvent event = UserUpdatedEvent.create(userId);
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.USER_EXCHANGE,
                RabbitMQConstants.USER_UPDATED_KEY,
                event
        );
        log.info("Published UserUpdatedEvent: userId={}, field={}", userId, fieldUpdated);
    }

    /**
     * Publish user deleted event.
     *
     * @param userId Deleted user ID
     */
    public void publishUserDeleted(UUID userId) {
        UserDeletedEvent event = UserDeletedEvent.builder()
                .userId(userId)
                .hardDelete(false)
                .build();
        event.initializeEvent("user-service", "USER_DELETED");
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.USER_EXCHANGE,
                RabbitMQConstants.USER_DELETED_KEY,
                event
        );
        log.info("Published UserDeletedEvent: userId={}", userId);
    }
}
