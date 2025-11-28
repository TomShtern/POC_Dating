package com.dating.user.repository;

import com.dating.user.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserPreference entity.
 * Handles all database operations for user preferences.
 */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

    /**
     * Find preferences by user ID.
     *
     * @param userId User UUID
     * @return Optional containing preferences if found
     */
    Optional<UserPreference> findByUserId(UUID userId);

    /**
     * Check if preferences exist for user.
     *
     * @param userId User UUID
     * @return true if preferences exist
     */
    boolean existsByUserId(UUID userId);

    /**
     * Delete preferences by user ID.
     *
     * @param userId User UUID
     */
    void deleteByUserId(UUID userId);
}
