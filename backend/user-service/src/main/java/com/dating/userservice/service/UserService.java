package com.dating.userservice.service;

import com.dating.userservice.dto.UpdateProfileRequest;
import com.dating.userservice.dto.UserProfileDTO;
import com.dating.userservice.dto.UserSummaryDTO;
import com.dating.userservice.exception.ResourceNotFoundException;
import com.dating.userservice.model.User;
import com.dating.userservice.repository.RefreshTokenRepository;
import com.dating.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * User Service
 *
 * Handles user profile management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Get user profile by ID
     */
    @Cacheable(value = "userProfiles", key = "#userId")
    public UserProfileDTO getUserProfile(UUID userId) {
        log.info("Fetching user profile for ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return mapToProfileDTO(user);
    }

    /**
     * Get active user profile by ID
     */
    @Cacheable(value = "userProfiles", key = "#userId")
    public UserProfileDTO getActiveUserProfile(UUID userId) {
        log.info("Fetching active user profile for ID: {}", userId);

        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return mapToProfileDTO(user);
    }

    /**
     * Get user summary by ID (lightweight for other services)
     */
    @Cacheable(value = "userSummaries", key = "#userId")
    public UserSummaryDTO getUserSummary(UUID userId) {
        log.info("Fetching user summary for ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return mapToSummaryDTO(user);
    }

    /**
     * Update user profile
     */
    @Transactional
    @CacheEvict(value = {"userProfiles", "userSummaries"}, key = "#userId")
    public UserProfileDTO updateProfile(UUID userId, UpdateProfileRequest request) {
        log.info("Updating profile for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Update fields if provided
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getPhotoUrl() != null) {
            user.setPhotoUrl(request.getPhotoUrl());
        }
        if (request.getLocation() != null) {
            user.setLocation(request.getLocation());
        }
        if (request.getPreferences() != null) {
            user.setPreferences(request.getPreferences());
        }

        user = userRepository.save(user);
        log.info("Profile updated successfully for user ID: {}", userId);

        return mapToProfileDTO(user);
    }

    /**
     * Delete user account (soft delete by setting status to DELETED)
     */
    @Transactional
    @CacheEvict(value = {"userProfiles", "userSummaries"}, key = "#userId")
    public void deleteAccount(UUID userId) {
        log.info("Deleting account for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Soft delete by setting status to DELETED
        user.setStatus(User.UserStatus.DELETED);
        userRepository.save(user);

        // Revoke all refresh tokens
        refreshTokenRepository.revokeAllUserTokens(user);

        log.info("Account deleted successfully for user ID: {}", userId);
    }

    /**
     * Map User entity to UserProfileDTO
     */
    private UserProfileDTO mapToProfileDTO(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .age(user.getAge())
                .gender(user.getGender())
                .bio(user.getBio())
                .photoUrl(user.getPhotoUrl())
                .location(user.getLocation())
                .preferences(user.getPreferences())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLogin(user.getLastLogin())
                .status(user.getStatus().name())
                .build();
    }

    /**
     * Map User entity to UserSummaryDTO
     */
    private UserSummaryDTO mapToSummaryDTO(User user) {
        return UserSummaryDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .age(user.getAge())
                .gender(user.getGender())
                .photoUrl(user.getPhotoUrl())
                .location(user.getLocation())
                .build();
    }
}
