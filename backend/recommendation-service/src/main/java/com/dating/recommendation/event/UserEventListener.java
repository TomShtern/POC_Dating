package com.dating.recommendation.event;

import com.dating.common.config.RabbitMQConstants;
import com.dating.common.event.UserRegisteredEvent;
import com.dating.common.event.UserUpdatedEvent;
import com.dating.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for user events from RabbitMQ.
 * Handles user registration and update events to manage recommendations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventListener {

    private final RecommendationService recommendationService;

    /**
     * Handle user registered event.
     * Generate initial recommendations for the new user.
     *
     * @param event User registered event
     */
    @RabbitListener(queues = RabbitMQConstants.RECOMMENDATION_USER_REGISTERED_QUEUE)
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for user: {}", event.getUserId());

        try {
            // Generate initial recommendations for the new user
            recommendationService.generateRecommendations(
                    event.getUserId(),
                    10,  // Default limit
                    "v1" // Default algorithm
            );

            log.info("Generated initial recommendations for new user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to generate recommendations for new user {}: {}",
                    event.getUserId(), e.getMessage(), e);
            // Don't rethrow - we don't want to block the queue
        }
    }

    /**
     * Handle user updated event.
     * Refresh recommendations when preferences are updated.
     *
     * @param event User updated event
     */
    @RabbitListener(queues = RabbitMQConstants.RECOMMENDATION_USER_UPDATED_QUEUE)
    public void handleUserUpdated(UserUpdatedEvent event) {
        log.info("Received UserUpdatedEvent for user: {}, preferencesUpdated: {}",
                event.getUserId(), event.isPreferencesUpdated());

        try {
            // Only refresh if preferences were updated
            if (event.isPreferencesUpdated()) {
                recommendationService.refreshRecommendations(event.getUserId());
                log.info("Refreshed recommendations for user {} due to preference change",
                        event.getUserId());
            }
        } catch (Exception e) {
            log.error("Failed to refresh recommendations for user {}: {}",
                    event.getUserId(), e.getMessage(), e);
            // Don't rethrow - we don't want to block the queue
        }
    }
}
