package com.dating.chat.event;

import com.dating.common.config.RabbitMQConstants;
import com.dating.common.event.UserDeletedEvent;
import com.dating.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event listener for user-related events from User Service.
 * Handles user deletion to clean up chat data.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventListener {

    private final MessageRepository messageRepository;

    /**
     * Handle user deleted event.
     * Clean up messages and chat data for the deleted user.
     *
     * @param event User deleted event
     */
    @RabbitListener(queues = RabbitMQConstants.CHAT_USER_DELETED_QUEUE)
    @Transactional
    public void handleUserDeleted(UserDeletedEvent event) {
        log.info("Received UserDeletedEvent: userId={}, hardDelete={}",
                event.getUserId(), event.isHardDelete());

        try {
            if (event.isHardDelete()) {
                // For hard delete, mark all messages from this user as deleted
                int updatedMessages = messageRepository.markMessagesAsDeletedBySenderId(event.getUserId());
                log.info("Marked {} messages as deleted for user {}", updatedMessages, event.getUserId());
            } else {
                // For soft delete, messages remain but user info should be anonymized
                log.info("Soft delete - messages preserved but user {} info should be anonymized",
                        event.getUserId());
            }
        } catch (Exception e) {
            log.error("Error handling UserDeletedEvent for user: {}", event.getUserId(), e);
            // Don't rethrow - we don't want to block the queue
        }
    }
}
