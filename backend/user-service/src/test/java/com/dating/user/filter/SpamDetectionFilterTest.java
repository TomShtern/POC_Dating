package com.dating.user.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SpamDetectionFilterTest {

    private SpamDetectionFilter spamFilter;

    @BeforeEach
    void setUp() {
        spamFilter = new SpamDetectionFilterImpl();
    }

    @Test
    void testAnalyze_Normal() {
        // Arrange
        String normalText = "Hello! I enjoy hiking and photography. Would love to chat about travel experiences.";

        // Act
        FilterResult result = spamFilter.analyze(normalText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isFalse();
        assertThat(result.getScore()).isLessThan(0.2);
        assertThat(result.getFilterName()).isEqualTo("SpamDetectionFilter");
        assertThat(result.getReasons()).isEmpty();
    }

    @Test
    void testAnalyze_ExcessiveCaps() {
        // Arrange
        String capsText = "HEY!!! CLICK HERE NOW!!! AMAZING OFFER!!! DON'T MISS OUT!!!";

        // Act
        FilterResult result = spamFilter.analyze(capsText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.6);
        assertThat(result.getFilterName()).isEqualTo("SpamDetectionFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("excessive caps") ||
            reason.toLowerCase().contains("uppercase") ||
            reason.toLowerCase().contains("capital letters")
        );
    }

    @Test
    void testAnalyze_TooManyURLs() {
        // Arrange
        String urlSpam = "Check out www.site1.com and www.site2.com also visit http://site3.com and https://site4.com";

        // Act
        FilterResult result = spamFilter.analyze(urlSpam);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.5);
        assertThat(result.getFilterName()).isEqualTo("SpamDetectionFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("url") ||
            reason.toLowerCase().contains("link") ||
            reason.toLowerCase().contains("multiple")
        );
    }

    @Test
    void testAnalyze_RepetitiveChars() {
        // Arrange
        String repetitiveText = "Heyyyyyy!!!!!!! This is sooooooo amazing!!!!!!!! Don't missssss out!!!!!";

        // Act
        FilterResult result = spamFilter.analyze(repetitiveText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.5);
        assertThat(result.getFilterName()).isEqualTo("SpamDetectionFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("repetitive") ||
            reason.toLowerCase().contains("repeated") ||
            reason.toLowerCase().contains("character")
        );
    }

    @Test
    void testAnalyze_PhoneNumbers() {
        // Arrange
        String phoneText = "Contact me at 555-123-4567 or call +1-800-555-9999 for more info";

        // Act
        FilterResult result = spamFilter.analyze(phoneText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.4);
        assertThat(result.getFilterName()).isEqualTo("SpamDetectionFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("phone") ||
            reason.toLowerCase().contains("number") ||
            reason.toLowerCase().contains("contact")
        );
    }

    @Test
    void testAnalyze_EmailAddresses() {
        // Arrange
        String emailText = "Reach out to me at spam@example.com or contact@site.com for opportunities";

        // Act
        FilterResult result = spamFilter.analyze(emailText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.4);
        assertThat(result.getFilterName()).isEqualTo("SpamDetectionFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("email") ||
            reason.toLowerCase().contains("address") ||
            reason.toLowerCase().contains("contact")
        );
    }

    @Test
    void testAnalyze_SpamKeywords() {
        // Arrange
        String keywordSpam = "FREE MONEY! Act now! Limited time offer! Click here! Buy now!";

        // Act
        FilterResult result = spamFilter.analyze(keywordSpam);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.7);
        assertThat(result.getFilterName()).isEqualTo("SpamDetectionFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("spam") ||
            reason.toLowerCase().contains("keyword") ||
            reason.toLowerCase().contains("promotional")
        );
    }

    @Test
    void testAnalyze_CombinedSpamSignals() {
        // Arrange
        String multipleSignals = "AMAZING OFFER!!!! Visit www.scam.com and www.fake.com NOW!!! Call 555-1234 IMMEDIATELY!!!";

        // Act
        FilterResult result = spamFilter.analyze(multipleSignals);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.8);  // Multiple signals should increase score
        assertThat(result.getFilterName()).isEqualTo("SpamDetectionFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons().size()).isGreaterThanOrEqualTo(2);  // Multiple reasons
    }

    @Test
    void testAnalyze_OneValidURL() {
        // Arrange
        String singleUrlText = "Check out my photography portfolio at www.mysite.com";

        // Act
        FilterResult result = spamFilter.analyze(singleUrlText);

        // Assert
        assertThat(result).isNotNull();
        // Single URL in normal context should have low spam score
        if (result.isFlagged()) {
            assertThat(result.getScore()).isLessThan(0.5);
        } else {
            assertThat(result.getScore()).isLessThan(0.3);
        }
    }
}
