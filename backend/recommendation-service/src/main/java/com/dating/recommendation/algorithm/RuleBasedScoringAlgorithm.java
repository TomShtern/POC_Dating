package com.dating.recommendation.algorithm;

import com.dating.recommendation.dto.UserProfileDto;
import com.dating.recommendation.dto.response.ScoreFactors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Rule-based scoring algorithm (v1).
 * Scores users based on profile completeness, preference match, and activity.
 */
@Component
@Slf4j
public class RuleBasedScoringAlgorithm implements ScoringAlgorithm {

    private static final String VERSION = "v1";
    private static final String DESCRIPTION = "Rule-based scoring with preference matching";

    @Override
    public int calculateScore(UserProfileDto sourceUser, UserProfileDto targetUser) {
        ScoreFactors factors = calculateFactors(sourceUser, targetUser);
        return (int) Math.round(factors.getTotalScore());
    }

    @Override
    public ScoreFactors calculateFactors(UserProfileDto sourceUser, UserProfileDto targetUser) {
        double profileScore = calculateProfileCompleteness(targetUser);
        double preferenceScore = calculatePreferenceMatch(sourceUser, targetUser);
        double activityScore = calculateActivityScore(targetUser);
        double mlScore = calculateMLPrediction(sourceUser, targetUser);

        // Calculate descriptive factors
        String interestMatch = calculateInterestMatchDescription(sourceUser, targetUser);
        String ageCompatibility = calculateAgeCompatibilityDescription(sourceUser, targetUser);
        String preferenceAlignment = calculatePreferenceAlignmentDescription(preferenceScore);

        return ScoreFactors.builder()
                .profileCompleteness(profileScore)
                .preferenceMatch(preferenceScore)
                .activity(activityScore)
                .mlPrediction(mlScore)
                .interestMatch(interestMatch)
                .ageCompatibility(ageCompatibility)
                .preferenceAlignment(preferenceAlignment)
                .build();
    }

    /**
     * Calculate profile completeness score (0-10 points).
     * Based on bio, photos, interests, and verification.
     */
    private double calculateProfileCompleteness(UserProfileDto user) {
        double score = 0;

        // Bio (2.5 points if > 50 chars)
        if (user.getBio() != null && user.getBio().length() > 50) {
            score += 2.5;
        }

        // Photos (2.5 points if >= 3 photos)
        if (user.getPhotoCount() >= 3) {
            score += 2.5;
        }

        // Interests (2.5 points if >= 5 interests)
        if (user.getInterests() != null && user.getInterests().size() >= 5) {
            score += 2.5;
        }

        // Verification (2.5 points)
        if (user.isVerified()) {
            score += 2.5;
        }

        return score;
    }

    /**
     * Calculate preference match score (0-40 points).
     * Based on age compatibility, distance, and shared interests.
     */
    private double calculatePreferenceMatch(UserProfileDto sourceUser, UserProfileDto targetUser) {
        double score = 0;

        // Age compatibility (13.3 points)
        if (isAgeInRange(sourceUser, targetUser)) {
            score += 13.3;
        }

        // Distance compatibility (13.3 points)
        if (isDistanceInRange(sourceUser, targetUser)) {
            score += 13.3;
        }

        // Shared interests (up to 13.4 points)
        double interestScore = calculateInterestOverlap(sourceUser, targetUser) * 13.4;
        score += interestScore;

        return score;
    }

    /**
     * Calculate activity score (0-20 points).
     * Based on recent login, swipe frequency, and response rate.
     */
    private double calculateActivityScore(UserProfileDto user) {
        double score = 0;

        // Recent login (6.6 points)
        if (user.getLastLogin() != null) {
            long daysSinceLogin = Duration.between(user.getLastLogin(), Instant.now()).toDays();
            if (daysSinceLogin <= 7) {
                score += 6.6;
            } else if (daysSinceLogin <= 30) {
                score += 3.3;
            }
        }

        // Swipe activity (6.6 points)
        if (user.getSwipeCount() >= 10) {
            score += 6.6;
        } else if (user.getSwipeCount() >= 5) {
            score += 3.3;
        }

        // Response rate (6.8 points)
        double responseRate = calculateResponseRate(user);
        score += responseRate * 6.8;

        return score;
    }

    /**
     * Calculate ML prediction score (0-30 points).
     * Currently a stub returning neutral score.
     */
    private double calculateMLPrediction(UserProfileDto sourceUser, UserProfileDto targetUser) {
        // Stub: Return neutral score (middle of 0-30 range)
        return 15.0;
    }

    /**
     * Check if target user's age is within source user's preference range.
     */
    private boolean isAgeInRange(UserProfileDto sourceUser, UserProfileDto targetUser) {
        int targetAge = targetUser.getAge();
        return targetAge >= sourceUser.getMinAge() && targetAge <= sourceUser.getMaxAge();
    }

    /**
     * Check if target user is within distance preference.
     * Uses simple distance calculation (not actual geo distance for POC).
     */
    private boolean isDistanceInRange(UserProfileDto sourceUser, UserProfileDto targetUser) {
        if (sourceUser.getLatitude() == null || sourceUser.getLongitude() == null ||
            targetUser.getLatitude() == null || targetUser.getLongitude() == null) {
            // If no location data, assume in range
            return true;
        }

        double distance = calculateDistanceKm(
                sourceUser.getLatitude(), sourceUser.getLongitude(),
                targetUser.getLatitude(), targetUser.getLongitude());

        return distance <= sourceUser.getMaxDistanceKm();
    }

    /**
     * Calculate interest overlap ratio (0.0 - 1.0).
     */
    private double calculateInterestOverlap(UserProfileDto sourceUser, UserProfileDto targetUser) {
        List<String> sourceInterests = sourceUser.getInterests();
        List<String> targetInterests = targetUser.getInterests();

        if (sourceInterests == null || sourceInterests.isEmpty() ||
            targetInterests == null || targetInterests.isEmpty()) {
            return 0;
        }

        Set<String> sourceSet = new HashSet<>(sourceInterests);
        Set<String> targetSet = new HashSet<>(targetInterests);

        Set<String> intersection = new HashSet<>(sourceSet);
        intersection.retainAll(targetSet);

        int totalUnique = sourceSet.size() + targetSet.size() - intersection.size();
        if (totalUnique == 0) {
            return 0;
        }

        return (double) intersection.size() / totalUnique;
    }

    /**
     * Calculate response rate (0.0 - 1.0).
     */
    private double calculateResponseRate(UserProfileDto user) {
        if (user.getMessagesReceived() == 0) {
            return 0.5; // Neutral for new users
        }
        return Math.min(1.0, (double) user.getMessagesSent() / user.getMessagesReceived());
    }

    /**
     * Calculate distance between two coordinates (Haversine formula simplified).
     */
    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    /**
     * Generate human-readable interest match description.
     */
    private String calculateInterestMatchDescription(UserProfileDto sourceUser, UserProfileDto targetUser) {
        List<String> sourceInterests = sourceUser.getInterests();
        List<String> targetInterests = targetUser.getInterests();

        if (sourceInterests == null || targetInterests == null) {
            return "N/A";
        }

        Set<String> shared = new HashSet<>(sourceInterests);
        shared.retainAll(targetInterests);

        int maxPossible = Math.max(sourceInterests.size(), targetInterests.size());
        if (maxPossible == 0) {
            return "N/A";
        }

        return shared.size() + "/" + Math.min(10, maxPossible);
    }

    /**
     * Generate human-readable age compatibility description.
     */
    private String calculateAgeCompatibilityDescription(UserProfileDto sourceUser, UserProfileDto targetUser) {
        int targetAge = targetUser.getAge();
        int preferredMin = sourceUser.getMinAge();
        int preferredMax = sourceUser.getMaxAge();

        if (targetAge < preferredMin || targetAge > preferredMax) {
            return "Outside range";
        }

        int middle = (preferredMin + preferredMax) / 2;
        int deviation = Math.abs(targetAge - middle);
        int range = (preferredMax - preferredMin) / 2;

        if (range == 0 || deviation <= range * 0.3) {
            return "Perfect";
        } else if (deviation <= range * 0.6) {
            return "Good";
        } else {
            return "Acceptable";
        }
    }

    /**
     * Generate human-readable preference alignment description.
     */
    private String calculatePreferenceAlignmentDescription(double preferenceScore) {
        if (preferenceScore >= 35) {
            return "High";
        } else if (preferenceScore >= 25) {
            return "Medium";
        } else {
            return "Low";
        }
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
