package com.dating.common.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Response DTO
 *
 * PURPOSE: API response object for user data
 * Used when returning user information to clients
 *
 * FIELDS:
 * - id: User UUID (never changes)
 * - email: User email (unique)
 * - username: User display name (unique)
 * - firstName, lastName: Full name
 * - age: Calculated from dateOfBirth (NOT stored, derived)
 * - gender: User gender
 * - bio: User biography
 * - profilePictureUrl: URL to profile image
 * - status: ACTIVE, SUSPENDED, DELETED
 * - createdAt, updatedAt: Timestamps
 *
 * SECURITY:
 * - DO NOT include passwordHash
 * - DO NOT include refresh tokens
 * - DO NOT include IP addresses or device info
 *
 * RATIONALE:
 * - Separate from entity to control what's exposed
 * - Allows evolution without breaking API
 * - Calculated fields (age) computed in service layer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    // TODO: Add all fields
    // TODO: Add constructors for different use cases
    // TODO: Add helper methods (e.g., getAge(), isActive())
}
