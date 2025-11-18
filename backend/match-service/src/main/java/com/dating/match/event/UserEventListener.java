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
     *
     * @param event User registered event
     */
    @RabbitListener(queues = RabbitMQConstants.MATCH_USER_REGISTERED_QUEUE)
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent: userId={}", event.getUserId());

        try {
            // Currently, no special initialization needed for new users
            // Swipe history will be created on first swipe
            // Feed will be generated on first request
            log.debug("User {} registered - ready for matching", event.getUserId());
        } catch (Exception e) {
            log.error("Error handling UserRegisteredEvent for user: {}", event.getUserId(), e);
        }
    }

    /**
     * Handle user updated event.
     * Invalidate cached feeds when user preferences change.
     *
     * @param event User updated event
     */
    @RabbitListener(queues = RabbitMQConstants.MATCH_USER_UPDATED_QUEUE)
    public void handleUserUpdated(UserUpdatedEvent event) {
        log.info("Received UserUpdatedEvent: userId={}", event.getUserId());

        try {
            // Invalidate feed cache for this user
            // Their preferences may have changed, affecting who they see
            var feedCache = cacheManager.getCache(CacheConfig.FEED_CACHE);
            if (feedCache != null) {
                // Evict all cache entries for this user
                // In production, use a more targeted eviction strategy
                feedCache.clear();
                log.debug("Cleared feed cache due to user update: {}", event.getUserId());
            }
        } catch (Exception e) {
            log.error("Error handling UserUpdatedEvent for user: {}", event.getUserId(), e);
        }
    }
}
