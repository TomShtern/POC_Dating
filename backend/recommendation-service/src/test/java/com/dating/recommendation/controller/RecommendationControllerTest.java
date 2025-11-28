package com.dating.recommendation.controller;

import com.dating.recommendation.dto.CandidateProfileDTO;
import com.dating.recommendation.dto.ScoredCandidate;
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
    private List<ScoredCandidate> recommendations;
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
        CandidateProfileDTO candidateProfile = new CandidateProfileDTO(
                targetId,
                "testcandidate",
                28,
                "https://example.com/photo.jpg",
                "Test bio",
                Set.of("hiking", "music"),
                "New York",
                "USA",
                "FEMALE"
        );

        scoredCandidate = new ScoredCandidate(
                candidateProfile,
                0.85,
                Map.of("age-compatibility", 0.9, "location", 0.8)
        );

        recommendations = List.of(scoredCandidate);
    }

    @Nested
    @DisplayName("GET /api/recommendations/users/{userId}")
    class GetRecommendationsEndpointTests {

        @Test
        @DisplayName("Should return 200 with recommendations")
        void testGetRecommendations_Success() throws Exception {
            // Arrange
            when(recommendationService.getRecommendations(userId)).thenReturn(recommendations);

            // Act & Assert
            mockMvc.perform(get("/api/recommendations/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].finalScore").value(0.85))
                    .andExpect(jsonPath("$[0].candidate.username").value("testcandidate"));

            verify(recommendationService).getRecommendations(userId);
        }

        @Test
        @DisplayName("Should return 200 with empty list when no recommendations")
        void testGetRecommendations_EmptyList() throws Exception {
            // Arrange
            when(recommendationService.getRecommendations(userId)).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/recommendations/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void testGetRecommendations_UserNotFound() throws Exception {
            // Arrange
            when(recommendationService.getRecommendations(userId))
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
            when(recommendationService.getCompatibilityScore(userId, targetId))
                    .thenReturn(scoredCandidate);

            // Act & Assert
            mockMvc.perform(get("/api/recommendations/users/{userId}/{targetId}/score",
                            userId, targetId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.finalScore").value(0.85))
                    .andExpect(jsonPath("$.scoreBreakdown").exists());

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
            doNothing().when(recommendationService).invalidateRecommendations(userId);
            when(recommendationService.getRecommendations(userId)).thenReturn(recommendations);

            // Act & Assert
            mockMvc.perform(post("/api/recommendations/users/{userId}/refresh", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(recommendationService).invalidateRecommendations(userId);
            verify(recommendationService).getRecommendations(userId);
        }

        @Test
        @DisplayName("Should return 404 when user not found during refresh")
        void testRefreshRecommendations_UserNotFound() throws Exception {
            // Arrange
            doNothing().when(recommendationService).invalidateRecommendations(userId);
            when(recommendationService.getRecommendations(userId))
                    .thenThrow(new UserNotFoundException(userId));

            // Act & Assert
            mockMvc.perform(post("/api/recommendations/users/{userId}/refresh", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/recommendations/algorithm/info")
    class GetAlgorithmInfoEndpointTests {

        @Test
        @DisplayName("Should return 200 with algorithm info")
        void testGetAlgorithmInfo_Success() throws Exception {
            // Arrange
            Map<String, Object> info = new HashMap<>();
            info.put("batchSize", 20);
            info.put("minimumScore", 0.3);
            info.put("scorers", Map.of("age", 0.2, "location", 0.3));
            info.put("activeScorerCount", 5);

            when(recommendationService.getAlgorithmInfo()).thenReturn(info);

            // Act & Assert
            mockMvc.perform(get("/api/recommendations/algorithm/info")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.batchSize").value(20))
                    .andExpect(jsonPath("$.minimumScore").value(0.3))
                    .andExpect(jsonPath("$.activeScorerCount").value(5))
                    .andExpect(jsonPath("$.scorers").exists());

            verify(recommendationService).getAlgorithmInfo();
        }
    }

    @Nested
    @DisplayName("Error handling tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return 500 for unexpected errors")
        void testUnexpectedException() throws Exception {
            // Arrange
            when(recommendationService.getRecommendations(userId))
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
