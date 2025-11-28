package com.dating.recommendation.algorithm;

import com.dating.recommendation.dto.UserProfileDto;
import com.dating.recommendation.dto.response.ScoreFactors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedScoringAlgorithmTest {

    private RuleBasedScoringAlgorithm algorithm;
    private UserProfileDto sourceUser;
    private UserProfileDto targetUser;

    @BeforeEach
    void setUp() {
        algorithm = new RuleBasedScoringAlgorithm();

        sourceUser = UserProfileDto.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("MALE")
                .bio("A tech enthusiast who loves hiking and photography")
                .lastLogin(Instant.now())
                .verified(false)
                .latitude(40.7128)
                .longitude(-74.0060)
                .minAge(25)
                .maxAge(35)
                .maxDistanceKm(50)
                .interestedIn("FEMALE")
                .interests(Arrays.asList("hiking", "movies", "travel", "photography", "cooking"))
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
                .bio("Love exploring new cuisines and outdoor adventures! Always looking for my next travel destination.")
                .lastLogin(Instant.now())
                .verified(true)
                .latitude(40.7580)
                .longitude(-73.9855)
                .minAge(28)
                .maxAge(40)
                .maxDistanceKm(30)
                .interestedIn("MALE")
                .interests(Arrays.asList("hiking", "cooking", "travel", "yoga", "reading"))
                .photoCount(5)
                .swipeCount(15)
                .messagesSent(8)
                .messagesReceived(8)
                .build();
    }

    @Test
    void testGetVersion() {
        assertEquals("v1", algorithm.getVersion());
    }

    @Test
    void testGetDescription() {
        assertNotNull(algorithm.getDescription());
        assertTrue(algorithm.getDescription().contains("Rule-based"));
    }

    @Test
    void testCalculateScore_HighCompatibility() {
        // Both users have good profiles, matching preferences
        int score = algorithm.calculateScore(sourceUser, targetUser);

        // Should be high score (60-100) due to good compatibility
        assertTrue(score >= 50 && score <= 100,
                "Score should be between 50 and 100 for compatible users, but was: " + score);
    }

    @Test
    void testCalculateScore_LowCompatibility() {
        // Make target incompatible
        targetUser.setDateOfBirth(LocalDate.of(2000, 1, 1)); // Too young
        targetUser.setInterests(Arrays.asList("gaming", "sports")); // No common interests

        int score = algorithm.calculateScore(sourceUser, targetUser);

        // Should be lower score due to incompatibility
        assertTrue(score < 70, "Score should be below 70 for incompatible users, but was: " + score);
    }

    @Test
    void testCalculateFactors_ProfileCompleteness() {
        ScoreFactors factors = algorithm.calculateFactors(sourceUser, targetUser);

        // Target has: bio > 50 chars (2.5), photos >= 3 (2.5), interests >= 5 (2.5), verified (2.5) = 10
        assertEquals(10.0, factors.getProfileCompleteness(), 0.01);
    }

    @Test
    void testCalculateFactors_IncompleteProfile() {
        // Create incomplete profile
        targetUser.setBio("Short");
        targetUser.setPhotoCount(1);
        targetUser.setInterests(Arrays.asList("hiking"));
        targetUser.setVerified(false);

        ScoreFactors factors = algorithm.calculateFactors(sourceUser, targetUser);

        // Should have low profile completeness
        assertTrue(factors.getProfileCompleteness() < 5,
                "Profile completeness should be below 5 for incomplete profile");
    }

    @Test
    void testCalculateFactors_AgeMatch() {
        // Target is 32 (born 1992), source prefers 25-35
        ScoreFactors factors = algorithm.calculateFactors(sourceUser, targetUser);

        // Age should match and get points
        assertTrue(factors.getPreferenceMatch() >= 13,
                "Should get age match points");
        assertEquals("Perfect", factors.getAgeCompatibility());
    }

    @Test
    void testCalculateFactors_AgeOutOfRange() {
        // Make target too young
        targetUser.setDateOfBirth(LocalDate.of(2005, 1, 1)); // Age 19

        ScoreFactors factors = algorithm.calculateFactors(sourceUser, targetUser);

        // Should not get full preference match
        assertEquals("Outside range", factors.getAgeCompatibility());
    }

    @Test
    void testCalculateFactors_SharedInterests() {
        // Source: hiking, movies, travel, photography, cooking
        // Target: hiking, cooking, travel, yoga, reading
        // Shared: hiking, cooking, travel (3 out of 7 unique)

        ScoreFactors factors = algorithm.calculateFactors(sourceUser, targetUser);

        // Should show shared interests
        assertNotNull(factors.getInterestMatch());
        assertTrue(factors.getPreferenceMatch() > 0);
    }

    @Test
    void testCalculateFactors_Activity_RecentLogin() {
        // Target logged in recently
        targetUser.setLastLogin(Instant.now().minus(1, ChronoUnit.DAYS));

        ScoreFactors factors = algorithm.calculateFactors(sourceUser, targetUser);

        // Should get full activity points for recent login
        assertTrue(factors.getActivity() >= 6,
                "Should get activity points for recent login");
    }

    @Test
    void testCalculateFactors_Activity_OldLogin() {
        // Target logged in 2 weeks ago
        targetUser.setLastLogin(Instant.now().minus(14, ChronoUnit.DAYS));

        ScoreFactors factors = algorithm.calculateFactors(sourceUser, targetUser);

        // Should get partial activity points
        assertTrue(factors.getActivity() >= 3,
                "Should get partial activity points for older login");
    }

    @Test
    void testCalculateFactors_Activity_VeryOldLogin() {
        // Target logged in 2 months ago
        targetUser.setLastLogin(Instant.now().minus(60, ChronoUnit.DAYS));

        ScoreFactors factors = algorithm.calculateFactors(sourceUser, targetUser);

        // Should get minimal login points
        assertTrue(factors.getActivity() < 10,
                "Should get minimal activity points for very old login");
    }

    @Test
    void testCalculateFactors_MLPrediction_Stub() {
        ScoreFactors factors = algorithm.calculateFactors(sourceUser, targetUser);

        // ML prediction is currently stubbed at 15.0
        assertEquals(15.0, factors.getMlPrediction(), 0.01);
    }

    @Test
    void testCalculateFactors_PreferenceAlignment_High() {
        ScoreFactors factors = algorithm.calculateFactors(sourceUser, targetUser);

        // With good compatibility, should have High alignment
        assertTrue(Arrays.asList("High", "Medium").contains(factors.getPreferenceAlignment()));
    }

    @Test
    void testCalculateScore_NoLocation() {
        // Remove location data
        sourceUser.setLatitude(null);
        sourceUser.setLongitude(null);

        int score = algorithm.calculateScore(sourceUser, targetUser);

        // Should still calculate score (assumes in range when no location)
        assertTrue(score > 0);
    }

    @Test
    void testCalculateScore_NoInterests() {
        // Remove interests
        sourceUser.setInterests(null);
        targetUser.setInterests(null);

        int score = algorithm.calculateScore(sourceUser, targetUser);

        // Should still calculate score
        assertTrue(score > 0);
    }

    @Test
    void testCalculateFactors_ResponseRate() {
        // Target has 8 sent, 8 received = 100% response rate
        ScoreFactors factors = algorithm.calculateFactors(sourceUser, targetUser);

        // Should contribute to activity score
        assertTrue(factors.getActivity() >= 0);
    }

    @Test
    void testCalculateScore_WithDistance() {
        // Source: 40.7128, -74.0060 (NYC)
        // Target: 40.7580, -73.9855 (also NYC, ~5km away)
        // Source max distance: 50km

        int score = algorithm.calculateScore(sourceUser, targetUser);

        // Should be in range and get distance points
        assertTrue(score > 40);
    }

    @Test
    void testCalculateScore_OutOfDistance() {
        // Move target far away
        targetUser.setLatitude(34.0522);  // Los Angeles
        targetUser.setLongitude(-118.2437);

        int score = algorithm.calculateScore(sourceUser, targetUser);

        // Should lose distance points
        assertTrue(score < 80);
    }
}
