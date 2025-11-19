package com.dating.recommendation.service;

import com.dating.recommendation.dto.ScoredCandidate;
import com.dating.recommendation.exception.UserNotFoundException;
import com.dating.recommendation.model.User;
import com.dating.recommendation.repository.SwipeRepository;
import com.dating.recommendation.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RecommendationService.
 * Tests the core recommendation generation logic.
 */
@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SwipeRepository swipeRepository;

    @Mock
    private ScoreAggregator scoreAggregator;

    @InjectMocks
    private RecommendationService recommendationService;

    private User testUser;
    private User candidate1;
    private User candidate2;
    private UUID userId;
    private UUID candidate1Id;
    private UUID candidate2Id;

    @BeforeEach
    void setUp() {
        // Set configuration values
        ReflectionTestUtils.setField(recommendationService, "batchSize", 20);
        ReflectionTestUtils.setField(recommendationService, "minimumScore", 0.3);

        // Create test users
        userId = UUID.randomUUID();
        candidate1Id = UUID.randomUUID();
        candidate2Id = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .gender("MALE")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .genderPreferences(Set.of("FEMALE"))
                .interests(Set.of("hiking", "music"))
                .active(true)
                .build();

        candidate1 = User.builder()
                .id(candidate1Id)
                .username("candidate1")
                .email("candidate1@example.com")
                .gender("FEMALE")
                .dateOfBirth(LocalDate.of(1992, 5, 15))
                .genderPreferences(Set.of("MALE"))
                .interests(Set.of("hiking", "reading"))
                .active(true)
                .lastActiveAt(LocalDateTime.now())
                .build();

        candidate2 = User.builder()
                .id(candidate2Id)
                .username("candidate2")
                .email("candidate2@example.com")
                .gender("FEMALE")
                .dateOfBirth(LocalDate.of(1995, 8, 20))
                .genderPreferences(Set.of("MALE"))
                .interests(Set.of("cooking", "travel"))
                .active(true)
                .lastActiveAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getRecommendations tests")
    class GetRecommendationsTests {

        @Test
        @DisplayName("Should return scored candidates for valid user")
        void testGetRecommendations_Success() {
            // Arrange
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(swipeRepository.findSwipedUserIds(userId)).thenReturn(new HashSet<>());
            when(userRepository.findCandidates(any())).thenReturn(List.of(candidate1, candidate2));

            ScoredCandidate scored1 = createScoredCandidate(candidate1, 0.8);
            ScoredCandidate scored2 = createScoredCandidate(candidate2, 0.6);
            when(scoreAggregator.aggregate(testUser, candidate1)).thenReturn(scored1);
            when(scoreAggregator.aggregate(testUser, candidate2)).thenReturn(scored2);

            // Act
            List<ScoredCandidate> results = recommendationService.getRecommendations(userId);

            // Assert
            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals(0.8, results.get(0).finalScore(), 0.001);
            assertEquals(0.6, results.get(1).finalScore(), 0.001);

            verify(userRepository).findById(userId);
            verify(swipeRepository).findSwipedUserIds(userId);
            verify(userRepository).findCandidates(any());
        }

        @Test
        @DisplayName("Should throw UserNotFoundException for non-existent user")
        void testGetRecommendations_UserNotFound() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(UserNotFoundException.class, () ->
                    recommendationService.getRecommendations(nonExistentId));

            verify(userRepository).findById(nonExistentId);
            verifyNoInteractions(swipeRepository);
        }

        @Test
        @DisplayName("Should exclude already swiped users")
        void testGetRecommendations_ExcludesSwipedUsers() {
            // Arrange
            Set<UUID> swipedIds = Set.of(candidate1Id);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(swipeRepository.findSwipedUserIds(userId)).thenReturn(swipedIds);
            when(userRepository.findCandidates(any())).thenReturn(List.of(candidate2));

            ScoredCandidate scored2 = createScoredCandidate(candidate2, 0.6);
            when(scoreAggregator.aggregate(testUser, candidate2)).thenReturn(scored2);

            // Act
            List<ScoredCandidate> results = recommendationService.getRecommendations(userId);

            // Assert
            assertEquals(1, results.size());

            // Verify exclude set includes swiped user and self
            verify(userRepository).findCandidates(argThat(excludeIds ->
                    excludeIds.contains(candidate1Id) && excludeIds.contains(userId)));
        }

        @Test
        @DisplayName("Should filter candidates below minimum score")
        void testGetRecommendations_FiltersLowScores() {
            // Arrange
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(swipeRepository.findSwipedUserIds(userId)).thenReturn(new HashSet<>());
            when(userRepository.findCandidates(any())).thenReturn(List.of(candidate1, candidate2));

            ScoredCandidate scored1 = createScoredCandidate(candidate1, 0.8);
            ScoredCandidate scored2 = createScoredCandidate(candidate2, 0.1); // Below threshold
            when(scoreAggregator.aggregate(testUser, candidate1)).thenReturn(scored1);
            when(scoreAggregator.aggregate(testUser, candidate2)).thenReturn(scored2);

            // Act
            List<ScoredCandidate> results = recommendationService.getRecommendations(userId);

            // Assert
            assertEquals(1, results.size());
            assertEquals(0.8, results.get(0).finalScore(), 0.001);
        }

        @Test
        @DisplayName("Should limit results to batch size")
        void testGetRecommendations_LimitsToBatchSize() {
            // Arrange
            ReflectionTestUtils.setField(recommendationService, "batchSize", 1);

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(swipeRepository.findSwipedUserIds(userId)).thenReturn(new HashSet<>());
            when(userRepository.findCandidates(any())).thenReturn(List.of(candidate1, candidate2));

            ScoredCandidate scored1 = createScoredCandidate(candidate1, 0.8);
            ScoredCandidate scored2 = createScoredCandidate(candidate2, 0.6);
            when(scoreAggregator.aggregate(testUser, candidate1)).thenReturn(scored1);
            when(scoreAggregator.aggregate(testUser, candidate2)).thenReturn(scored2);

            // Act
            List<ScoredCandidate> results = recommendationService.getRecommendations(userId);

            // Assert
            assertEquals(1, results.size());
            assertEquals(0.8, results.get(0).finalScore(), 0.001); // Highest score
        }

        @Test
        @DisplayName("Should return empty list when no candidates")
        void testGetRecommendations_NoCandidates() {
            // Arrange
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(swipeRepository.findSwipedUserIds(userId)).thenReturn(new HashSet<>());
            when(userRepository.findCandidates(any())).thenReturn(List.of());

            // Act
            List<ScoredCandidate> results = recommendationService.getRecommendations(userId);

            // Assert
            assertNotNull(results);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Should sort candidates by score descending")
        void testGetRecommendations_SortsByScoreDescending() {
            // Arrange
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(swipeRepository.findSwipedUserIds(userId)).thenReturn(new HashSet<>());
            when(userRepository.findCandidates(any())).thenReturn(List.of(candidate1, candidate2));

            // Return in reverse order to test sorting
            ScoredCandidate scored1 = createScoredCandidate(candidate1, 0.4);
            ScoredCandidate scored2 = createScoredCandidate(candidate2, 0.9);
            when(scoreAggregator.aggregate(testUser, candidate1)).thenReturn(scored1);
            when(scoreAggregator.aggregate(testUser, candidate2)).thenReturn(scored2);

            // Act
            List<ScoredCandidate> results = recommendationService.getRecommendations(userId);

            // Assert
            assertEquals(2, results.size());
            assertEquals(0.9, results.get(0).finalScore(), 0.001); // Highest first
            assertEquals(0.4, results.get(1).finalScore(), 0.001);
        }
    }

    @Nested
    @DisplayName("getCompatibilityScore tests")
    class GetCompatibilityScoreTests {

        @Test
        @DisplayName("Should return score between two users")
        void testGetCompatibilityScore_Success() {
            // Arrange
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(candidate1Id)).thenReturn(Optional.of(candidate1));

            ScoredCandidate expectedScore = createScoredCandidate(candidate1, 0.75);
            when(scoreAggregator.aggregate(testUser, candidate1)).thenReturn(expectedScore);

            // Act
            ScoredCandidate result = recommendationService.getCompatibilityScore(userId, candidate1Id);

            // Assert
            assertNotNull(result);
            assertEquals(0.75, result.finalScore(), 0.001);
        }

        @Test
        @DisplayName("Should throw exception when first user not found")
        void testGetCompatibilityScore_FirstUserNotFound() {
            // Arrange
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(UserNotFoundException.class, () ->
                    recommendationService.getCompatibilityScore(userId, candidate1Id));
        }

        @Test
        @DisplayName("Should throw exception when second user not found")
        void testGetCompatibilityScore_SecondUserNotFound() {
            // Arrange
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(candidate1Id)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(UserNotFoundException.class, () ->
                    recommendationService.getCompatibilityScore(userId, candidate1Id));
        }
    }

    @Nested
    @DisplayName("getAlgorithmInfo tests")
    class GetAlgorithmInfoTests {

        @Test
        @DisplayName("Should return algorithm configuration")
        void testGetAlgorithmInfo() {
            // Arrange
            Map<String, Double> scorerWeights = Map.of("age", 0.2, "location", 0.3);
            when(scoreAggregator.getScorerWeights()).thenReturn(scorerWeights);
            when(scoreAggregator.getActiveScorerCount()).thenReturn(2);

            // Act
            Map<String, Object> info = recommendationService.getAlgorithmInfo();

            // Assert
            assertNotNull(info);
            assertEquals(20, info.get("batchSize"));
            assertEquals(0.3, info.get("minimumScore"));
            assertEquals(scorerWeights, info.get("scorers"));
            assertEquals(2, info.get("activeScorerCount"));
        }
    }

    // Helper method to create ScoredCandidate
    private ScoredCandidate createScoredCandidate(User candidate, double score) {
        return new ScoredCandidate(
                com.dating.recommendation.dto.CandidateProfileDTO.fromUser(candidate),
                score,
                Map.of("test-scorer", score)
        );
    }
}
