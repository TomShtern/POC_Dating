package com.dating.user.service;

import com.dating.user.dto.request.LoginRequest;
import com.dating.user.dto.request.RegisterRequest;
import com.dating.user.dto.response.AuthResponse;
import com.dating.user.dto.response.UserResponse;
import com.dating.user.event.UserEventPublisher;
import com.dating.user.exception.InvalidCredentialsException;
import com.dating.user.exception.UserAlreadyExistsException;
import com.dating.user.mapper.UserMapper;
import com.dating.user.model.User;
import com.dating.user.repository.UserPreferenceRepository;
import com.dating.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .email("test@example.com")
                .username("testuser")
                .password("Password123!")
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("MALE")
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .build();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .username("testuser")
                .passwordHash("$2a$12$hashedpassword")
                .firstName("John")
                .lastName("Doe")
                .status("ACTIVE")
                .build();

        userResponse = UserResponse.builder()
                .id(testUser.getId().toString())
                .email("test@example.com")
                .username("testuser")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    @Test
    @DisplayName("Should register user successfully")
    void testRegister_Success() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("$2a$12$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userPreferenceRepository.save(any())).thenReturn(null);
        when(tokenService.generateAccessToken(testUser.getId())).thenReturn("access_token");
        when(tokenService.createRefreshToken(testUser)).thenReturn("refresh_token");
        when(tokenService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(userMapper.toUserResponse(testUser)).thenReturn(userResponse);

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals("test@example.com", response.getUser().getEmail());

        verify(userRepository).save(any(User.class));
        verify(userPreferenceRepository).save(any());
        verify(eventPublisher).publishUserRegistered(testUser);
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void testRegister_EmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Act & Assert
        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(registerRequest));

        assertTrue(exception.getMessage().contains("Email already registered"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void testRegister_UsernameAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        // Act & Assert
        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(registerRequest));

        assertTrue(exception.getMessage().contains("Username already taken"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login user successfully")
    void testLogin_Success() {
        // Arrange
        when(userRepository.findActiveByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPasswordHash())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenService.generateAccessToken(testUser.getId())).thenReturn("access_token");
        when(tokenService.createRefreshToken(testUser)).thenReturn("refresh_token");
        when(tokenService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(userMapper.toUserResponse(testUser)).thenReturn(userResponse);

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals("test@example.com", response.getUser().getEmail());
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testLogin_UserNotFound() {
        // Arrange
        when(userRepository.findActiveByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest));

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when password is invalid")
    void testLogin_InvalidPassword() {
        // Arrange
        when(userRepository.findActiveByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPasswordHash())).thenReturn(false);

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest));

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    @DisplayName("Should logout user successfully")
    void testLogout_Success() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        authService.logout(userId);

        // Assert
        verify(tokenService).revokeAllUserTokens(userId);
    }
}
