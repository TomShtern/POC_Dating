package com.dating.match.event;

import com.dating.common.config.RabbitMQConstants;
import com.dating.common.event.UserRegisteredEvent;
import com.dating.common.event.UserUpdatedEvent;
import com.dating.match.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Event listener for user-related events from User Service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventListener {

    private final CacheManager cacheManager;

    /**
     * Handle user registered event.
     * Initialize any necessary data structures for the new user.
     * Handles both typed events and Map format for compatibility.
     *
     * @param message Raw message object
     */
    @RabbitListener(queues = RabbitMQConstants.MATCH_USER_REGISTERED_QUEUE)
    public void handleUserRegistered(Object message) {
        String userId = null;

        try {
            if (message instanceof UserRegisteredEvent event) {
                userId = event.getUserId() != null ? event.getUserId().toString() : null;
            } else if (message instanceof Map<?, ?> map) {
                userId = (String) map.get("userId");
            }

            log.info("Received USER_REGISTERED event for user: {}", userId);

            // Currently, no special initialization needed for new users
            // Swipe history will be created on first swipe
            // Feed will be generated on first request
            log.debug("User {} registered - ready for matching", userId);
        } catch (Exception e) {
            log.error("Error handling USER_REGISTERED event for user: {}", userId, e);
        }
    }

    /**
     * Handle user updated event.
     * Invalidate cached feeds when user preferences change.
     * Handles both typed events and Map format for compatibility.
     *
     * @param message Raw message object
     */
    @RabbitListener(queues = RabbitMQConstants.MATCH_USER_UPDATED_QUEUE)
    public void handleUserUpdated(Object message) {
        String userId = null;

        try {
            if (message instanceof UserUpdatedEvent event) {
                userId = event.getUserId() != null ? event.getUserId().toString() : null;
            } else if (message instanceof Map<?, ?> map) {
                userId = (String) map.get("userId");
            }

            log.info("Received USER_UPDATED event for user: {}", userId);

            // Invalidate feed cache for this user
            // Their preferences may have changed, affecting who they see
            var feedCache = cacheManager.getCache(CacheConfig.FEED_CACHE);
            if (feedCache != null) {
                // Evict all cache entries for this user
                // In production, use a more targeted eviction strategy
                feedCache.clear();
                log.debug("Cleared feed cache due to user update: {}", userId);
            }
        } catch (Exception e) {
            log.error("Error handling USER_UPDATED event for user: {}", userId, e);
        }
    }
}
