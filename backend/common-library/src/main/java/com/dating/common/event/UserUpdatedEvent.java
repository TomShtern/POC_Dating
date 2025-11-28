package com.dating.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Event published when a user profile is updated.
 * Consumed by other services to invalidate caches and
 * update any denormalized user data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UserUpdatedEvent extends BaseEvent {

    private static final long serialVersionUID = 1L;

    /**
     * ID of the updated user.
     */
    private UUID userId;

    /**
     * Updated first name (null if not changed).
     */
    private String firstName;

    /**
     * Updated last name (null if not changed).
     */
    private String lastName;

    /**
     * Updated bio (null if not changed).
     */
    private String bio;

    /**
     * Updated photo URL (null if not changed).
     */
    private String photoUrl;

    /**
     * Updated city (null if not changed).
     */
    private String city;

    /**
     * Updated country (null if not changed).
     */
    private String country;

    /**
     * Updated latitude (null if not changed).
     */
    private Double latitude;

    /**
     * Updated longitude (null if not changed).
     */
    private Double longitude;

    /**
     * Whether preferences were updated.
     */
    private boolean preferencesUpdated;

    /**
     * Create a new UserUpdatedEvent with default event metadata.
     *
     * @param userId ID of the updated user
     * @return New UserUpdatedEvent instance
     */
    public static UserUpdatedEvent create(UUID userId) {
        UserUpdatedEvent event = UserUpdatedEvent.builder()
                .userId(userId)
                .preferencesUpdated(false)
                .build();
        event.initializeEvent("user-service", "USER_UPDATED");
        return event;
    }
}
