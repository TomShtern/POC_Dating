package com.dating.common.constant;

/**
 * Enumeration of swipe action types.
 */
public enum SwipeType {

    /**
     * User likes the profile.
     * Can result in a match if mutual.
     */
    LIKE("Like", true),

    /**
     * User passes on the profile.
     * No match possible.
     */
    PASS("Pass", false),

    /**
     * User super-likes the profile.
     * More prominent notification, can result in match.
     */
    SUPER_LIKE("Super Like", true);

    private final String displayName;
    private final boolean canMatch;

    SwipeType(String displayName, boolean canMatch) {
        this.displayName = displayName;
        this.canMatch = canMatch;
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
     * Check if this swipe type can result in a match.
     *
     * @return true if can match
     */
    public boolean canMatch() {
        return canMatch;
    }

    /**
     * Check if this is a positive swipe (like or super like).
     *
     * @return true if positive
     */
    public boolean isPositive() {
        return this == LIKE || this == SUPER_LIKE;
    }

    /**
     * Check if this is a super like.
     *
     * @return true if super like
     */
    public boolean isSuperLike() {
        return this == SUPER_LIKE;
    }
}
