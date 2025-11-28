package com.dating.common.constant;

/**
 * Enumeration of gender options for user profiles.
 */
public enum Gender {

    /**
     * Male gender.
     */
    MALE("Male"),

    /**
     * Female gender.
     */
    FEMALE("Female"),

    /**
     * Other or non-binary gender.
     */
    OTHER("Other");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
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
     * Parse gender from string value (case-insensitive).
     *
     * @param value String value to parse
     * @return Matching Gender enum value
     * @throws IllegalArgumentException if value doesn't match any gender
     */
    public static Gender fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Gender value cannot be null");
        }
        try {
            return Gender.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid gender value: " + value);
        }
    }
}
