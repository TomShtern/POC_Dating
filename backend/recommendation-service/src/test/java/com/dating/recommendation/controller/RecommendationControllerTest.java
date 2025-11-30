package com.dating.recommendation.controller;

import com.dating.recommendation.dto.CandidateProfileDTO;
import com.dating.recommendation.dto.ScoredCandidate;
import com.dating.recommendation.dto.response.RecommendationListResponse;
import com.dating.recommendation.dto.response.RecommendedUserResponse;
import com.dating.recommendation.dto.response.ScoreFactors;
import com.dating.recommendation.dto.response.RecommendationResponse;
import com.dating.recommendation.dto.response.ScoreResponse;
import com.dating.recommendation.exception.GlobalExceptionHandler;
import com.dating.recommendation.exception.UserNotFoundException;
import com.dating.recommendation.service.RecommendationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for RecommendationController.
 * Tests REST endpoints, status codes, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private RecommendationController recommendationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID userId;
    private UUID targetId;
    private RecommendationListResponse recommendationListResponse;
    private ScoredCandidate scoredCandidate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(recommendationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        userId = UUID.randomUUID();
        targetId = UUID.randomUUID();

        // Create test data
        RecommendedUserResponse recommendedUser = RecommendedUserResponse.builder()
                .id(targetId)
                .name("testcandidate")
                .age(28)
                .profilePictureUrl("https://example.com/photo.jpg")
                .bio("Test bio")
                .build();

        RecommendationResponse recResponse = RecommendationResponse.builder()
                .id(UUID.randomUUID())
                .recommendedUser(recommendedUser)
                .score(85)
                .scoreFactors(ScoreFactors.builder().build())
                .reason("Test reason")
                .build();

        recommendationListResponse = RecommendationListResponse.builder()
                .recommendations(List.of(recResponse))
                .total(1)
                .build();
    }

    @Nested
    @DisplayName("GET /api/recommendations/users/{userId}")
    class GetRecommendationsEndpointTests {

        @Test
        @DisplayName("Should return 200 with recommendations")
        void testGetRecommendations_Success() throws Exception {
            // Arrange
            when(recommendationService.getRecommendations(eq(userId), any(), any())).thenReturn(recommendationListResponse);

            // Act & Assert
            mockMvc.perform(get("/api/recommendations/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recommendations").isArray())
                    .andExpect(jsonPath("$.recommendations.length()").value(1))
                    .andExpect(jsonPath("$.recommendations[0].score").value(85))
                    .andExpect(jsonPath("$.recommendations[0].recommendedUser.name").value("testcandidate"));

            verify(recommendationService).getRecommendations(eq(userId), any(), any());
        }

        @Test
        @DisplayName("Should return 200 with empty list when no recommendations")
        void testGetRecommendations_EmptyList() throws Exception {
            // Arrange
            RecommendationListResponse emptyResponse = RecommendationListResponse.builder()
                    .recommendations(List.of())
                    .total(0)
                    .build();
            when(recommendationService.getRecommendations(eq(userId), any(), any())).thenReturn(emptyResponse);

            // Act & Assert
            mockMvc.perform(get("/api/recommendations/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recommendations").isArray())
                    .andExpect(jsonPath("$.recommendations.length()").value(0));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void testGetRecommendations_UserNotFound() throws Exception {
            // Arrange
            when(recommendationService.getRecommendations(eq(userId), any(), any()))
                    .thenThrow(new UserNotFoundException(userId));

            // Act & Assert
            mockMvc.perform(get("/api/recommendations/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"));
        }

        @Test
        @DisplayName("Should return 400 for invalid UUID")
        void testGetRecommendations_InvalidUUID() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/recommendations/users/{userId}", "not-a-uuid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }
    }

    @Nested
    @DisplayName("GET /api/recommendations/users/{userId}/{targetId}/score")
    class GetCompatibilityScoreEndpointTests {

        @Test
        @DisplayName("Should return 200 with compatibility score")
        void testGetCompatibilityScore_Success() throws Exception {
            // Arrange
            ScoreResponse scoreResponse = ScoreResponse.builder()
                    .score(85)
                    .factors(Map.of("age-compatibility", 90, "location", 80))
                    .build();

            when(recommendationService.getCompatibilityScore(userId, targetId))
                    .thenReturn(scoreResponse);

            // Act & Assert
            mockMvc.perform(get("/api/recommendations/users/{userId}/{targetId}/score",
                            userId, targetId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.score").value(85))
                    .andExpect(jsonPath("$.factors").exists());

            verify(recommendationService).getCompatibilityScore(userId, targetId);
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void testGetCompatibilityScore_UserNotFound() throws Exception {
            // Arrange
            when(recommendationService.getCompatibilityScore(userId, targetId))
                    .thenThrow(new UserNotFoundException(userId));

            // Act & Assert
            mockMvc.perform(get("/api/recommendations/users/{userId}/{targetId}/score",
                            userId, targetId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Should return 404 when target not found")
        void testGetCompatibilityScore_TargetNotFound() throws Exception {
            // Arrange
            when(recommendationService.getCompatibilityScore(userId, targetId))
                    .thenThrow(new UserNotFoundException(targetId));

            // Act & Assert
            mockMvc.perform(get("/api/recommendations/users/{userId}/{targetId}/score",
                            userId, targetId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"));
        }
    }

    @Nested
    @DisplayName("POST /api/recommendations/users/{userId}/refresh")
    class RefreshRecommendationsEndpointTests {

        @Test
        @DisplayName("Should return 202 Accepted with refreshed recommendations")
        void testRefreshRecommendations_Success() throws Exception {
            // Arrange
            doNothing().when(recommendationService).refreshRecommendations(userId);
            when(recommendationService.getRecommendations(eq(userId), any(), any())).thenReturn(recommendationListResponse);

            // Act & Assert
            mockMvc.perform(post("/api/recommendations/users/{userId}/refresh", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.recommendations").isArray())
                    .andExpect(jsonPath("$.recommendations.length()").value(1));

            verify(recommendationService).refreshRecommendations(userId);
            verify(recommendationService).getRecommendations(eq(userId), any(), any());
        }

        @Test
        @DisplayName("Should return 404 when user not found during refresh")
        void testRefreshRecommendations_UserNotFound() throws Exception {
            // Arrange
            doNothing().when(recommendationService).refreshRecommendations(userId);
            when(recommendationService.getRecommendations(eq(userId), any(), any()))
                    .thenThrow(new UserNotFoundException(userId));

            // Act & Assert
            mockMvc.perform(post("/api/recommendations/users/{userId}/refresh", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Error handling tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return 500 for unexpected errors")
        void testUnexpectedException() throws Exception {
            // Arrange
            when(recommendationService.getRecommendations(eq(userId), any(), any()))
                    .thenThrow(new RuntimeException("Unexpected error"));

            // Act & Assert
            mockMvc.perform(get("/api/recommendations/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error").value("Internal Server Error"))
                    .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
        }
    }
}
