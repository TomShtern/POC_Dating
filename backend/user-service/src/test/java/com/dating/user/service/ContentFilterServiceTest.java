package com.dating.user.service;

import com.dating.user.dto.ContentAnalysisResult;
import com.dating.user.filter.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentFilterServiceTest {

    @Mock
    private ProfanityFilter profanityFilter;

    @Mock
    private SpamDetectionFilter spamDetectionFilter;

    @Mock
    private ScamPatternFilter scamPatternFilter;

    @Mock
    private HarassmentFilter harassmentFilter;

    @InjectMocks
    private ContentFilterServiceImpl contentFilterService;

    @BeforeEach
    void setUp() {
        // Reset filters before each test
        reset(profanityFilter, spamDetectionFilter, scamPatternFilter, harassmentFilter);
    }

    @Test
    void testAnalyzeText_Clean() {
        // Arrange
        String cleanText = "Hello! I enjoy hiking and photography. Looking forward to meeting new people.";

        // Mock all filters returning clean results
        when(profanityFilter.analyze(cleanText)).thenReturn(
                FilterResult.builder()
                        .flagged(false)
                        .score(0.0)
                        .filterName("ProfanityFilter")
                        .build()
        );
        when(spamDetectionFilter.analyze(cleanText)).thenReturn(
                FilterResult.builder()
                        .flagged(false)
                        .score(0.0)
                        .filterName("SpamDetectionFilter")
                        .build()
        );
        when(scamPatternFilter.analyze(cleanText)).thenReturn(
                FilterResult.builder()
                        .flagged(false)
                        .score(0.0)
                        .filterName("ScamPatternFilter")
                        .build()
        );
        when(harassmentFilter.analyze(cleanText)).thenReturn(
                FilterResult.builder()
                        .flagged(false)
                        .score(0.0)
                        .filterName("HarassmentFilter")
                        .build()
        );

        // Act
        ContentAnalysisResult result = contentFilterService.analyzeText(cleanText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isFalse();
        assertThat(result.getOverallScore()).isEqualTo(0.0);
        assertThat(result.getFilterResults()).hasSize(4);
        assertThat(result.getFlaggedCategories()).isEmpty();

        verify(profanityFilter, times(1)).analyze(cleanText);
        verify(spamDetectionFilter, times(1)).analyze(cleanText);
        verify(scamPatternFilter, times(1)).analyze(cleanText);
        verify(harassmentFilter, times(1)).analyze(cleanText);
    }

    @Test
    void testAnalyzeText_Profanity() {
        // Arrange
        String profaneText = "This contains bad words and offensive language";

        when(profanityFilter.analyze(profaneText)).thenReturn(
                FilterResult.builder()
                        .flagged(true)
                        .score(0.85)
                        .filterName("ProfanityFilter")
                        .reasons(Arrays.asList("Contains profanity"))
                        .build()
        );
        when(spamDetectionFilter.analyze(profaneText)).thenReturn(
                FilterResult.builder()
                        .flagged(false)
                        .score(0.1)
                        .filterName("SpamDetectionFilter")
                        .build()
        );
        when(scamPatternFilter.analyze(profaneText)).thenReturn(
                FilterResult.builder()
                        .flagged(false)
                        .score(0.0)
                        .filterName("ScamPatternFilter")
                        .build()
        );
        when(harassmentFilter.analyze(profaneText)).thenReturn(
                FilterResult.builder()
                        .flagged(false)
                        .score(0.2)
                        .filterName("HarassmentFilter")
                        .build()
        );

        // Act
        ContentAnalysisResult result = contentFilterService.analyzeText(profaneText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getOverallScore()).isGreaterThan(0.5);
        assertThat(result.getFlaggedCategories()).contains("ProfanityFilter");
        assertThat(result.getFilterResults()).hasSize(4);

        verify(profanityFilter, times(1)).analyze(profaneText);
    }

    @Test
    void testAnalyzeText_Spam() {
        // Arrange
        String spamText = "CLICK HERE NOW!!! WWW.SCAM.COM FREE MONEY $$$$$";

        when(profanityFilter.analyze(spamText)).thenReturn(
                FilterResult.builder()
                        .flagged(false)
                        .score(0.1)
                        .filterName("ProfanityFilter")
                        .build()
        );
        when(spamDetectionFilter.analyze(spamText)).thenReturn(
                FilterResult.builder()
                        .flagged(true)
                        .score(0.95)
                        .filterName("SpamDetectionFilter")
                        .reasons(Arrays.asList("Excessive caps", "Multiple URLs", "Spam keywords"))
                        .build()
        );
        when(scamPatternFilter.analyze(spamText)).thenReturn(
                FilterResult.builder()
                        .flagged(true)
                        .score(0.8)
                        .filterName("ScamPatternFilter")
                        .reasons(Arrays.asList("Suspicious URL"))
                        .build()
        );
        when(harassmentFilter.analyze(spamText)).thenReturn(
                FilterResult.builder()
                        .flagged(false)
                        .score(0.0)
                        .filterName("HarassmentFilter")
                        .build()
        );

        // Act
        ContentAnalysisResult result = contentFilterService.analyzeText(spamText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getOverallScore()).isGreaterThan(0.8);
        assertThat(result.getFlaggedCategories()).contains("SpamDetectionFilter", "ScamPatternFilter");

        verify(spamDetectionFilter, times(1)).analyze(spamText);
        verify(scamPatternFilter, times(1)).analyze(spamText);
    }

    @Test
    void testAnalyzeText_Scam() {
        // Arrange
        String scamText = "I need money urgently. Please send Bitcoin to help me. I'll pay you back double!";

        when(profanityFilter.analyze(scamText)).thenReturn(
                FilterResult.builder()
                        .flagged(false)
                        .score(0.0)
                        .filterName("ProfanityFilter")
                        .build()
        );
        when(spamDetectionFilter.analyze(scamText)).thenReturn(
                FilterResult.builder()
                        .flagged(false)
                        .score(0.3)
                        .filterName("SpamDetectionFilter")
                        .build()
        );
        when(scamPatternFilter.analyze(scamText)).thenReturn(
                FilterResult.builder()
                        .flagged(true)
                        .score(0.9)
                        .filterName("ScamPatternFilter")
                        .reasons(Arrays.asList("Money request", "Cryptocurrency mention", "Investment scam pattern"))
                        .build()
        );
        when(harassmentFilter.analyze(scamText)).thenReturn(
                FilterResult.builder()
                        .flagged(false)
                        .score(0.1)
                        .filterName("HarassmentFilter")
                        .build()
        );

        // Act
        ContentAnalysisResult result = contentFilterService.analyzeText(scamText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getOverallScore()).isGreaterThan(0.7);
        assertThat(result.getFlaggedCategories()).contains("ScamPatternFilter");

        verify(scamPatternFilter, times(1)).analyze(scamText);
    }

    @Test
    void testShouldAutoReject() {
        // Arrange - High confidence score should trigger auto-reject
        ContentAnalysisResult highScoreResult = ContentAnalysisResult.builder()
                .flagged(true)
                .overallScore(0.95)
                .filterResults(Arrays.asList(
                        FilterResult.builder()
                                .flagged(true)
                                .score(0.95)
                                .filterName("ProfanityFilter")
                                .build()
                ))
                .flaggedCategories(Arrays.asList("ProfanityFilter"))
                .build();

        // Act
        boolean shouldReject = contentFilterService.shouldAutoReject(highScoreResult);

        // Assert
        assertThat(shouldReject).isTrue();
    }

    @Test
    void testShouldAutoApprove() {
        // Arrange - Low score should trigger auto-approve
        ContentAnalysisResult lowScoreResult = ContentAnalysisResult.builder()
                .flagged(false)
                .overallScore(0.05)
                .filterResults(Arrays.asList(
                        FilterResult.builder()
                                .flagged(false)
                                .score(0.05)
                                .filterName("ProfanityFilter")
                                .build()
                ))
                .flaggedCategories(Arrays.asList())
                .build();

        // Act
        boolean shouldApprove = contentFilterService.shouldAutoApprove(lowScoreResult);

        // Assert
        assertThat(shouldApprove).isTrue();
    }

    @Test
    void testRequiresHumanReview() {
        // Arrange - Medium score should require human review
        ContentAnalysisResult mediumScoreResult = ContentAnalysisResult.builder()
                .flagged(true)
                .overallScore(0.55)
                .filterResults(Arrays.asList(
                        FilterResult.builder()
                                .flagged(true)
                                .score(0.55)
                                .filterName("ProfanityFilter")
                                .build()
                ))
                .flaggedCategories(Arrays.asList("ProfanityFilter"))
                .build();

        // Act
        boolean requiresReview = contentFilterService.requiresHumanReview(mediumScoreResult);

        // Assert
        assertThat(requiresReview).isTrue();
        assertThat(contentFilterService.shouldAutoReject(mediumScoreResult)).isFalse();
        assertThat(contentFilterService.shouldAutoApprove(mediumScoreResult)).isFalse();
    }
}
