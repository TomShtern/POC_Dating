package com.dating.ui.dto.admin;

/**
 * Enum for user account status
 */
public enum UserStatus {
    ACTIVE("Active"),
    SUSPENDED("Suspended"),
    DELETED("Deleted");

    private final String displayName;

    UserStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
