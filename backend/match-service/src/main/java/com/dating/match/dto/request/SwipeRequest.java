package com.dating.match.dto.request;

import com.dating.common.constant.SwipeType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for recording a swipe action.
 *
 * @param targetUserId ID of the user being swiped on
 * @param action Swipe action (LIKE, PASS, SUPER_LIKE)
 */
public record SwipeRequest(
    @NotNull(message = "Target user ID is required")
    UUID targetUserId,

    @NotNull(message = "Swipe action is required")
    SwipeType action
) {}
