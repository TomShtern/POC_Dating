package com.dating.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Event published when a user account is deleted (soft delete).
 * Consumed by other services to clean up related data and
 * remove user from active matching pools.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UserDeletedEvent extends BaseEvent {

    private static final long serialVersionUID = 1L;

    /**
     * ID of the deleted user.
     */
    private UUID userId;

    /**
     * Email of the deleted user (for audit purposes).
     */
    private String email;

    /**
     * Reason for deletion (optional).
     */
    private String reason;

    /**
     * Whether this is a hard delete (permanent) or soft delete.
     */
    private boolean hardDelete;

    /**
     * Create a new UserDeletedEvent with default event metadata.
     *
     * @param userId ID of the deleted user
     * @param email User's email
     * @return New UserDeletedEvent instance
     */
    public static UserDeletedEvent create(UUID userId, String email) {
        UserDeletedEvent event = UserDeletedEvent.builder()
                .userId(userId)
                .email(email)
                .hardDelete(false)
                .build();
        event.initializeEvent("user-service", "USER_DELETED");
        return event;
    }
}
