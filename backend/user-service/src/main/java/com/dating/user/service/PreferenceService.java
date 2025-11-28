package com.dating.user.service;

import com.dating.user.config.CacheConfig;
import com.dating.user.dto.request.UpdatePreferencesRequest;
import com.dating.user.dto.response.PreferencesResponse;
import com.dating.user.exception.UserNotFoundException;
import com.dating.user.mapper.UserMapper;
import com.dating.user.model.User;
import com.dating.user.model.UserPreference;
import com.dating.user.repository.UserPreferenceRepository;
import com.dating.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for user preferences management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PreferenceService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserMapper userMapper;

    /**
     * Get user preferences.
     *
     * @param userId User UUID
     * @return Preferences response
     * @throws UserNotFoundException if user not found
     */
    @Cacheable(value = CacheConfig.USER_PREFERENCES_CACHE, key = "#userId")
    @Transactional(readOnly = true)
    public PreferencesResponse getPreferences(UUID userId) {
        log.debug("Getting preferences for user: {}", userId);

        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found: " + userId);
        }

        UserPreference preference = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Create default preferences if not found
                    log.info("Creating default preferences for user: {}", userId);
                    User user = userRepository.getReferenceById(userId);
                    UserPreference newPreference = UserPreference.builder()
                            .user(user)
                            .build();
                    return userPreferenceRepository.save(newPreference);
                });

        return userMapper.toPreferencesResponse(preference);
    }

    /**
     * Update user preferences.
     *
     * @param userId User UUID
     * @param request Update request
     * @return Updated preferences response
     * @throws UserNotFoundException if user not found
     */
    @CacheEvict(value = CacheConfig.USER_PREFERENCES_CACHE, key = "#userId")
    @Transactional
    public PreferencesResponse updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        log.debug("Updating preferences for user: {}", userId);

        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found: " + userId);
        }

        UserPreference preference = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.getReferenceById(userId);
                    return UserPreference.builder()
                            .user(user)
                            .build();
                });

        // Update only non-null fields
        if (request.getMinAge() != null) {
            preference.setMinAge(request.getMinAge());
        }
        if (request.getMaxAge() != null) {
            preference.setMaxAge(request.getMaxAge());
        }
        if (request.getMaxDistanceKm() != null) {
            preference.setMaxDistanceKm(request.getMaxDistanceKm());
        }
        if (request.getInterestedIn() != null) {
            preference.setInterestedIn(request.getInterestedIn());
        }
        if (request.getInterests() != null) {
            preference.setInterests(request.getInterests().toArray(new String[0]));
        }
        if (request.getNotificationEnabled() != null) {
            preference.setNotificationEnabled(request.getNotificationEnabled());
        }

        // Validate age range
        if (preference.getMinAge() > preference.getMaxAge()) {
            throw new IllegalArgumentException("Minimum age cannot be greater than maximum age");
        }

        UserPreference updated = userPreferenceRepository.save(preference);
        log.info("Preferences updated for user: {}", userId);

        return userMapper.toPreferencesResponse(updated);
    }
}
