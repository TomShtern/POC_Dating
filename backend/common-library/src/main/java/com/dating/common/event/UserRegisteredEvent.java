package com.dating.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Event published when a new user registers in the system.
 * Consumed by Match Service to initialize swipe history and
 * Recommendation Service to generate initial recommendations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UserRegisteredEvent extends BaseEvent {

    private static final long serialVersionUID = 1L;

    /**
     * ID of the newly registered user.
     */
    private UUID userId;

    /**
     * User's email address.
     */
    private String email;

    /**
     * User's username.
     */
    private String username;

    /**
     * User's first name.
     */
    private String firstName;

    /**
     * User's last name.
     */
    private String lastName;

    /**
     * User's date of birth.
     */
    private LocalDate dateOfBirth;

    /**
     * User's gender.
     */
    private String gender;

    /**
     * User's city location.
     */
    private String city;

    /**
     * User's country.
     */
    private String country;

    /**
     * User's latitude coordinate.
     */
    private Double latitude;

    /**
     * User's longitude coordinate.
     */
    private Double longitude;

    /**
     * Create a new UserRegisteredEvent with default event metadata.
     *
     * @param userId ID of the registered user
     * @param email User's email
     * @param username User's username
     * @return New UserRegisteredEvent instance
     */
    public static UserRegisteredEvent create(UUID userId, String email, String username) {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(userId)
                .email(email)
                .username(username)
                .build();
        event.initializeEvent("user-service", "USER_REGISTERED");
        return event;
    }
}
