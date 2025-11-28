package com.dating.match.service;

import com.dating.common.constant.SwipeType;
import com.dating.match.client.UserServiceClient;
import com.dating.match.dto.request.SwipeRequest;
import com.dating.match.dto.response.SwipeResponse;
import com.dating.match.event.MatchEventPublisher;
import com.dating.match.exception.DuplicateSwipeException;
import com.dating.match.exception.InvalidSwipeException;
import com.dating.match.model.Match;
import com.dating.match.model.Swipe;
import com.dating.match.repository.MatchRepository;
import com.dating.match.repository.SwipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SwipeService.
 */
@ExtendWith(MockitoExtension.class)
class SwipeServiceTest {

    @Mock
    private SwipeRepository swipeRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private MatchEventPublisher eventPublisher;

    @InjectMocks
    private SwipeService swipeService;

    private UUID userId;
    private UUID targetUserId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should record a LIKE swipe without match")
    void recordSwipe_Like_NoMatch() {
        // Arrange
        SwipeRequest request = new SwipeRequest(targetUserId, SwipeType.LIKE);

        when(swipeRepository.existsByUserIdAndTargetUserId(userId, targetUserId)).thenReturn(false);

        Swipe savedSwipe = Swipe.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .targetUserId(targetUserId)
                .action(SwipeType.LIKE)
                .createdAt(Instant.now())
                .build();
        when(swipeRepository.save(any(Swipe.class))).thenReturn(savedSwipe);

        when(swipeRepository.findMutualLike(targetUserId, userId)).thenReturn(Optional.empty());

        // Act
        SwipeResponse response = swipeService.recordSwipe(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(savedSwipe.getId(), response.id());
        assertEquals(userId, response.userId());
        assertEquals(targetUserId, response.targetUserId());
        assertEquals(SwipeType.LIKE, response.action());
        assertFalse(response.isMatch());
        assertNull(response.matchId());

        verify(swipeRepository).save(any(Swipe.class));
        verify(swipeRepository).findMutualLike(targetUserId, userId);
        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    @DisplayName("Should record a LIKE swipe and create match when mutual")
    void recordSwipe_Like_WithMatch() {
        // Arrange
        SwipeRequest request = new SwipeRequest(targetUserId, SwipeType.LIKE);

        when(swipeRepository.existsByUserIdAndTargetUserId(userId, targetUserId)).thenReturn(false);

        Swipe savedSwipe = Swipe.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .targetUserId(targetUserId)
                .action(SwipeType.LIKE)
                .createdAt(Instant.now())
                .build();
        when(swipeRepository.save(any(Swipe.class))).thenReturn(savedSwipe);

        // Mutual like exists
        Swipe mutualSwipe = Swipe.builder()
                .id(UUID.randomUUID())
                .userId(targetUserId)
                .targetUserId(userId)
                .action(SwipeType.LIKE)
                .createdAt(Instant.now())
                .build();
        when(swipeRepository.findMutualLike(targetUserId, userId)).thenReturn(Optional.of(mutualSwipe));

        // Match does not exist
        when(matchRepository.existsActiveMatchBetweenUsers(any(), any())).thenReturn(false);

        Match savedMatch = Match.builder()
                .id(UUID.randomUUID())
                .user1Id(targetUserId.compareTo(userId) < 0 ? targetUserId : userId)
                .user2Id(targetUserId.compareTo(userId) < 0 ? userId : targetUserId)
                .matchedAt(Instant.now())
                .build();
        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);

        // Mock user service calls
        when(userServiceClient.getUserById(any())).thenReturn(
                new UserServiceClient.UserProfileResponse(
                        userId, "test@test.com", "testuser", "Test", "User",
                        25, "MALE", "Bio", "http://photo.jpg", "ACTIVE"));

        // Act
        SwipeResponse response = swipeService.recordSwipe(userId, request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isMatch());
        assertNotNull(response.matchId());
        assertNotNull(response.matchedAt());

        verify(matchRepository).save(any(Match.class));
        verify(eventPublisher).publishMatchCreated(any(Match.class), anyString(), anyString());
    }

    @Test
    @DisplayName("Should record a PASS swipe without checking for match")
    void recordSwipe_Pass() {
        // Arrange
        SwipeRequest request = new SwipeRequest(targetUserId, SwipeType.PASS);

        when(swipeRepository.existsByUserIdAndTargetUserId(userId, targetUserId)).thenReturn(false);

        Swipe savedSwipe = Swipe.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .targetUserId(targetUserId)
                .action(SwipeType.PASS)
                .createdAt(Instant.now())
                .build();
        when(swipeRepository.save(any(Swipe.class))).thenReturn(savedSwipe);

        // Act
        SwipeResponse response = swipeService.recordSwipe(userId, request);

        // Assert
        assertNotNull(response);
        assertFalse(response.isMatch());
        assertEquals(SwipeType.PASS, response.action());

        verify(swipeRepository).save(any(Swipe.class));
        verify(swipeRepository, never()).findMutualLike(any(), any());
    }

    @Test
    @DisplayName("Should throw DuplicateSwipeException when already swiped")
    void recordSwipe_DuplicateSwipe() {
        // Arrange
        SwipeRequest request = new SwipeRequest(targetUserId, SwipeType.LIKE);

        when(swipeRepository.existsByUserIdAndTargetUserId(userId, targetUserId)).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateSwipeException.class, () ->
                swipeService.recordSwipe(userId, request));

        verify(swipeRepository, never()).save(any(Swipe.class));
    }

    @Test
    @DisplayName("Should throw InvalidSwipeException when swiping on self")
    void recordSwipe_SelfSwipe() {
        // Arrange
        SwipeRequest request = new SwipeRequest(userId, SwipeType.LIKE);

        // Act & Assert
        assertThrows(InvalidSwipeException.class, () ->
                swipeService.recordSwipe(userId, request));

        verify(swipeRepository, never()).save(any(Swipe.class));
    }

    @Test
    @DisplayName("Should record SUPER_LIKE swipe and check for match")
    void recordSwipe_SuperLike() {
        // Arrange
        SwipeRequest request = new SwipeRequest(targetUserId, SwipeType.SUPER_LIKE);

        when(swipeRepository.existsByUserIdAndTargetUserId(userId, targetUserId)).thenReturn(false);

        Swipe savedSwipe = Swipe.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .targetUserId(targetUserId)
                .action(SwipeType.SUPER_LIKE)
                .createdAt(Instant.now())
                .build();
        when(swipeRepository.save(any(Swipe.class))).thenReturn(savedSwipe);

        when(swipeRepository.findMutualLike(targetUserId, userId)).thenReturn(Optional.empty());

        // Act
        SwipeResponse response = swipeService.recordSwipe(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(SwipeType.SUPER_LIKE, response.action());
        assertFalse(response.isMatch());

        verify(swipeRepository).findMutualLike(targetUserId, userId);
    }
}
