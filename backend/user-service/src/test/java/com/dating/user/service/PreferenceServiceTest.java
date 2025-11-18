package com.dating.user.service;

import com.dating.user.dto.request.UpdatePreferencesRequest;
import com.dating.user.dto.response.PreferencesResponse;
import com.dating.user.exception.UserNotFoundException;
import com.dating.user.mapper.UserMapper;
import com.dating.user.model.User;
import com.dating.user.model.UserPreference;
import com.dating.user.repository.UserPreferenceRepository;
import com.dating.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PreferenceService.
 */
@ExtendWith(MockitoExtension.class)
class PreferenceServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private PreferenceService preferenceService;

    private UUID userId;
    private User testUser;
    private UserPreference testPreference;
    private PreferencesResponse preferencesResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .username("testuser")
                .build();

        testPreference = UserPreference.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .minAge(21)
                .maxAge(35)
                .maxDistanceKm(50)
                .interestedIn("FEMALE")
                .notificationEnabled(true)
                .build();

        preferencesResponse = PreferencesResponse.builder()
                .id(testPreference.getId())
                .userId(userId)
                .minAge(21)
                .maxAge(35)
                .maxDistanceKm(50)
                .interestedIn("FEMALE")
                .notificationEnabled(true)
                .build();
    }

    @Test
    @DisplayName("Should get preferences successfully")
    void testGetPreferences_Success() {
        // Arrange
        when(userRepository.existsById(userId)).thenReturn(true);
        when(userPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(testPreference));
        when(userMapper.toPreferencesResponse(testPreference)).thenReturn(preferencesResponse);

        // Act
        PreferencesResponse response = preferenceService.getPreferences(userId);

        // Assert
        assertNotNull(response);
        assertEquals(21, response.getMinAge());
        assertEquals(35, response.getMaxAge());
        assertEquals("FEMALE", response.getInterestedIn());
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testGetPreferences_UserNotFound() {
        // Arrange
        when(userRepository.existsById(userId)).thenReturn(false);

        // Act & Assert
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> preferenceService.getPreferences(userId));

        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    @DisplayName("Should update preferences successfully")
    void testUpdatePreferences_Success() {
        // Arrange
        UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                .minAge(25)
                .maxAge(40)
                .maxDistanceKm(100)
                .interestedIn("BOTH")
                .interests(Arrays.asList("hiking", "music"))
                .notificationEnabled(false)
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(userPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(testPreference));
        when(userPreferenceRepository.save(any(UserPreference.class))).thenReturn(testPreference);

        PreferencesResponse updatedResponse = PreferencesResponse.builder()
                .id(testPreference.getId())
                .userId(userId)
                .minAge(25)
                .maxAge(40)
                .maxDistanceKm(100)
                .interestedIn("BOTH")
                .notificationEnabled(false)
                .build();
        when(userMapper.toPreferencesResponse(any(UserPreference.class))).thenReturn(updatedResponse);

        // Act
        PreferencesResponse response = preferenceService.updatePreferences(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(25, response.getMinAge());
        assertEquals(40, response.getMaxAge());
        assertEquals(100, response.getMaxDistanceKm());
        assertEquals("BOTH", response.getInterestedIn());
        assertFalse(response.getNotificationEnabled());

        verify(userPreferenceRepository).save(any(UserPreference.class));
    }

    @Test
    @DisplayName("Should throw exception when min age greater than max age")
    void testUpdatePreferences_InvalidAgeRange() {
        // Arrange
        UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                .minAge(50)
                .maxAge(30)
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(userPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(testPreference));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> preferenceService.updatePreferences(userId, request));

        assertTrue(exception.getMessage().contains("Minimum age cannot be greater than maximum age"));
        verify(userPreferenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update only provided fields")
    void testUpdatePreferences_PartialUpdate() {
        // Arrange
        UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                .maxDistanceKm(75)
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(userPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(testPreference));
        when(userPreferenceRepository.save(any(UserPreference.class))).thenReturn(testPreference);
        when(userMapper.toPreferencesResponse(any(UserPreference.class))).thenReturn(preferencesResponse);

        // Act
        preferenceService.updatePreferences(userId, request);

        // Assert
        assertEquals(75, testPreference.getMaxDistanceKm());
        assertEquals(21, testPreference.getMinAge()); // Should remain unchanged
        assertEquals(35, testPreference.getMaxAge()); // Should remain unchanged
        verify(userPreferenceRepository).save(testPreference);
    }
}
