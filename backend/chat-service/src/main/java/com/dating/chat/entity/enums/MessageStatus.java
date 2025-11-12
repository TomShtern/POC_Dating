package com.dating.chat.entity.enums;

/**
 * Message Status Enum
 *
 * Tracks the delivery status of messages
 */
public enum MessageStatus {
    /**
     * Message has been sent but not yet delivered to recipient
     */
    SENT,

    /**
     * Message has been delivered to recipient's device
     */
    DELIVERED,

    /**
     * Message has been read by recipient
     */
    READ
}
