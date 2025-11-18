package com.dating.common.config;

/**
 * Constants for RabbitMQ configuration.
 * Defines exchange names, queue names, and routing keys
 * used for event-driven communication between microservices.
 */
public final class RabbitMQConstants {

    private RabbitMQConstants() {
        // Prevent instantiation
    }

    // ===========================================
    // EXCHANGES
    // ===========================================

    /**
     * Exchange for user-related events.
     */
    public static final String USER_EXCHANGE = "user.exchange";

    /**
     * Exchange for match-related events.
     */
    public static final String MATCH_EXCHANGE = "match.exchange";

    /**
     * Exchange for chat/message-related events.
     */
    public static final String CHAT_EXCHANGE = "chat.exchange";

    /**
     * Exchange for notification events.
     */
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";

    // ===========================================
    // QUEUES - USER SERVICE
    // ===========================================

    /**
     * Queue for user registration events.
     */
    public static final String USER_REGISTERED_QUEUE = "user.registered.queue";

    /**
     * Queue for user update events.
     */
    public static final String USER_UPDATED_QUEUE = "user.updated.queue";

    /**
     * Queue for user deletion events.
     */
    public static final String USER_DELETED_QUEUE = "user.deleted.queue";

    // ===========================================
    // QUEUES - MATCH SERVICE
    // ===========================================

    /**
     * Queue for match creation events.
     */
    public static final String MATCH_CREATED_QUEUE = "match.created.queue";

    /**
     * Queue for match ended events.
     */
    public static final String MATCH_ENDED_QUEUE = "match.ended.queue";

    /**
     * Queue for match service to consume user registration events.
     */
    public static final String MATCH_USER_REGISTERED_QUEUE = "match.user.registered.queue";

    /**
     * Queue for match service to consume user update events.
     */
    public static final String MATCH_USER_UPDATED_QUEUE = "match.user.updated.queue";

    /**
     * Queue for match service to consume user deletion events.
     */
    public static final String MATCH_USER_DELETED_QUEUE = "match.user.deleted.queue";

    // ===========================================
    // QUEUES - CHAT SERVICE
    // ===========================================

    /**
     * Queue for message sent events.
     */
    public static final String MESSAGE_SENT_QUEUE = "message.sent.queue";

    /**
     * Queue for message read events.
     */
    public static final String MESSAGE_READ_QUEUE = "message.read.queue";

    /**
     * Queue for chat service to consume match creation events.
     */
    public static final String CHAT_MATCH_CREATED_QUEUE = "chat.match.created.queue";

    /**
     * Queue for chat service to consume match ended events.
     */
    public static final String CHAT_MATCH_ENDED_QUEUE = "chat.match.ended.queue";

    /**
     * Queue for chat service to consume user deletion events.
     */
    public static final String CHAT_USER_DELETED_QUEUE = "chat.user.deleted.queue";

    // ===========================================
    // QUEUES - RECOMMENDATION SERVICE
    // ===========================================

    /**
     * Queue for recommendation service to consume user registration events.
     */
    public static final String RECOMMENDATION_USER_REGISTERED_QUEUE = "recommendation.user.registered.queue";

    /**
     * Queue for recommendation service to consume user update events.
     */
    public static final String RECOMMENDATION_USER_UPDATED_QUEUE = "recommendation.user.updated.queue";

    /**
     * Queue for recommendation service to consume user deletion events.
     */
    public static final String RECOMMENDATION_USER_DELETED_QUEUE = "recommendation.user.deleted.queue";

    // ===========================================
    // ROUTING KEYS
    // ===========================================

    /**
     * Routing key for user registered events.
     */
    public static final String USER_REGISTERED_KEY = "user.registered";

    /**
     * Routing key for user updated events.
     */
    public static final String USER_UPDATED_KEY = "user.updated";

    /**
     * Routing key for user deleted events.
     */
    public static final String USER_DELETED_KEY = "user.deleted";

    /**
     * Routing key for match created events.
     */
    public static final String MATCH_CREATED_KEY = "match.created";

    /**
     * Routing key for match ended events.
     */
    public static final String MATCH_ENDED_KEY = "match.ended";

    /**
     * Routing key for message sent events.
     */
    public static final String MESSAGE_SENT_KEY = "message.sent";

    /**
     * Routing key for message read events.
     */
    public static final String MESSAGE_READ_KEY = "message.read";

    // ===========================================
    // DEAD LETTER CONFIGURATION
    // ===========================================

    /**
     * Dead letter exchange for failed messages.
     */
    public static final String DEAD_LETTER_EXCHANGE = "dead.letter.exchange";

    /**
     * Dead letter queue for failed messages.
     */
    public static final String DEAD_LETTER_QUEUE = "dead.letter.queue";

    /**
     * Routing key for dead letter messages.
     */
    public static final String DEAD_LETTER_KEY = "dead.letter";
}
