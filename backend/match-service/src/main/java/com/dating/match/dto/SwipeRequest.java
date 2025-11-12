package com.dating.match.dto;

import com.dating.match.entity.SwipeType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for swipe action requests
 *
 * PURPOSE: Transfer swipe data from client to service
 *
 * FIELDS:
 * - targetUserId: ID of user being swiped on (required)
 * - swipeType: Type of swipe action (required)
 *
 * WHY DTO vs Entity:
 * - DTOs decouple API from database schema
 * - Validation annotations for input checking
 * - Can add fields without changing entity
 * - Security: Don't expose entity internals
 *
 * VALIDATION:
 * - @NotNull ensures required fields are present
 * - Enum validation is automatic (invalid values rejected)
 * - Additional business validation in service layer
 *
 * NOTES:
 * - userId comes from JWT token (not in request body)
 * - timestamp is auto-generated (not in request)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwipeRequest {

    @NotNull(message = "Target user ID is required")
    private Long targetUserId;

    @NotNull(message = "Swipe type is required")
    private SwipeType swipeType;
}
