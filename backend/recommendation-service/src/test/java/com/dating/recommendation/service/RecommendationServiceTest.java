package com.dating.recommendation.service;

import com.dating.recommendation.client.UserServiceClient;
import com.dating.recommendation.dto.UserProfileDto;
import com.dating.recommendation.dto.response.BatchScoreResponse;
import com.dating.recommendation.dto.response.RecommendationListResponse;
import com.dating.recommendation.dto.response.ScoreFactors;
import com.dating.recommendation.dto.response.ScoreResponse;
import com.dating.recommendation.mapper.RecommendationMapper;
import com.dating.recommendation.model.Recommendation;
import com.dating.recommendation.repository.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ScoringService scoringService;

    @Mock
    private PreferenceAnalyzerService preferenceAnalyzerService;

    @Mock
    private RecommendationMapper recommendationMapper;

    @InjectMocks
    private RecommendationService recommendationService;

    private UUID userId;
    private UUID targetUserId;
    private UserProfileDto sourceUser;
    private UserProfileDto targetUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();

        sourceUser = UserProfileDto.builder()
                .id(userId)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("MALE")
                .bio("Test bio")
                .lastLogin(Instant.now())
                .minAge(25)
                .maxAge(35)
                .maxDistanceKm(50)
                .interestedIn("FEMALE")
                .interests(Arrays.asList("hiking", "movies"))
                .build();

        targetUser = UserProfileDto.builder()
                .id(targetUserId)
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.of(1992, 5, 15))
                .gender("FEMALE")
                .bio("Test bio")
                .lastLogin(Instant.now())
                .interests(Arrays.asList("hiking", "cooking"))
                .build();

        // Set configuration values
        ReflectionTestUtils.setField(recommendationService, "defaultAlgorithm", "v1");
        ReflectionTestUtils.setField(recommendationService, "cacheTtlHours", 24);
        ReflectionTestUtils.setField(recommendationService, "defaultLimit", 10);
        ReflectionTestUtils.setField(recommendationService, "maxLimit", 50);
    }

    @Test
    void testGetCompatibilityScore_Success() {
        // Arrange
        when(userServiceClient.getUserById(userId)).thenReturn(sourceUser);
        when(userServiceClient.getUserById(targetUserId)).thenReturn(targetUser);
        when(preferenceAnalyzerService.enrichWithActivityStats(any())).thenAnswer(i -> i.getArgument(0));

        ScoreFactors factors = ScoreFactors.builder()
                .profileCompleteness(10.0)
                .preferenceMatch(30.0)
                .activity(15.0)
                .mlPrediction(15.0)
                .build();

        when(scoringService.calculateFactors(any(), any(), anyString())).thenReturn(factors);

        // Act
        ScoreResponse response = recommendationService.getCompatibilityScore(userId, targetUserId);

        // Assert
        assertNotNull(response);
        assertEquals(70, response.getScore());
        assertNotNull(response.getFactors());
        assertNotNull(response.getCalculatedAt());
    }

    @Test
    void testScoreProfiles_Success() {
        // Arrange
        List<UUID> candidateIds = Arrays.asList(targetUserId);

        when(userServiceClient.getUserById(userId)).thenReturn(sourceUser);
        when(userServiceClient.getUsersByIds(anyList())).thenReturn(Arrays.asList(targetUser));
        when(preferenceAnalyzerService.enrichWithActivityStats(any())).thenAnswer(i -> i.getArgument(0));

        Map<UUID, Integer> scores = new HashMap<>();
        scores.put(targetUserId, 75);

        Map<UUID, ScoreFactors> factors = new HashMap<>();
        factors.put(targetUserId, ScoreFactors.builder().build());

        BatchScoreResponse batchResponse = BatchScoreResponse.builder()
                .scores(scores)
                .factors(factors)
                .build();

        when(scoringService.scoreMultiple(any(), anyList(), anyString())).thenReturn(batchResponse);

        // Act
        BatchScoreResponse response = recommendationService.scoreProfiles(userId, candidateIds, "v1");

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getScores().size());
        assertTrue(response.getScores().containsKey(targetUserId));
        assertEquals(75, response.getScores().get(targetUserId));
    }

    @Test
    void testRefreshRecommendations_DeletesExisting() {
        // Arrange & Act
        recommendationService.refreshRecommendations(userId);

        // Assert
        verify(recommendationRepository, times(1)).deleteByUserId(userId);
    }

    @Test
    void testCleanupExpiredRecommendations_Success() {
        // Arrange
        when(recommendationRepository.deleteExpiredRecommendations(any())).thenReturn(5);

        // Act
        int deleted = recommendationService.cleanupExpiredRecommendations();

        // Assert
        assertEquals(5, deleted);
        verify(recommendationRepository, times(1)).deleteExpiredRecommendations(any());
    }

    @Test
    void testGetRecommendations_UsesCache() {
        // Arrange
        List<Recommendation> cachedRecs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UUID recUserId = UUID.randomUUID();
            Recommendation rec = Recommendation.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .recommendedUserId(recUserId)
                    .score(BigDecimal.valueOf(80 - i))
                    .algorithmVersion("v1")
                    .factors(new HashMap<>())
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            cachedRecs.add(rec);
        }

        when(recommendationRepository.findActiveRecommendations(eq(userId), any()))
                .thenReturn(cachedRecs);

        List<UUID> userIds = cachedRecs.stream()
                .map(Recommendation::getRecommendedUserId)
                .toList();

        List<UserProfileDto> users = userIds.stream()
                .map(id -> UserProfileDto.builder()
                        .id(id)
                        .firstName("User")
                        .build())
                .toList();

        when(userServiceClient.getUsersByIds(anyList())).thenReturn(users);
        when(scoringService.generateReason(any(), any(), any())).thenReturn("Test reason");
        when(recommendationMapper.toResponse(any(), any(), any(), any()))
                .thenAnswer(i -> com.dating.recommendation.dto.response.RecommendationResponse.builder()
                        .id(UUID.randomUUID())
                        .score(80)
                        .build());

        // Act
        RecommendationListResponse response = recommendationService.getRecommendations(userId, 10, "v1");

        // Assert
        assertNotNull(response);
        verify(recommendationRepository, times(1)).findActiveRecommendations(eq(userId), any());
    }

    @Test
    void testGetRecommendations_LimitsMaxResults() {
        // Arrange
        when(recommendationRepository.findActiveRecommendations(any(), any()))
                .thenReturn(new ArrayList<>());

        when(userServiceClient.getUserById(userId)).thenReturn(sourceUser);
        when(userServiceClient.getCandidates(eq(userId), anyInt())).thenReturn(new ArrayList<>());
        when(preferenceAnalyzerService.enrichWithActivityStats(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        RecommendationListResponse response = recommendationService.getRecommendations(userId, 100, "v1");

        // Assert - limit should be capped at maxLimit (50)
        verify(userServiceClient, times(1)).getCandidates(eq(userId), eq(150)); // 50 * 3
    }
}
