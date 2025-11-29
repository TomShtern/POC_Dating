package com.dating.user.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HarassmentFilterTest {

    private HarassmentFilter harassmentFilter;

    @BeforeEach
    void setUp() {
        harassmentFilter = new HarassmentFilterImpl();
    }

    @Test
    void testAnalyze_Normal() {
        // Arrange
        String normalText = "Hi! I'd love to get to know you better. What kind of music do you like?";

        // Act
        FilterResult result = harassmentFilter.analyze(normalText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isFalse();
        assertThat(result.getScore()).isLessThan(0.2);
        assertThat(result.getFilterName()).isEqualTo("HarassmentFilter");
        assertThat(result.getReasons()).isEmpty();
    }

    @Test
    void testAnalyze_Threats() {
        // Arrange
        String threatText = "I'm going to find you and hurt you. You better watch your back. I know where you live.";

        // Act
        FilterResult result = harassmentFilter.analyze(threatText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.8);
        assertThat(result.getFilterName()).isEqualTo("HarassmentFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("threat") ||
            reason.toLowerCase().contains("violence") ||
            reason.toLowerCase().contains("intimidation")
        );
    }

    @Test
    void testAnalyze_HateSpeech() {
        // Arrange
        String hateSpeech = "You people are disgusting and should go back to where you came from. Your kind doesn't belong here.";

        // Act
        FilterResult result = harassmentFilter.analyze(hateSpeech);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.7);
        assertThat(result.getFilterName()).isEqualTo("HarassmentFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("hate") ||
            reason.toLowerCase().contains("discriminat") ||
            reason.toLowerCase().contains("offensive")
        );
    }

    @Test
    void testAnalyze_Aggressive() {
        // Arrange
        String aggressiveText = "You're an idiot and nobody likes you. Stop messaging me or else!";

        // Act
        FilterResult result = harassmentFilter.analyze(aggressiveText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.6);
        assertThat(result.getFilterName()).isEqualTo("HarassmentFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("aggressive") ||
            reason.toLowerCase().contains("hostile") ||
            reason.toLowerCase().contains("abusive")
        );
    }

    @Test
    void testAnalyze_SexualHarassment() {
        // Arrange
        String sexualHarassment = "Send me nudes or I'll share your photos. You know you want to. Don't be a tease.";

        // Act
        FilterResult result = harassmentFilter.analyze(sexualHarassment);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.8);
        assertThat(result.getFilterName()).isEqualTo("HarassmentFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("sexual") ||
            reason.toLowerCase().contains("harassment") ||
            reason.toLowerCase().contains("inappropriate")
        );
    }

    @Test
    void testAnalyze_Stalking() {
        // Arrange
        String stalkingText = "I've been watching you. I saw you at the coffee shop yesterday. I know your schedule and where you work.";

        // Act
        FilterResult result = harassmentFilter.analyze(stalkingText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.7);
        assertThat(result.getFilterName()).isEqualTo("HarassmentFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("stalk") ||
            reason.toLowerCase().contains("watching") ||
            reason.toLowerCase().contains("following")
        );
    }

    @Test
    void testAnalyze_Bullying() {
        // Arrange
        String bullyingText = "You're ugly and fat. No wonder you're alone. Nobody would ever want to date someone like you.";

        // Act
        FilterResult result = harassmentFilter.analyze(bullyingText);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.7);
        assertThat(result.getFilterName()).isEqualTo("HarassmentFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("bully") ||
            reason.toLowerCase().contains("insult") ||
            reason.toLowerCase().contains("degrading")
        );
    }

    @Test
    void testAnalyze_PersistentUnwantedContact() {
        // Arrange
        String unwantedContact = "Why aren't you responding? Answer me now! I've messaged you 10 times. You can't ignore me!";

        // Act
        FilterResult result = harassmentFilter.analyze(unwantedContact);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.5);
        assertThat(result.getFilterName()).isEqualTo("HarassmentFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("persistent") ||
            reason.toLowerCase().contains("unwanted") ||
            reason.toLowerCase().contains("demanding")
        );
    }

    @Test
    void testAnalyze_BorderlineContent() {
        // Arrange
        String borderlineText = "I'm disappointed you didn't respond. I thought we had a good conversation.";

        // Act
        FilterResult result = harassmentFilter.analyze(borderlineText);

        // Assert
        assertThat(result).isNotNull();
        // Borderline content should have lower score
        if (result.isFlagged()) {
            assertThat(result.getScore()).isLessThan(0.5);
        } else {
            assertThat(result.getScore()).isLessThan(0.3);
        }
    }

    @Test
    void testAnalyze_RacialSlurs() {
        // Arrange
        String racialSlurs = "You're just another [racial slur]. Your race is inferior to mine.";

        // Act
        FilterResult result = harassmentFilter.analyze(racialSlurs);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0.8);
        assertThat(result.getFilterName()).isEqualTo("HarassmentFilter");
        assertThat(result.getReasons()).isNotEmpty();
        assertThat(result.getReasons()).anyMatch(reason ->
            reason.toLowerCase().contains("hate") ||
            reason.toLowerCase().contains("discriminat") ||
            reason.toLowerCase().contains("racial") ||
            reason.toLowerCase().contains("slur")
        );
    }
}
