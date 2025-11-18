package com.dating.recommendation.service;

import com.dating.recommendation.client.UserServiceClient;
import com.dating.recommendation.config.CacheConfig;
import com.dating.recommendation.dto.UserProfileDto;
import com.dating.recommendation.dto.response.*;
import com.dating.recommendation.mapper.RecommendationMapper;
import com.dating.recommendation.model.Recommendation;
import com.dating.recommendation.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main service for generating and managing recommendations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final UserServiceClient userServiceClient;
    private final ScoringService scoringService;
    private final PreferenceAnalyzerService preferenceAnalyzerService;
    private final RecommendationMapper recommendationMapper;

    @Value("${recommendation.default-algorithm:v1}")
    private String defaultAlgorithm;

    @Value("${recommendation.cache-ttl-hours:24}")
    private int cacheTtlHours;

    @Value("${recommendation.default-limit:10}")
    private int defaultLimit;

    @Value("${recommendation.max-limit:50}")
    private int maxLimit;

    /**
     * Get recommendations for a user.
     *
     * @param userId User ID
     * @param limit Maximum number of recommendations
     * @param algorithm Algorithm version
     * @return List of recommendations
     */
    @Cacheable(value = CacheConfig.RECOMMENDATIONS_CACHE, key = "#userId + '-' + #limit + '-' + #algorithm")
    public RecommendationListResponse getRecommendations(UUID userId, Integer limit, String algorithm) {
        int effectiveLimit = limit != null ? Math.min(limit, maxLimit) : defaultLimit;
        String effectiveAlgorithm = algorithm != null ? algorithm : defaultAlgorithm;

        log.info("Getting recommendations for user {} with limit {} and algorithm {}",
                userId, effectiveLimit, effectiveAlgorithm);

        // Check for cached recommendations
        List<Recommendation> cachedRecs = recommendationRepository.findActiveRecommendations(userId, Instant.now());

        if (!cachedRecs.isEmpty() && cachedRecs.size() >= effectiveLimit) {
            log.debug("Using cached recommendations for user {}", userId);
            return buildResponseFromCache(cachedRecs, effectiveLimit);
        }

        // Generate new recommendations
        return generateRecommendations(userId, effectiveLimit, effectiveAlgorithm);
    }

    /**
     * Get compatibility score between two users.
     *
     * @param userId Source user ID
     * @param targetUserId Target user ID
     * @return Score response
     */
    @Cacheable(value = CacheConfig.COMPATIBILITY_SCORES_CACHE, key = "#userId + '-' + #targetUserId")
    public ScoreResponse getCompatibilityScore(UUID userId, UUID targetUserId) {
        log.info("Calculating compatibility score between {} and {}", userId, targetUserId);

        // Get user profiles
        UserProfileDto sourceUser = userServiceClient.getUserById(userId);
        UserProfileDto targetUser = userServiceClient.getUserById(targetUserId);

        // Enrich with activity stats
        sourceUser = preferenceAnalyzerService.enrichWithActivityStats(sourceUser);
        targetUser = preferenceAnalyzerService.enrichWithActivityStats(targetUser);

        // Calculate score
        ScoreFactors factors = scoringService.calculateFactors(sourceUser, targetUser, defaultAlgorithm);
        int score = (int) Math.round(factors.getTotalScore());

        // Build response
        Map<String, Integer> factorMap = new HashMap<>();
        factorMap.put("interestMatch", (int) factors.getPreferenceMatch());
        factorMap.put("ageCompatibility", (int) factors.getProfileCompleteness());
        factorMap.put("preferenceAlignment", (int) factors.getActivity());

        return ScoreResponse.builder()
                .score(score)
                .factors(factorMap)
                .calculatedAt(Instant.now())
                .build();
    }

    /**
     * Score profiles for match-service (internal API).
     *
     * @param userId Source user ID
     * @param candidateIds Candidate user IDs
     * @param algorithm Algorithm version
     * @return Batch score response
     */
    public BatchScoreResponse scoreProfiles(UUID userId, List<UUID> candidateIds, String algorithm) {
        String effectiveAlgorithm = algorithm != null ? algorithm : defaultAlgorithm;

        log.info("Scoring {} candidates for user {} using algorithm {}",
                candidateIds.size(), userId, effectiveAlgorithm);

        // Get source user profile
        UserProfileDto sourceUser = userServiceClient.getUserById(userId);
        sourceUser = preferenceAnalyzerService.enrichWithActivityStats(sourceUser);

        // Get candidate profiles
        List<UserProfileDto> candidates = userServiceClient.getUsersByIds(candidateIds);
        candidates = candidates.stream()
                .map(preferenceAnalyzerService::enrichWithActivityStats)
                .collect(Collectors.toList());

        return scoringService.scoreMultiple(sourceUser, candidates, effectiveAlgorithm);
    }

    /**
     * Generate new recommendations for a user.
     */
    @Transactional
    public RecommendationListResponse generateRecommendations(UUID userId, int limit, String algorithm) {
        log.info("Generating new recommendations for user {}", userId);

        // Get source user profile
        UserProfileDto sourceUser = userServiceClient.getUserById(userId);
        sourceUser = preferenceAnalyzerService.enrichWithActivityStats(sourceUser);

        // Get eligible candidates
        List<UserProfileDto> candidates = userServiceClient.getCandidates(userId, limit * 3);

        // Score and sort candidates
        List<ScoredCandidate> scoredCandidates = candidates.stream()
                .map(candidate -> {
                    UserProfileDto enrichedCandidate = preferenceAnalyzerService.enrichWithActivityStats(candidate);
                    ScoreFactors factors = scoringService.calculateFactors(sourceUser, enrichedCandidate, algorithm);
                    int score = (int) Math.round(factors.getTotalScore());
                    String reason = scoringService.generateReason(factors, sourceUser, enrichedCandidate);
                    return new ScoredCandidate(enrichedCandidate, score, factors, reason);
                })
                .sorted((a, b) -> Integer.compare(b.score, a.score))
                .limit(limit)
                .toList();

        // Save recommendations to database
        Instant expiresAt = Instant.now().plus(cacheTtlHours, ChronoUnit.HOURS);

        // Clear old recommendations first
        recommendationRepository.deleteByUserId(userId);

        List<Recommendation> savedRecs = scoredCandidates.stream()
                .map(scored -> {
                    Recommendation rec = Recommendation.builder()
                            .userId(userId)
                            .recommendedUserId(scored.profile.getId())
                            .score(BigDecimal.valueOf(scored.score))
                            .algorithmVersion(algorithm)
                            .factors(convertFactorsToMap(scored.factors))
                            .expiresAt(expiresAt)
                            .build();
                    return recommendationRepository.save(rec);
                })
                .toList();

        // Build response
        List<RecommendationResponse> recommendations = new ArrayList<>();
        for (int i = 0; i < savedRecs.size(); i++) {
            ScoredCandidate scored = scoredCandidates.get(i);
            Recommendation rec = savedRecs.get(i);

            RecommendationResponse response = recommendationMapper.toResponse(
                    rec, scored.profile, scored.factors, scored.reason);
            recommendations.add(response);
        }

        return RecommendationListResponse.builder()
                .recommendations(recommendations)
                .total(recommendations.size())
                .hasMore(candidates.size() > limit)
                .generatedAt(Instant.now())
                .build();
    }

    /**
     * Refresh recommendations for a user (invalidate cache).
     */
    @CacheEvict(value = CacheConfig.RECOMMENDATIONS_CACHE, allEntries = true)
    @Transactional
    public void refreshRecommendations(UUID userId) {
        log.info("Refreshing recommendations for user {}", userId);
        recommendationRepository.deleteByUserId(userId);
    }

    /**
     * Clean up expired recommendations.
     */
    @Transactional
    public int cleanupExpiredRecommendations() {
        int deleted = recommendationRepository.deleteExpiredRecommendations(Instant.now());
        log.info("Cleaned up {} expired recommendations", deleted);
        return deleted;
    }

    /**
     * Build response from cached recommendations.
     */
    private RecommendationListResponse buildResponseFromCache(List<Recommendation> cachedRecs, int limit) {
        List<UUID> userIds = cachedRecs.stream()
                .limit(limit)
                .map(Recommendation::getRecommendedUserId)
                .toList();

        List<UserProfileDto> users = userServiceClient.getUsersByIds(userIds);
        Map<UUID, UserProfileDto> userMap = users.stream()
                .collect(Collectors.toMap(UserProfileDto::getId, u -> u));

        List<RecommendationResponse> recommendations = cachedRecs.stream()
                .limit(limit)
                .map(rec -> {
                    UserProfileDto user = userMap.get(rec.getRecommendedUserId());
                    ScoreFactors factors = convertMapToFactors(rec.getFactors());
                    String reason = user != null ?
                            scoringService.generateReason(factors, null, user) :
                            "Recommended based on your preferences";
                    return recommendationMapper.toResponse(rec, user, factors, reason);
                })
                .toList();

        return RecommendationListResponse.builder()
                .recommendations(recommendations)
                .total(cachedRecs.size())
                .hasMore(cachedRecs.size() > limit)
                .generatedAt(cachedRecs.get(0).getCreatedAt())
                .build();
    }

    /**
     * Convert ScoreFactors to Map for storage.
     */
    private Map<String, Object> convertFactorsToMap(ScoreFactors factors) {
        Map<String, Object> map = new HashMap<>();
        map.put("profileCompleteness", factors.getProfileCompleteness());
        map.put("preferenceMatch", factors.getPreferenceMatch());
        map.put("activity", factors.getActivity());
        map.put("mlPrediction", factors.getMlPrediction());
        map.put("interestMatch", factors.getInterestMatch());
        map.put("ageCompatibility", factors.getAgeCompatibility());
        map.put("preferenceAlignment", factors.getPreferenceAlignment());
        return map;
    }

    /**
     * Convert Map back to ScoreFactors.
     */
    private ScoreFactors convertMapToFactors(Map<String, Object> map) {
        if (map == null) {
            return ScoreFactors.builder().build();
        }

        return ScoreFactors.builder()
                .profileCompleteness(getDouble(map, "profileCompleteness"))
                .preferenceMatch(getDouble(map, "preferenceMatch"))
                .activity(getDouble(map, "activity"))
                .mlPrediction(getDouble(map, "mlPrediction"))
                .interestMatch((String) map.get("interestMatch"))
                .ageCompatibility((String) map.get("ageCompatibility"))
                .preferenceAlignment((String) map.get("preferenceAlignment"))
                .build();
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * Internal class for scored candidate.
     */
    private record ScoredCandidate(UserProfileDto profile, int score, ScoreFactors factors, String reason) {}
}
