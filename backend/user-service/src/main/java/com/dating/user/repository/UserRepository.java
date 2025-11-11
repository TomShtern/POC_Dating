package com.dating.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * User Repository
 *
 * PURPOSE: Spring Data JPA repository for User entity
 *
 * CUSTOM QUERY METHODS TO IMPLEMENT:
 * Optional<User> findByEmail(String email)
 *   - Query: SELECT * FROM users WHERE email = ? AND status != 'DELETED'
 *   - Usage: Verify email uniqueness during registration
 *   - Performance: Indexed column
 *
 * Optional<User> findByUsername(String username)
 *   - Query: SELECT * FROM users WHERE username = ? AND status != 'DELETED'
 *   - Usage: Find user by username for login
 *   - Performance: Indexed column
 *
 * Page<User> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable)
 *   - Query: SELECT * FROM users WHERE status = ? ORDER BY created_at DESC
 *   - Usage: Admin user listing
 *   - Performance: Composite index on (status, created_at)
 *
 * List<User> findUsersMatchingPreferences(UUID userId)
 *   - Complex query: Join users, user_preferences, apply filters
 *   - Usage: Feed generation in match service
 *   - Performance: May need @Query annotation with optimized SQL
 *
 * INHERITED METHODS (from JpaRepository):
 * findById(UUID id): Optional<User>
 * findAll(): List<User>
 * save(User user): User
 * delete(User user): void
 * deleteById(UUID id): void
 *
 * NOTES:
 * - All queries should exclude deleted users (status != 'DELETED')
 * - Consider soft deletes in queries
 * - Add @Query annotations for complex queries
 * - Use projection for performance where needed
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // TODO: Add custom query methods
    // TODO: Add @Query annotations where needed
    // TODO: Document performance considerations
}
