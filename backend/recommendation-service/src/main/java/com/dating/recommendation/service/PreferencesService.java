package com.dating.recommendation.service;

import com.dating.recommendation.dto.PreferencesDTO;
import com.dating.recommendation.dto.UpdatePreferencesRequest;
import com.dating.recommendation.entity.UserPreferences;
import com.dating.recommendation.repository.RecommendationScoreRepository;
import com.dating.recommendation.repository.UserPreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user preferences.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PreferencesService {

    private final UserPreferencesRepository preferencesRepository;
    private final RecommendationScoreRepository scoreRepository;

    /**
     * Get preferences for a user.
     *
     * @param userId the user ID
     * @return preferences DTO
     */
    public PreferencesDTO getPreferences(Long userId) {
        UserPreferences preferences = preferencesRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("Preferences not found for user: " + userId));

        return mapToDTO(preferences);
    }

    /**
     * Create or update preferences for a user.
     *
     * @param userId the user ID
     * @param request the preferences update request
     * @return updated preferences DTO
     */
    @Transactional
    public PreferencesDTO updatePreferences(Long userId, UpdatePreferencesRequest request) {
        // Validate age range
        if (request.getMinAge() > request.getMaxAge()) {
            throw new IllegalArgumentException("Minimum age cannot be greater than maximum age");
        }

        UserPreferences preferences = preferencesRepository.findByUserId(userId)
            .orElse(UserPreferences.builder()
                .userId(userId)
                .build());

        // Update preferences
        preferences.setMinAge(request.getMinAge());
        preferences.setMaxAge(request.getMaxAge());
        preferences.setPreferredGender(request.getPreferredGender());
        preferences.setMaxDistance(request.getMaxDistance());
        preferences.setInterests(request.getInterests());
        preferences.setFlexibleAgeRange(request.getFlexibleAgeRange() != null ? request.getFlexibleAgeRange() : false);
        preferences.setFlexibleDistance(request.getFlexibleDistance() != null ? request.getFlexibleDistance() : false);

        preferences = preferencesRepository.save(preferences);

        // Invalidate cached recommendation scores when preferences change
        scoreRepository.invalidateScoresForUser(userId);

        log.info("Updated preferences for user: {}", userId);

        return mapToDTO(preferences);
    }

    /**
     * Create default preferences for a new user.
     *
     * @param userId the user ID
     * @return default preferences DTO
     */
    @Transactional
    public PreferencesDTO createDefaultPreferences(Long userId) {
        if (preferencesRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException("Preferences already exist for user: " + userId);
        }

        UserPreferences preferences = UserPreferences.builder()
            .userId(userId)
            .minAge(18)
            .maxAge(99)
            .preferredGender("ANY")
            .maxDistance(50)
            .flexibleAgeRange(true)
            .flexibleDistance(true)
            .build();

        preferences = preferencesRepository.save(preferences);

        log.info("Created default preferences for user: {}", userId);

        return mapToDTO(preferences);
    }

    /**
     * Delete preferences for a user.
     *
     * @param userId the user ID
     */
    @Transactional
    public void deletePreferences(Long userId) {
        preferencesRepository.deleteByUserId(userId);
        scoreRepository.invalidateScoresForUser(userId);

        log.info("Deleted preferences for user: {}", userId);
    }

    /**
     * Check if preferences exist for a user.
     *
     * @param userId the user ID
     * @return true if preferences exist
     */
    public boolean hasPreferences(Long userId) {
        return preferencesRepository.existsByUserId(userId);
    }

    /**
     * Map entity to DTO.
     */
    private PreferencesDTO mapToDTO(UserPreferences preferences) {
        return PreferencesDTO.builder()
            .id(preferences.getId())
            .userId(preferences.getUserId())
            .minAge(preferences.getMinAge())
            .maxAge(preferences.getMaxAge())
            .preferredGender(preferences.getPreferredGender())
            .maxDistance(preferences.getMaxDistance())
            .interests(preferences.getInterests())
            .flexibleAgeRange(preferences.getFlexibleAgeRange())
            .flexibleDistance(preferences.getFlexibleDistance())
            .createdAt(preferences.getCreatedAt())
            .updatedAt(preferences.getUpdatedAt())
            .build();
    }
}
