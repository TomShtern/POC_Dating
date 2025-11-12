package com.dating.recommendation.repository;

import com.dating.recommendation.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for UserPreferences entity.
 * Provides database access methods for user preferences.
 */
@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    /**
     * Find preferences by user ID.
     *
     * @param userId the user ID
     * @return Optional containing the user's preferences if found
     */
    Optional<UserPreferences> findByUserId(Long userId);

    /**
     * Check if preferences exist for a user.
     *
     * @param userId the user ID
     * @return true if preferences exist, false otherwise
     */
    boolean existsByUserId(Long userId);

    /**
     * Delete preferences by user ID.
     *
     * @param userId the user ID
     */
    void deleteByUserId(Long userId);
}
