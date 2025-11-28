package com.dating.recommendation.health;

import com.dating.recommendation.service.ScoreAggregator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for the Recommendation Service.
 * Checks service-specific components beyond basic database connectivity.
 */
@Component
@RequiredArgsConstructor
public class RecommendationServiceHealthIndicator implements HealthIndicator {

    private final ScoreAggregator scoreAggregator;

    @Override
    public Health health() {
        try {
            // Check that scorers are properly configured
            int activeScorerCount = scoreAggregator.getActiveScorerCount();

            if (activeScorerCount == 0) {
                return Health.down()
                    .withDetail("error", "No active scorers configured")
                    .withDetail("activeScorerCount", activeScorerCount)
                    .build();
            }

            return Health.up()
                .withDetail("activeScorerCount", activeScorerCount)
                .withDetail("scorerWeights", scoreAggregator.getScorerWeights())
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
