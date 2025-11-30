package com.dating.user.mapper;

import com.dating.user.dto.response.PreferencesResponse;
import com.dating.user.dto.response.UserResponse;
import com.dating.user.model.User;
import com.dating.user.model.UserPreference;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;

/**
 * Mapper for converting between entities and DTOs.
 */
@Component
public class UserMapper {

    /**
     * Convert User entity to UserResponse DTO.
     *
     * @param user User entity
     * @return User response DTO
     */
    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dateOfBirth(user.getDateOfBirth())
                .age(calculateAge(user.getDateOfBirth()))
                .gender(user.getGender())
                .bio(user.getBio())
                .profilePictureUrl(user.getProfilePictureUrl())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLogin(user.getLastLoginAt())
                .build();
    }

    /**
     * Convert UserPreference entity to PreferencesResponse DTO.
     *
     * @param preference UserPreference entity
     * @return Preferences response DTO
     */
    public PreferencesResponse toPreferencesResponse(UserPreference preference) {
        if (preference == null) {
            return null;
        }

        return PreferencesResponse.builder()
                .id(preference.getId())
                .userId(preference.getUser().getId())
                .minAge(preference.getMinAge())
                .maxAge(preference.getMaxAge())
                .maxDistanceKm(preference.getMaxDistanceKm())
                .interestedIn(preference.getInterestedIn())
                .interests(preference.getInterests() != null
                        ? Arrays.asList(preference.getInterests())
                        : null)
                .notificationEnabled(preference.getNotificationEnabled())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }

    /**
     * Calculate age from date of birth.
     *
     * @param dateOfBirth Date of birth
     * @return Age in years, or null if date of birth is null
     */
    private Integer calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return null;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
