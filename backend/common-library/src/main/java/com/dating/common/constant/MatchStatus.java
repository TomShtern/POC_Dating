package com.dating.common.constant;

/**
 * Enumeration of match statuses.
 */
public enum MatchStatus {

    /**
     * Match is active.
     * Users can chat and interact.
     */
    ACTIVE("Active", "Match is active"),

    /**
     * Match has ended.
     * Users can no longer chat.
     */
    ENDED("Ended", "Match has ended");

    private final String displayName;
    private final String description;

    MatchStatus(String displayName, String description) {
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
     * Check if users can interact in this match.
     *
     * @return true if can interact
     */
    public boolean canInteract() {
        return this == ACTIVE;
    }
}
