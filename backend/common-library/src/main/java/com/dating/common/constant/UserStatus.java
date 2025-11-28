package com.dating.common.constant;

/**
 * Enumeration of user account statuses.
 */
public enum UserStatus {

    /**
     * User account is active and can use all features.
     */
    ACTIVE("Active", "User account is active"),

    /**
     * User account is temporarily suspended.
     * Cannot login or access any features.
     */
    SUSPENDED("Suspended", "User account is suspended"),

    /**
     * User account has been soft-deleted.
     * Data retained for compliance but user cannot access.
     */
    DELETED("Deleted", "User account has been deleted");

    private final String displayName;
    private final String description;

    UserStatus(String displayName, String description) {
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
     * Check if user can access the application.
     *
     * @return true if user has access
     */
    public boolean canAccess() {
        return this == ACTIVE;
    }
}
