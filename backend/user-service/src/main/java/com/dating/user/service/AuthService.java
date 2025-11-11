package com.dating.user.service;

/**
 * Authentication Service Interface
 *
 * PURPOSE: Business logic for user authentication operations
 *
 * METHODS TO IMPLEMENT:
 * register(RegisterRequest): AuthResponse
 *   - Validate email doesn't exist
 *   - Validate username doesn't exist
 *   - Validate password strength
 *   - Hash password with BCrypt
 *   - Create User entity
 *   - Create UserPreferences (default values)
 *   - Save to database
 *   - Generate JWT and refresh token
 *   - Return AuthResponse
 *   - Publish user:registered event
 *
 * login(LoginRequest): AuthResponse
 *   - Find user by email
 *   - Throw UserNotFoundException if not found
 *   - Check if user is suspended/deleted
 *   - Verify password hash
 *   - Update last_login timestamp
 *   - Generate JWT and refresh token
 *   - Save refresh token to database
 *   - Return AuthResponse
 *
 * refreshToken(String refreshToken): TokenResponse
 *   - Validate refresh token format
 *   - Look up refresh token in database
 *   - Verify not revoked
 *   - Verify not expired
 *   - Extract userId from token
 *   - Generate new JWT
 *   - Return TokenResponse
 *
 * logout(UUID userId): void
 *   - Invalidate all refresh tokens for user
 *   - Add JWT to blacklist in Redis
 *   - Clear session cache
 *
 * EXCEPTIONS TO THROW:
 * - UserNotFoundException: User doesn't exist
 * - InvalidCredentialsException: Password mismatch
 * - UserSuspendedException: Account suspended
 * - InvalidTokenException: Token validation failed
 * - ValidationException: Input validation failed
 *
 * DEPENDENCIES:
 * - UserRepository: Database access
 * - JwtProvider: JWT operations
 * - PasswordEncoder: BCrypt operations
 * - EventPublisher: RabbitMQ
 * - RedisCache: Token blacklist
 */
public interface AuthService {
    // TODO: Define interface methods
    // TODO: Create implementation class (AuthServiceImpl)
}
