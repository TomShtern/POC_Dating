package com.dating.recommendation.mapper;

import com.dating.recommendation.dto.UserProfileDto;
import com.dating.recommendation.dto.response.RecommendationResponse;
import com.dating.recommendation.dto.response.RecommendedUserResponse;
import com.dating.recommendation.dto.response.ScoreFactors;
import com.dating.recommendation.model.Recommendation;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between entities and DTOs.
 */
@Component
public class RecommendationMapper {

    /**
     * Convert Recommendation entity to response DTO.
     *
     * @param recommendation Recommendation entity
     * @param userProfile User profile data
     * @param factors Score factors
     * @param reason Recommendation reason
     * @return Recommendation response DTO
     */
    public RecommendationResponse toResponse(
            Recommendation recommendation,
            UserProfileDto userProfile,
            ScoreFactors factors,
            String reason) {

        RecommendedUserResponse recommendedUser = null;
        if (userProfile != null) {
            recommendedUser = RecommendedUserResponse.builder()
                    .id(userProfile.getId())
                    .name(userProfile.getDisplayName())
                    .age(userProfile.getAge())
                    .profilePictureUrl(userProfile.getProfilePictureUrl())
                    .bio(userProfile.getBio())
                    .build();
        }

        return RecommendationResponse.builder()
                .id(recommendation.getId())
                .recommendedUser(recommendedUser)
                .score(recommendation.getScore() != null ?
                        recommendation.getScore().intValue() : 0)
                .scoreFactors(factors)
                .reason(reason)
                .build();
    }

    /**
     * Convert UserProfileDto to RecommendedUserResponse.
     *
     * @param profile User profile
     * @return Recommended user response
     */
    public RecommendedUserResponse toRecommendedUser(UserProfileDto profile) {
        if (profile == null) {
            return null;
        }

        return RecommendedUserResponse.builder()
                .id(profile.getId())
                .name(profile.getDisplayName())
                .age(profile.getAge())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .bio(profile.getBio())
                .build();
    }
}
