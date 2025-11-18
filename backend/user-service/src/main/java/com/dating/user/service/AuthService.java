package com.dating.user.service;

import com.dating.user.dto.request.LoginRequest;
import com.dating.user.dto.request.RefreshTokenRequest;
import com.dating.user.dto.request.RegisterRequest;
import com.dating.user.dto.response.AuthResponse;
import com.dating.user.event.UserEventPublisher;
import com.dating.user.exception.InvalidCredentialsException;
import com.dating.user.exception.InvalidTokenException;
import com.dating.user.exception.UserAlreadyExistsException;
import com.dating.user.exception.UserNotFoundException;
import com.dating.user.model.User;
import com.dating.user.model.UserPreference;
import com.dating.user.repository.UserPreferenceRepository;
import com.dating.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for authentication operations (register, login, refresh, logout).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserEventPublisher eventPublisher;

    /**
     * Register a new user.
     *
     * @param request Registration request
     * @return Auth response with user and tokens
     * @throws UserAlreadyExistsException if email or username already exists
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        // Check for existing email
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        // Check for existing username
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed - username already exists: {}", request.getUsername());
            throw new UserAlreadyExistsException("Username already taken: " + request.getUsername());
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .status("ACTIVE")
                .build();

        User savedUser = userRepository.save(user);

        // Create default preferences
        UserPreference preference = UserPreference.builder()
                .user(savedUser)
                .build();
        userPreferenceRepository.save(preference);

        // Generate tokens
        String accessToken = tokenService.generateAccessToken(savedUser.getId());
        String refreshToken = tokenService.createRefreshToken(savedUser);

        log.info("User registered successfully: {}", savedUser.getId());

        // Publish event
        eventPublisher.publishUserRegistered(savedUser.getId(), savedUser.getEmail(), savedUser.getUsername());

        return AuthResponse.of(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getUsername(),
                accessToken,
                refreshToken,
                tokenService.getAccessTokenExpirationSeconds()
        );
    }

    /**
     * Login user with email and password.
     *
     * @param request Login request
     * @return Auth response with user and tokens
     * @throws UserNotFoundException if user not found
     * @throws InvalidCredentialsException if password is incorrect
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.debug("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findActiveByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found: {}", request.getEmail());
                    return new InvalidCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed - invalid password for: {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Update last login
        user.setLastLogin(Instant.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = tokenService.generateAccessToken(user.getId());
        String refreshToken = tokenService.createRefreshToken(user);

        log.info("User logged in: {}", user.getId());

        return AuthResponse.of(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                accessToken,
                refreshToken,
                tokenService.getAccessTokenExpirationSeconds()
        );
    }

    /**
     * Refresh access token using refresh token.
     *
     * @param request Refresh token request
     * @return Auth response with new tokens
     * @throws InvalidTokenException if refresh token is invalid
     */
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        log.debug("Token refresh request");

        if (!tokenService.validateRefreshToken(request.getRefreshToken())) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        UUID userId = tokenService.getUserIdFromRefreshToken(request.getRefreshToken());

        User user = userRepository.findById(userId)
                .filter(u -> "ACTIVE".equals(u.getStatus()))
                .orElseThrow(() -> new UserNotFoundException("User not found or inactive: " + userId));

        // Revoke old tokens and generate new ones
        tokenService.revokeAllUserTokens(userId);
        String accessToken = tokenService.generateAccessToken(userId);
        String refreshToken = tokenService.createRefreshToken(user);

        log.info("Token refreshed for user: {}", userId);

        return AuthResponse.of(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                accessToken,
                refreshToken,
                tokenService.getAccessTokenExpirationSeconds()
        );
    }

    /**
     * Logout user by revoking all refresh tokens.
     *
     * @param userId User UUID
     */
    @Transactional
    public void logout(UUID userId) {
        log.info("User logout: {}", userId);
        tokenService.revokeAllUserTokens(userId);
    }
}
