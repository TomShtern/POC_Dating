package com.dating.match.mapper;

import com.dating.match.dto.response.MatchDetailResponse;
import com.dating.match.dto.response.MatchResponse;
import com.dating.match.model.Match;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mapper for Match entity to DTO conversions.
 */
@Component
public class MatchMapper {

    /**
     * Map Match entity to MatchResponse DTO.
     *
     * @param match Match entity
     * @param currentUserId Current user's ID
     * @param userInfo Matched user info
     * @return MatchResponse DTO
     */
    public MatchResponse toMatchResponse(Match match, UUID currentUserId,
                                          MatchResponse.MatchedUserInfo userInfo) {
        return new MatchResponse(
                match.getId(),
                userInfo,
                match.getMatchedAt()
        );
    }

    /**
     * Map Match entity to MatchDetailResponse DTO.
     *
     * @param match Match entity
     * @param user1Info User 1 info
     * @param user2Info User 2 info
     * @return MatchDetailResponse DTO
     */
    public MatchDetailResponse toMatchDetailResponse(Match match,
                                                      MatchDetailResponse.UserInfo user1Info,
                                                      MatchDetailResponse.UserInfo user2Info) {
        BigDecimal score = match.getMatchScore() != null ?
                match.getMatchScore().getScore() : BigDecimal.ZERO;

        Map<String, Object> factors = match.getMatchScore() != null ?
                match.getMatchScore().getFactors() : new HashMap<>();

        return new MatchDetailResponse(
                match.getId(),
                user1Info,
                user2Info,
                score,
                factors,
                match.getMatchedAt()
        );
    }
}
