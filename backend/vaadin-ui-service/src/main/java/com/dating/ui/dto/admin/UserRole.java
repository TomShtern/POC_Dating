package com.dating.ui.dto.admin;

/**
 * Enum for user roles in the system
 */
public enum UserRole {
    ROLE_USER("User"),
    ROLE_MODERATOR("Moderator"),
    ROLE_ANALYST("Analyst"),
    ROLE_ADMIN("Administrator");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
