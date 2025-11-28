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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        eventPublisher.publishUserUpdated(userId, "profile");

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

    /**
     * Get candidate users for matching.
     * Filters users by age range and excludes specified IDs.
     *
     * @param userId Requesting user's ID
     * @param minAge Minimum age filter
     * @param maxAge Maximum age filter
     * @param maxDistance Maximum distance (not implemented in this version)
     * @param excludeIds List of user IDs to exclude
     * @return List of candidate user responses
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getCandidates(UUID userId, int minAge, int maxAge,
                                            int maxDistance, List<UUID> excludeIds) {
        log.debug("Getting candidates for user: {}, minAge: {}, maxAge: {}, excludeIds count: {}",
                userId, minAge, maxAge, excludeIds != null ? excludeIds.size() : 0);

        // Calculate birth date range from age range
        LocalDate today = LocalDate.now();
        LocalDate minBirthDate = today.minusYears(minAge);  // Max birth date for min age
        LocalDate maxBirthDate = today.minusYears(maxAge + 1).plusDays(1);  // Min birth date for max age

        // Ensure excludeIds is not null or empty (JPA doesn't handle empty IN clause well)
        List<UUID> safeExcludeIds = (excludeIds == null || excludeIds.isEmpty())
                ? Collections.singletonList(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                : excludeIds;

        List<User> candidates = userRepository.findCandidates(userId, safeExcludeIds,
                minBirthDate, maxBirthDate);

        log.debug("Found {} candidates for user: {}", candidates.size(), userId);

        return candidates.stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get multiple users by their IDs.
     *
     * @param userIds List of user UUIDs
     * @return List of user responses
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByIds(List<UUID> userIds) {
        log.debug("Getting users by IDs, count: {}", userIds != null ? userIds.size() : 0);

        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<User> users = userRepository.findByIdIn(userIds);

        log.debug("Found {} users out of {} requested", users.size(), userIds.size());

        return users.stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }
}
