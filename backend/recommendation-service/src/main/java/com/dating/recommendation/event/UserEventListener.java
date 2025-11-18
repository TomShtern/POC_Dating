package com.dating.recommendation.event;

import com.dating.common.config.RabbitMQConstants;
import com.dating.common.event.UserDeletedEvent;
import com.dating.common.event.UserRegisteredEvent;
import com.dating.common.event.UserUpdatedEvent;
import com.dating.recommendation.repository.RecommendationRepository;
import com.dating.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event listener for user events from RabbitMQ.
 * Handles user registration and update events to manage recommendations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventListener {

    private final RecommendationService recommendationService;
    private final RecommendationRepository recommendationRepository;

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

    /**
     * Handle user deleted event.
     * Delete all recommendations for and about the deleted user.
     *
     * @param event User deleted event
     */
    @RabbitListener(queues = RabbitMQConstants.RECOMMENDATION_USER_DELETED_QUEUE)
    @Transactional
    public void handleUserDeleted(UserDeletedEvent event) {
        log.info("Received UserDeletedEvent for user: {}", event.getUserId());

        try {
            // Delete all recommendations for this user
            int deletedRecommendations = recommendationRepository.deleteByUserId(event.getUserId());
            log.info("Deleted {} recommendations for user {}", deletedRecommendations, event.getUserId());

            // Also need to delete recommendations where this user is the recommended user
            // This would require a custom query - for now, log as TODO
            log.debug("Note: Should also delete recommendations where {} is the recommended user",
                    event.getUserId());

        } catch (Exception e) {
            log.error("Failed to handle user deletion for user {}: {}",
                    event.getUserId(), e.getMessage(), e);
            // Don't rethrow - we don't want to block the queue
        }
    }
}
