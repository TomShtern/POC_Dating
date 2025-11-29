package com.dating.user.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ScamPatternFilterTest {

    private ScamPatternFilter scamFilter;

    @BeforeEach
    void setUp() {
        scamFilter = new ScamPatternFilterImpl();
    }

    @Test
    void testAnalyze_Normal() {
        // Arrange
        String normalText = "I love traveling and trying new restaurants. What are your hobbies?";

        // Act
        FilterResult result = scamFilter.analyze(normalText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isFalse();
        assertThat(result.getScore()).isLessThan(0.2);
        assertThat(result.getFilterName()).isEqualTo("ScamPatternFilter");
        assertThat(result.getReasons()).isEmpty();
    }

    @Test
    void testAnalyze_MoneyRequest() {
        // Arrange
        String moneyRequest = "I'm in trouble and need money urgently. Can you send me $500? I'll pay you back I promise.";

        // Act
        FilterResult result = scamFilter.analyze(moneyRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.7);
        assertThat(result.getFilterName()).isEqualTo("ScamPatternFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("money") ||
            reason.toLowerCase().contains("financial") ||
            reason.toLowerCase().contains("payment")
        );
    }

    @Test
    void testAnalyze_CryptoScam() {
        // Arrange
        String cryptoScam = "I made $10,000 with Bitcoin in one week! Invest in crypto now and double your money! Send to this wallet address.";

        // Act
        FilterResult result = scamFilter.analyze(cryptoScam);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.8);
        assertThat(result.getFilterName()).isEqualTo("ScamPatternFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("crypto") ||
            reason.toLowerCase().contains("bitcoin") ||
            reason.toLowerCase().contains("investment")
        );
    }

    @Test
    void testAnalyze_ExternalRedirect() {
        // Arrange
        String redirect = "I can't chat here. Add me on WhatsApp at +1234567890 or message me on Telegram @scammer";

        // Act
        FilterResult result = scamFilter.analyze(redirect);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.6);
        assertThat(result.getFilterName()).isEqualTo("ScamPatternFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("external") ||
            reason.toLowerCase().contains("platform") ||
            reason.toLowerCase().contains("redirect") ||
            reason.toLowerCase().contains("whatsapp") ||
            reason.toLowerCase().contains("telegram")
        );
    }

    @Test
    void testAnalyze_SugarDaddyScam() {
        // Arrange
        String sugarScam = "Looking for a sugar baby. I'll give you $5000 weekly allowance. Just need your bank account for direct deposit.";

        // Act
        FilterResult result = scamFilter.analyze(sugarScam);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.8);
        assertThat(result.getFilterName()).isEqualTo("ScamPatternFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("sugar") ||
            reason.toLowerCase().contains("allowance") ||
            reason.toLowerCase().contains("financial") ||
            reason.toLowerCase().contains("bank account")
        );
    }

    @Test
    void testAnalyze_EmergencyScam() {
        // Arrange
        String emergencyScam = "URGENT! I'm stranded at the airport and lost my wallet. Please wire money to this Western Union location!";

        // Act
        FilterResult result = scamFilter.analyze(emergencyScam);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.7);
        assertThat(result.getFilterName()).isEqualTo("ScamPatternFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("emergency") ||
            reason.toLowerCase().contains("urgent") ||
            reason.toLowerCase().contains("wire") ||
            reason.toLowerCase().contains("western union")
        );
    }

    @Test
    void testAnalyze_GiftCardScam() {
        // Arrange
        String giftCardScam = "Can you buy me iTunes gift cards? I'll reimburse you. Just send the codes to me.";

        // Act
        FilterResult result = scamFilter.analyze(giftCardScam);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.7);
        assertThat(result.getFilterName()).isEqualTo("ScamPatternFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("gift card") ||
            reason.toLowerCase().contains("itunes") ||
            reason.toLowerCase().contains("prepaid")
        );
    }

    @Test
    void testAnalyze_InheritanceScam() {
        // Arrange
        String inheritanceScam = "You've been selected to receive a $1 million inheritance. Just pay the processing fee of $500.";

        // Act
        FilterResult result = scamFilter.analyze(inheritanceScam);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.8);
        assertThat(result.getFilterName()).isEqualTo("ScamPatternFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("inheritance") ||
            reason.toLowerCase().contains("selected") ||
            reason.toLowerCase().contains("fee") ||
            reason.toLowerCase().contains("million")
        );
    }

    @Test
    void testAnalyze_LegitimateMoneyMention() {
        // Arrange
        String legitText = "I work in finance and enjoy investing. What do you do for a living?";

        // Act
        FilterResult result = scamFilter.analyze(legitText);

        // Assert
        assertThat(result).isNotNull();
        // Legitimate mention of money/finance should have low score
        if (result.isFlagged()) {
            assertThat(result.getScore()).isLessThan(0.4);
        } else {
            assertThat(result.getScore()).isLessThan(0.2);
        }
    }
}
