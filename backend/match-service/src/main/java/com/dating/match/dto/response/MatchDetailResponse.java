package com.dating.match.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for detailed match information.
 *
 * @param id Match ID
 * @param user1 First user info
 * @param user2 Second user info
 * @param matchScore Compatibility score
 * @param scoreFactors Score breakdown factors
 * @param matchedAt When the match was created
 */
public record MatchDetailResponse(
    UUID id,
    UserInfo user1,
    UserInfo user2,
    BigDecimal matchScore,
    Map<String, Object> scoreFactors,
    Instant matchedAt
) {
    /**
     * Basic user information.
     *
     * @param id User ID
     * @param name User's display name
     */
    public record UserInfo(
        UUID id,
        String name
    ) {}
}
