package com.dating.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events in the dating application.
 * All events should extend this class to ensure consistent event structure.
 *
 * <p>Events are used for asynchronous communication between microservices
 * via RabbitMQ message broker.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class BaseEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for this event instance.
     * Used for idempotency and event tracking.
     */
    private UUID eventId;

    /**
     * Timestamp when the event was created.
     */
    private Instant timestamp;

    /**
     * Source service that generated the event.
     * e.g., "user-service", "match-service", "chat-service"
     */
    private String source;

    /**
     * Type of the event.
     * e.g., "USER_REGISTERED", "MATCH_CREATED", "MESSAGE_SENT"
     */
    private String eventType;

    /**
     * Initialize event with default values.
     * Sets eventId to a new UUID and timestamp to now.
     *
     * @param source Service that generated the event
     * @param eventType Type of the event
     */
    public void initializeEvent(String source, String eventType) {
        this.eventId = UUID.randomUUID();
        this.timestamp = Instant.now();
        this.source = source;
        this.eventType = eventType;
    }
}
