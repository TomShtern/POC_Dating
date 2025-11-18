package com.dating.user.service;

import com.dating.user.config.CacheConfig;
import com.dating.user.dto.request.UpdateUserRequest;
import com.dating.user.dto.response.UserResponse;
import com.dating.user.event.UserEventPublisher;
import com.dating.user.exception.UserNotFoundException;
import com.dating.user.mapper.UserMapper;
import com.dating.user.model.User;
import com.dating.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for user profile management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserEventPublisher eventPublisher;

    /**
     * Get user by ID.
     *
     * @param userId User UUID
     * @return User response
     * @throws UserNotFoundException if user not found
     */
    @Cacheable(value = CacheConfig.USERS_CACHE, key = "#userId")
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        log.debug("Getting user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        return userMapper.toUserResponse(user);
    }

    /**
     * Update user profile.
     *
     * @param userId User UUID
     * @param request Update request
     * @return Updated user response
     * @throws UserNotFoundException if user not found
     */
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.USERS_CACHE, key = "#userId"),
            @CacheEvict(value = CacheConfig.USER_BY_EMAIL_CACHE, allEntries = true)
    })
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        log.debug("Updating user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        // Update only non-null fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getProfilePictureUrl() != null) {
            user.setProfilePictureUrl(request.getProfilePictureUrl());
        }

        User updated = userRepository.save(user);
        log.info("User updated: {}", userId);

        // Publish event
        eventPublisher.publishUserUpdated(updated);

        return userMapper.toUserResponse(updated);
    }

    /**
     * Delete user account (soft delete).
     *
     * @param userId User UUID
     * @throws UserNotFoundException if user not found
     */
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.USERS_CACHE, key = "#userId"),
            @CacheEvict(value = CacheConfig.USER_BY_EMAIL_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.USER_PREFERENCES_CACHE, key = "#userId")
    })
    @Transactional
    public void deleteUser(UUID userId) {
        log.debug("Deleting user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        user.setStatus("DELETED");
        userRepository.save(user);

        log.info("User deleted (soft delete): {}", userId);

        // Publish event
        eventPublisher.publishUserDeleted(userId);
    }
}
