package com.dating.recommendation.service;

import com.dating.recommendation.dto.UserProfileDto;
import com.dating.recommendation.model.InteractionHistory;
import com.dating.recommendation.repository.InteractionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for analyzing user preferences and behavior.
 * Used to improve recommendation accuracy over time.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PreferenceAnalyzerService {

    private final InteractionHistoryRepository interactionHistoryRepository;

    /**
     * Analyze user's interaction patterns.
     *
     * @param userId User ID
     * @return Map of analysis results
     */
    public Map<String, Object> analyzeUserBehavior(UUID userId) {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        Map<String, Object> analysis = new HashMap<>();

        // Count different action types
        long swipes = interactionHistoryRepository.countSwipesSince(userId, thirtyDaysAgo);
        long totalInteractions = interactionHistoryRepository.countTotalInteractionsSince(userId, thirtyDaysAgo);

        analysis.put("swipeCount", swipes);
        analysis.put("totalInteractions", totalInteractions);
        analysis.put("activityLevel", calculateActivityLevel(totalInteractions));

        // Response rate
        long messagesSent = interactionHistoryRepository.countMessagesSent(userId);
        long messagesReceived = interactionHistoryRepository.countMessagesReceived(userId);
        double responseRate = messagesReceived > 0 ? (double) messagesSent / messagesReceived : 0.5;
        analysis.put("responseRate", responseRate);

        log.debug("Analyzed behavior for user {}: {} swipes, {} total interactions",
                userId, swipes, totalInteractions);

        return analysis;
    }

    /**
     * Record user interaction for analysis.
     *
     * @param userId User ID
     * @param action Action type
     * @param targetId Target user ID (optional)
     * @param metadata Additional metadata
     */
    @Transactional
    public void recordInteraction(UUID userId, String action, UUID targetId, Map<String, Object> metadata) {
        InteractionHistory history = InteractionHistory.builder()
                .userId(userId)
                .action(action)
                .targetId(targetId)
                .metadata(metadata)
                .build();

        interactionHistoryRepository.save(history);

        log.debug("Recorded interaction: user {} performed {} on {}",
                userId, action, targetId);
    }

    /**
     * Get user activity statistics for scoring.
     *
     * @param userId User ID
     * @return Updated profile with activity stats
     */
    public UserProfileDto enrichWithActivityStats(UserProfileDto profile) {
        if (profile == null || profile.getId() == null) {
            return profile;
        }

        UUID userId = profile.getId();
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        long swipeCount = interactionHistoryRepository.countSwipesSince(userId, thirtyDaysAgo);
        long messagesSent = interactionHistoryRepository.countMessagesSent(userId);
        long messagesReceived = interactionHistoryRepository.countMessagesReceived(userId);

        profile.setSwipeCount(swipeCount);
        profile.setMessagesSent(messagesSent);
        profile.setMessagesReceived(messagesReceived);

        return profile;
    }

    /**
     * Determine activity level category.
     */
    private String calculateActivityLevel(long totalInteractions) {
        if (totalInteractions >= 100) {
            return "HIGH";
        } else if (totalInteractions >= 30) {
            return "MEDIUM";
        } else if (totalInteractions >= 10) {
            return "LOW";
        } else {
            return "NEW";
        }
    }

    /**
     * Check if a user is active (logged in within 7 days).
     *
     * @param profile User profile
     * @return True if user is active
     */
    public boolean isUserActive(UserProfileDto profile) {
        if (profile.getLastLogin() == null) {
            return false;
        }

        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        return profile.getLastLogin().isAfter(sevenDaysAgo);
    }

    /**
     * Get interaction history for a user.
     *
     * @param userId User ID
     * @return List of interactions
     */
    public List<InteractionHistory> getInteractionHistory(UUID userId) {
        return interactionHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
