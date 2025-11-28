package com.dating.common.constant;

/**
 * Enumeration of message delivery statuses.
 */
public enum MessageStatus {

    /**
     * Message has been sent by the sender.
     * Not yet confirmed delivered to receiver.
     */
    SENT("Sent", "Message sent"),

    /**
     * Message has been delivered to receiver's device.
     * Not yet read by receiver.
     */
    DELIVERED("Delivered", "Message delivered"),

    /**
     * Message has been read by the receiver.
     */
    READ("Read", "Message read");

    private final String displayName;
    private final String description;

    MessageStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get the display name for UI purposes.
     *
     * @return Display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the description of this status.
     *
     * @return Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if the message has been delivered.
     *
     * @return true if delivered or read
     */
    public boolean isDelivered() {
        return this == DELIVERED || this == READ;
    }

    /**
     * Check if the message has been read.
     *
     * @return true if read
     */
    public boolean isRead() {
        return this == READ;
    }

    /**
     * Get the next status in the delivery flow.
     *
     * @return Next status or null if final
     */
    public MessageStatus nextStatus() {
        return switch (this) {
            case SENT -> DELIVERED;
            case DELIVERED -> READ;
            case READ -> null;
        };
    }
}
