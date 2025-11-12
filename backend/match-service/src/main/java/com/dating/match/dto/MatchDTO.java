package com.dating.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for match data transfer
 *
 * PURPOSE: Return match information with user details to client
 *
 * FIELDS:
 * - matchId: ID of the match record
 * - matchedUser: Summary of the matched user (profile data)
 * - matchedAt: When the match was created
 * - isActive: Whether the match is still active
 *
 * WHY INCLUDE USER SUMMARY:
 * - Avoids client making additional API calls
 * - Reduces network round trips
 * - Improves user experience (faster loading)
 * - Common pattern in REST APIs (nested resources)
 *
 * DESIGN DECISIONS:
 * - Only returns the OTHER user (not the requesting user)
 * - UserSummaryDTO contains essential profile info
 * - matchedAt for sorting (newest first)
 * - isActive to filter out unmatched users
 *
 * CLIENT USAGE:
 * - Display list of matches with photos/names
 * - Sort by matchedAt for "New Match" badges
 * - Filter out inactive matches
 * - Enable chat button for each match
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchDTO {

    private Long matchId;
    private UserSummaryDTO matchedUser;
    private LocalDateTime matchedAt;
    private Boolean isActive;
}
