package com.dating.match.dto;

import com.dating.match.entity.SwipeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for swipe action responses
 *
 * PURPOSE: Return swipe result and match status to client
 *
 * FIELDS:
 * - swipeId: ID of created swipe record
 * - userId: User who performed the swipe
 * - targetUserId: User who was swiped on
 * - swipeType: Type of swipe that occurred
 * - timestamp: When the swipe occurred
 * - isMatch: Whether this swipe created a mutual match
 * - matchId: ID of created match (if isMatch=true)
 *
 * WHY INCLUDE isMatch:
 * - Client needs immediate feedback for match animations
 * - Avoids extra API call to check match status
 * - Enables real-time "It's a Match!" notifications
 *
 * DESIGN DECISIONS:
 * - All fields are nullable for flexibility
 * - matchId only populated when isMatch=true
 * - Timestamp uses LocalDateTime (ISO-8601 in JSON)
 *
 * CLIENT USAGE:
 * 1. Send swipe request
 * 2. Receive response
 * 3. If isMatch=true, show match celebration
 * 4. If isMatch=false, show next profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwipeResponse {

    private Long swipeId;
    private Long userId;
    private Long targetUserId;
    private SwipeType swipeType;
    private LocalDateTime timestamp;
    private Boolean isMatch;
    private Long matchId;
}
