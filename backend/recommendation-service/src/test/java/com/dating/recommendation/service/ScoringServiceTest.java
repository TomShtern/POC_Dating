package com.dating.recommendation.service;

import com.dating.recommendation.algorithm.CollaborativeFilteringAlgorithm;
import com.dating.recommendation.algorithm.RuleBasedScoringAlgorithm;
import com.dating.recommendation.dto.UserProfileDto;
import com.dating.recommendation.dto.response.BatchScoreResponse;
import com.dating.recommendation.dto.response.ScoreFactors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    @Mock
    private RuleBasedScoringAlgorithm ruleBasedAlgorithm;

    @Mock
    private CollaborativeFilteringAlgorithm collaborativeAlgorithm;

    @InjectMocks
    private ScoringService scoringService;

    private UserProfileDto sourceUser;
    private UserProfileDto targetUser;

    @BeforeEach
    void setUp() {
        sourceUser = UserProfileDto.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("MALE")
                .bio("Test bio for John")
                .lastLogin(Instant.now())
                .minAge(25)
                .maxAge(35)
                .maxDistanceKm(50)
                .interestedIn("FEMALE")
                .interests(Arrays.asList("hiking", "movies", "travel"))
                .photoCount(3)
                .swipeCount(10)
                .messagesSent(5)
                .messagesReceived(10)
                .build();

        targetUser = UserProfileDto.builder()
                .id(UUID.randomUUID())
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.of(1992, 5, 15))
                .gender("FEMALE")
                .bio("Test bio for Jane that is long enough")
                .lastLogin(Instant.now())
                .minAge(28)
                .maxAge(40)
                .maxDistanceKm(30)
                .interestedIn("MALE")
                .interests(Arrays.asList("hiking", "cooking", "travel"))
                .photoCount(5)
                .swipeCount(15)
                .messagesSent(8)
                .messagesReceived(8)
                .verified(true)
                .build();
    }

    @Test
    void testCalculateScore_WithV1Algorithm() {
        // Arrange
        when(ruleBasedAlgorithm.calculateScore(any(), any())).thenReturn(75);

        // Act
        int score = scoringService.calculateScore(sourceUser, targetUser, "v1");

        // Assert
        assertEquals(75, score);
    }

    @Test
    void testCalculateScore_WithDefaultAlgorithm() {
        // Arrange
        when(ruleBasedAlgorithm.calculateScore(any(), any())).thenReturn(80);

        // Act
        int score = scoringService.calculateScore(sourceUser, targetUser, null);

        // Assert
        assertEquals(80, score);
    }

    @Test
    void testCalculateScore_WithV2Algorithm() {
        // Arrange
        when(collaborativeAlgorithm.calculateScore(any(), any())).thenReturn(50);

        // Act
        int score = scoringService.calculateScore(sourceUser, targetUser, "v2");

        // Assert
        assertEquals(50, score);
    }

    @Test
    void testCalculateFactors_ReturnsFactors() {
        // Arrange
        ScoreFactors expectedFactors = ScoreFactors.builder()
                .profileCompleteness(10.0)
                .preferenceMatch(35.0)
                .activity(15.0)
                .mlPrediction(15.0)
                .interestMatch("2/3")
                .ageCompatibility("Perfect")
                .preferenceAlignment("High")
                .build();

        when(ruleBasedAlgorithm.calculateFactors(any(), any())).thenReturn(expectedFactors);

        // Act
        ScoreFactors factors = scoringService.calculateFactors(sourceUser, targetUser, "v1");

        // Assert
        assertNotNull(factors);
        assertEquals(10.0, factors.getProfileCompleteness());
        assertEquals(35.0, factors.getPreferenceMatch());
        assertEquals("Perfect", factors.getAgeCompatibility());
    }

    @Test
    void testScoreMultiple_ScoresAllCandidates() {
        // Arrange
        UserProfileDto candidate1 = UserProfileDto.builder()
                .id(UUID.randomUUID())
                .firstName("Candidate1")
                .build();

        UserProfileDto candidate2 = UserProfileDto.builder()
                .id(UUID.randomUUID())
                .firstName("Candidate2")
                .build();

        List<UserProfileDto> candidates = Arrays.asList(candidate1, candidate2);

        when(ruleBasedAlgorithm.calculateScore(any(), any())).thenReturn(70, 85);
        when(ruleBasedAlgorithm.calculateFactors(any(), any())).thenReturn(
                ScoreFactors.builder().build(),
                ScoreFactors.builder().build()
        );

        // Act
        BatchScoreResponse response = scoringService.scoreMultiple(sourceUser, candidates, "v1");

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getScores().size());
        assertEquals(2, response.getFactors().size());
        assertTrue(response.getScores().containsKey(candidate1.getId()));
        assertTrue(response.getScores().containsKey(candidate2.getId()));
    }

    @Test
    void testGenerateReason_WithSharedInterests() {
        // Arrange
        ScoreFactors factors = ScoreFactors.builder()
                .interestMatch("2/3")
                .ageCompatibility("Good")
                .preferenceAlignment("Medium")
                .build();

        // Act
        String reason = scoringService.generateReason(factors, sourceUser, targetUser);

        // Assert
        assertNotNull(reason);
        assertTrue(reason.contains("interests"));
    }

    @Test
    void testGenerateReason_WithPerfectAge() {
        // Arrange
        ScoreFactors factors = ScoreFactors.builder()
                .interestMatch("0/3")
                .ageCompatibility("Perfect")
                .preferenceAlignment("Medium")
                .build();

        // Source user without interests
        UserProfileDto noInterestUser = UserProfileDto.builder()
                .id(UUID.randomUUID())
                .interests(Arrays.asList())
                .build();

        // Act
        String reason = scoringService.generateReason(factors, noInterestUser, targetUser);

        // Assert
        assertNotNull(reason);
        assertEquals("Perfect age match", reason);
    }
}
