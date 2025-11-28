package com.dating.user.service;

import com.dating.user.dto.request.UpdateUserRequest;
import com.dating.user.dto.response.UserResponse;
import com.dating.user.event.UserEventPublisher;
import com.dating.user.exception.UserNotFoundException;
import com.dating.user.mapper.UserMapper;
import com.dating.user.model.User;
import com.dating.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserEventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserResponse userResponse;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .username("testuser")
                .passwordHash("$2a$12$hashedpassword")
                .firstName("John")
                .lastName("Doe")
                .status("ACTIVE")
                .build();

        userResponse = UserResponse.builder()
                .id(userId)
                .email("test@example.com")
                .username("testuser")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void testGetUserById_Success() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userMapper.toUserResponse(testUser)).thenReturn(userResponse);

        // Act
        UserResponse response = userService.getUserById(userId);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("test@example.com", response.getEmail());
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testGetUserById_NotFound() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserById(userId));

        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    @DisplayName("Should update user successfully")
    void testUpdateUser_Success() {
        // Arrange
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .bio("New bio")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse updatedResponse = UserResponse.builder()
                .id(userId)
                .email("test@example.com")
                .username("testuser")
                .firstName("Jane")
                .lastName("Smith")
                .bio("New bio")
                .build();
        when(userMapper.toUserResponse(any(User.class))).thenReturn(updatedResponse);

        // Act
        UserResponse response = userService.updateUser(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals("Jane", response.getFirstName());
        assertEquals("Smith", response.getLastName());
        assertEquals("New bio", response.getBio());

        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishUserUpdated(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent user")
    void testUpdateUser_NotFound() {
        // Arrange
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Jane")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUser(userId, request));

        assertTrue(exception.getMessage().contains("User not found"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete user successfully (soft delete)")
    void testDeleteUser_Success() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.deleteUser(userId);

        // Assert
        assertEquals("DELETED", testUser.getStatus());
        verify(userRepository).save(testUser);
        verify(eventPublisher).publishUserDeleted(userId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent user")
    void testDeleteUser_NotFound() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.deleteUser(userId));

        assertTrue(exception.getMessage().contains("User not found"));
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishUserDeleted(any());
    }

    @Test
    @DisplayName("Should update only provided fields")
    void testUpdateUser_PartialUpdate() {
        // Arrange
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Jane")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

        // Act
        userService.updateUser(userId, request);

        // Assert
        assertEquals("Jane", testUser.getFirstName());
        assertEquals("Doe", testUser.getLastName()); // Should remain unchanged
        verify(userRepository).save(testUser);
    }
}
