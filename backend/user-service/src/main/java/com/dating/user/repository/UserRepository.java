package com.dating.user.repository;

import com.dating.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 * Handles all database operations for users.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email.
     *
     * @param email User email
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username.
     *
     * @param username Username
     * @return Optional containing user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if email already exists.
     *
     * @param email Email to check
     * @return true if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if username already exists.
     *
     * @param username Username to check
     * @return true if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Find user by email and status.
     *
     * @param email User email
     * @param status User status
     * @return Optional containing user if found
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = :status")
    Optional<User> findByEmailAndStatus(@Param("email") String email, @Param("status") String status);

    /**
     * Find active user by email.
     *
     * @param email User email
     * @return Optional containing user if found and active
     */
    default Optional<User> findActiveByEmail(String email) {
        return findByEmailAndStatus(email, "ACTIVE");
    }

    /**
     * Find candidate users for matching.
     * Filters by age range and excludes specified user IDs.
     *
     * @param userId User ID to exclude (the requesting user)
     * @param excludeIds List of user IDs to exclude (already swiped, etc.)
     * @param minBirthDate Maximum birth date (for minimum age)
     * @param maxBirthDate Minimum birth date (for maximum age)
     * @return List of candidate users
     */
    @Query("SELECT u FROM User u WHERE u.id != :userId " +
           "AND u.id NOT IN :excludeIds " +
           "AND u.status = 'ACTIVE' " +
           "AND u.dateOfBirth IS NOT NULL " +
           "AND u.dateOfBirth <= :minBirthDate " +
           "AND u.dateOfBirth >= :maxBirthDate")
    List<User> findCandidates(
            @Param("userId") UUID userId,
            @Param("excludeIds") List<UUID> excludeIds,
            @Param("minBirthDate") LocalDate minBirthDate,
            @Param("maxBirthDate") LocalDate maxBirthDate);

    /**
     * Find multiple users by their IDs.
     *
     * @param ids List of user UUIDs
     * @return List of users matching the IDs
     */
    List<User> findByIdIn(List<UUID> ids);

    /**
     * Find users who logged in after a given time.
     * Used for cache warming to identify recently active users.
     *
     * @param threshold Time threshold
     * @param pageable Pagination
     * @return Page of recently active users
     */
    Page<User> findByLastLoginAtAfterOrderByLastLoginAtDesc(Instant threshold, Pageable pageable);
}
