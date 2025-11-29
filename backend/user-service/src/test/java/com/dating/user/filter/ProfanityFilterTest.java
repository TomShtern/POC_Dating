package com.dating.user.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProfanityFilterTest {

    private ProfanityFilter profanityFilter;

    @BeforeEach
    void setUp() {
        profanityFilter = new ProfanityFilterImpl();
    }

    @Test
    void testAnalyze_CleanText() {
        // Arrange
        String cleanText = "I enjoy hiking, photography, and meeting new people. Looking forward to connecting!";

        // Act
        FilterResult result = profanityFilter.analyze(cleanText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isFalse();
        assertThat(result.getScore()).isLessThan(0.1);
        assertThat(result.getFilterName()).isEqualTo("ProfanityFilter");
        assertThat(result.getReasons()).isEmpty();
    }

    @Test
    void testAnalyze_Profanity() {
        // Arrange
        String profaneText = "This is some damn bullshit and you're a fucking idiot";

        // Act
        FilterResult result = profanityFilter.analyze(profaneText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.7);
        assertThat(result.getFilterName()).isEqualTo("ProfanityFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("profanity") ||
            reason.toLowerCase().contains("offensive language")
        );
    }

    @Test
    void testAnalyze_ObfuscatedProfanity() {
        // Arrange
        String obfuscatedText = "You are such a d@mn f***ing sh1t for brains";

        // Act
        FilterResult result = profanityFilter.analyze(obfuscatedText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.5);
        assertThat(result.getFilterName()).isEqualTo("ProfanityFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("obfuscated") ||
            reason.toLowerCase().contains("profanity") ||
            reason.toLowerCase().contains("masked")
        );
    }

    @Test
    void testAnalyze_MixedContent() {
        // Arrange
        String mixedText = "I really love this damn city! The architecture is amazing and the food is great.";

        // Act
        FilterResult result = profanityFilter.analyze(mixedText);

        // Assert
        assertThat(result).isNotNull();
        // Mild profanity in otherwise positive context
        // Should be flagged but with lower score
        if (result.isFlagged()) {
            assertThat(result.getScore()).isBetween(0.2, 0.6);
            assertThat(result.getFilterName()).isEqualTo("ProfanityFilter");
            assertThat(result.getReasons()).isNotEmpty();
        } else {
            // If not flagged, score should be low
            assertThat(result.getScore()).isLessThan(0.3);
        }
    }

    @Test
    void testAnalyze_EmptyText() {
        // Arrange
        String emptyText = "";

        // Act
        FilterResult result = profanityFilter.analyze(emptyText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isFalse();
        assertThat(result.getScore()).isEqualTo(0.0);
        assertThat(result.getFilterName()).isEqualTo("ProfanityFilter");
    }

    @Test
    void testAnalyze_NullText() {
        // Arrange
        String nullText = null;

        // Act
        FilterResult result = profanityFilter.analyze(nullText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isFalse();
        assertThat(result.getScore()).isEqualTo(0.0);
        assertThat(result.getFilterName()).isEqualTo("ProfanityFilter");
    }

    @Test
    void testAnalyze_CaseInsensitive() {
        // Arrange
        String upperCaseText = "YOU ARE A DAMN FOOL";
        String lowerCaseText = "you are a damn fool";
        String mixedCaseText = "YoU aRe A DaMn FoOl";

        // Act
        FilterResult upperResult = profanityFilter.analyze(upperCaseText);
        FilterResult lowerResult = profanityFilter.analyze(lowerCaseText);
        FilterResult mixedResult = profanityFilter.analyze(mixedCaseText);

        // Assert - All should be detected similarly
        assertThat(upperResult.isFlagged()).isTrue();
        assertThat(lowerResult.isFlagged()).isTrue();
        assertThat(mixedResult.isFlagged()).isTrue();

        // Scores should be similar (within reasonable range)
        assertThat(upperResult.getScore()).isBetween(lowerResult.getScore() - 0.2, lowerResult.getScore() + 0.2);
        assertThat(mixedResult.getScore()).isBetween(lowerResult.getScore() - 0.2, lowerResult.getScore() + 0.2);
    }
}
