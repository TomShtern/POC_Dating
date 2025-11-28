package com.dating.match.service;

import com.dating.match.client.RecommendationServiceClient;
import com.dating.match.client.UserServiceClient;
import com.dating.match.dto.response.FeedResponse;
import com.dating.match.repository.SwipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FeedService.
 */
@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private SwipeRepository swipeRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private RecommendationServiceClient recommendationServiceClient;

    @InjectMocks
    private FeedService feedService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should return feed with recommended users")
    void getFeed_Success() {
        // Arrange
        UUID recUser1 = UUID.randomUUID();
        UUID recUser2 = UUID.randomUUID();

        when(swipeRepository.findSwipedUserIdsByUserId(userId)).thenReturn(List.of());

        RecommendationServiceClient.RecommendationsResponse recommendations =
                new RecommendationServiceClient.RecommendationsResponse(
                        List.of(
                                new RecommendationServiceClient.RecommendedUser(
                                        recUser1, 85, Map.of(), "High compatibility"),
                                new RecommendationServiceClient.RecommendedUser(
                                        recUser2, 75, Map.of(), "Good match")
                        ),
                        2,
                        false
                );

        when(recommendationServiceClient.getRecommendations(eq(userId), anyInt()))
                .thenReturn(recommendations);

        when(userServiceClient.getUserById(recUser1)).thenReturn(
                new UserServiceClient.UserProfileResponse(
                        recUser1, "user1@test.com", "user1", "User", "One",
                        25, "FEMALE", "Bio 1", "http://photo1.jpg", "ACTIVE"));

        when(userServiceClient.getUserById(recUser2)).thenReturn(
                new UserServiceClient.UserProfileResponse(
                        recUser2, "user2@test.com", "user2", "User", "Two",
                        28, "FEMALE", "Bio 2", "http://photo2.jpg", "ACTIVE"));

        // Act
        FeedResponse response = feedService.getFeed(userId, 10, 0);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.feed().size());
        assertEquals(2, response.total());
        assertFalse(response.hasMore());

        assertEquals(85, response.feed().get(0).compatibilityScore());
        assertEquals(75, response.feed().get(1).compatibilityScore());
    }

    @Test
    @DisplayName("Should exclude already swiped users from feed")
    void getFeed_ExcludesSwipedUsers() {
        // Arrange
        UUID swipedUser = UUID.randomUUID();
        UUID recUser = UUID.randomUUID();

        when(swipeRepository.findSwipedUserIdsByUserId(userId))
                .thenReturn(List.of(swipedUser));

        RecommendationServiceClient.RecommendationsResponse recommendations =
                new RecommendationServiceClient.RecommendationsResponse(
                        List.of(
                                new RecommendationServiceClient.RecommendedUser(
                                        swipedUser, 90, Map.of(), "Already swiped"),
                                new RecommendationServiceClient.RecommendedUser(
                                        recUser, 75, Map.of(), "Good match")
                        ),
                        2,
                        false
                );

        when(recommendationServiceClient.getRecommendations(eq(userId), anyInt()))
                .thenReturn(recommendations);

        when(userServiceClient.getUserById(recUser)).thenReturn(
                new UserServiceClient.UserProfileResponse(
                        recUser, "user@test.com", "user", "User", "Name",
                        25, "FEMALE", "Bio", "http://photo.jpg", "ACTIVE"));

        // Act
        FeedResponse response = feedService.getFeed(userId, 10, 0);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.feed().size());
        assertEquals(recUser, response.feed().get(0).id());
    }

    @Test
    @DisplayName("Should return empty feed when no recommendations")
    void getFeed_Empty() {
        // Arrange
        when(swipeRepository.findSwipedUserIdsByUserId(userId)).thenReturn(List.of());

        RecommendationServiceClient.RecommendationsResponse emptyRecommendations =
                new RecommendationServiceClient.RecommendationsResponse(List.of(), 0, false);

        when(recommendationServiceClient.getRecommendations(eq(userId), anyInt()))
                .thenReturn(emptyRecommendations);

        // Act
        FeedResponse response = feedService.getFeed(userId, 10, 0);

        // Assert
        assertNotNull(response);
        assertTrue(response.feed().isEmpty());
        assertEquals(0, response.total());
        assertFalse(response.hasMore());
    }

    @Test
    @DisplayName("Should handle pagination correctly")
    void getFeed_Pagination() {
        // Arrange
        UUID recUser1 = UUID.randomUUID();
        UUID recUser2 = UUID.randomUUID();
        UUID recUser3 = UUID.randomUUID();

        when(swipeRepository.findSwipedUserIdsByUserId(userId)).thenReturn(List.of());

        RecommendationServiceClient.RecommendationsResponse recommendations =
                new RecommendationServiceClient.RecommendationsResponse(
                        List.of(
                                new RecommendationServiceClient.RecommendedUser(
                                        recUser1, 90, Map.of(), "User 1"),
                                new RecommendationServiceClient.RecommendedUser(
                                        recUser2, 85, Map.of(), "User 2"),
                                new RecommendationServiceClient.RecommendedUser(
                                        recUser3, 80, Map.of(), "User 3")
                        ),
                        3,
                        false
                );

        when(recommendationServiceClient.getRecommendations(eq(userId), anyInt()))
                .thenReturn(recommendations);

        when(userServiceClient.getUserById(recUser2)).thenReturn(
                new UserServiceClient.UserProfileResponse(
                        recUser2, "user2@test.com", "user2", "User", "Two",
                        25, "FEMALE", "Bio 2", "http://photo2.jpg", "ACTIVE"));

        // Act - get second page (offset=1, limit=1)
        FeedResponse response = feedService.getFeed(userId, 1, 1);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.feed().size());
        assertEquals(recUser2, response.feed().get(0).id());
        assertTrue(response.hasMore());
    }

    @Test
    @DisplayName("Should handle recommendation service failure gracefully")
    void getFeed_RecommendationServiceFailure() {
        // Arrange
        when(swipeRepository.findSwipedUserIdsByUserId(userId)).thenReturn(List.of());

        when(recommendationServiceClient.getRecommendations(eq(userId), anyInt()))
                .thenThrow(new RuntimeException("Service unavailable"));

        // Act
        FeedResponse response = feedService.getFeed(userId, 10, 0);

        // Assert
        assertNotNull(response);
        assertTrue(response.feed().isEmpty());
        assertEquals(0, response.total());
        assertFalse(response.hasMore());
    }

    @Test
    @DisplayName("Should handle user service failure gracefully")
    void getFeed_UserServiceFailure() {
        // Arrange
        UUID recUser = UUID.randomUUID();

        when(swipeRepository.findSwipedUserIdsByUserId(userId)).thenReturn(List.of());

        RecommendationServiceClient.RecommendationsResponse recommendations =
                new RecommendationServiceClient.RecommendationsResponse(
                        List.of(
                                new RecommendationServiceClient.RecommendedUser(
                                        recUser, 85, Map.of(), "User")
                        ),
                        1,
                        false
                );

        when(recommendationServiceClient.getRecommendations(eq(userId), anyInt()))
                .thenReturn(recommendations);

        when(userServiceClient.getUserById(recUser))
                .thenThrow(new RuntimeException("Service unavailable"));

        // Act
        FeedResponse response = feedService.getFeed(userId, 10, 0);

        // Assert
        assertNotNull(response);
        // User should be filtered out due to error
        assertTrue(response.feed().isEmpty());
    }
}
