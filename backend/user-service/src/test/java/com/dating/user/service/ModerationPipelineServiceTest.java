package com.dating.user.service;

import com.dating.user.dto.ContentAnalysisResult;
import com.dating.user.dto.ModerationQueueItemDTO;
import com.dating.user.filter.FilterResult;
import com.dating.user.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModerationPipelineServiceTest {

    @Mock
    private ContentFilterService contentFilterService;

    @Mock
    private ModerationQueueService moderationQueueService;

    @Mock
    private PunishmentService punishmentService;

    @InjectMocks
    private ModerationPipelineServiceImpl pipelineService;

    private UUID userId;
    private UUID photoId;
    private String photoUrl;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        photoId = UUID.randomUUID();
        photoUrl = "https://example.com/photos/user123.jpg";
    }

    @Test
    void testProcessPhotoUpload_AutoApprove() {
        // Arrange
        ContentAnalysisResult cleanResult = ContentAnalysisResult.builder()
                .flagged(false)
                .overallScore(0.02)
                .filterResults(Arrays.asList(
                        FilterResult.builder()
                                .flagged(false)
                                .score(0.02)
                                .filterName("ImageContentFilter")
                                .build()
                ))
                .flaggedCategories(Arrays.asList())
                .build();

        when(contentFilterService.analyzeImage(photoUrl)).thenReturn(cleanResult);
        when(contentFilterService.shouldAutoApprove(cleanResult)).thenReturn(true);

        // Act
        ModerationResult result = pipelineService.processPhotoUpload(userId, photoId, photoUrl);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDecision()).isEqualTo(ModerationDecision.APPROVE);
        assertThat(result.isAutomatic()).isTrue();
        assertThat(result.getConfidenceScore()).isLessThan(0.1);

        verify(contentFilterService, times(1)).analyzeImage(photoUrl);
        verify(contentFilterService, times(1)).shouldAutoApprove(cleanResult);
        verify(moderationQueueService, never()).addToQueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testProcessPhotoUpload_AutoReject() {
        // Arrange
        ContentAnalysisResult explicitResult = ContentAnalysisResult.builder()
                .flagged(true)
                .overallScore(0.98)
                .filterResults(Arrays.asList(
                        FilterResult.builder()
                                .flagged(true)
                                .score(0.98)
                                .filterName("ExplicitContentFilter")
                                .reasons(Arrays.asList("Explicit content detected"))
                                .build()
                ))
                .flaggedCategories(Arrays.asList("ExplicitContentFilter"))
                .build();

        when(contentFilterService.analyzeImage(photoUrl)).thenReturn(explicitResult);
        when(contentFilterService.shouldAutoReject(explicitResult)).thenReturn(true);

        // Act
        ModerationResult result = pipelineService.processPhotoUpload(userId, photoId, photoUrl);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDecision()).isEqualTo(ModerationDecision.REJECT);
        assertThat(result.isAutomatic()).isTrue();
        assertThat(result.getConfidenceScore()).isGreaterThan(0.9);

        verify(contentFilterService, times(1)).analyzeImage(photoUrl);
        verify(contentFilterService, times(1)).shouldAutoReject(explicitResult);
        verify(moderationQueueService, never()).addToQueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testProcessPhotoUpload_HumanReview() {
        // Arrange
        ContentAnalysisResult uncertainResult = ContentAnalysisResult.builder()
                .flagged(true)
                .overallScore(0.65)
                .filterResults(Arrays.asList(
                        FilterResult.builder()
                                .flagged(true)
                                .score(0.65)
                                .filterName("SuggestiveContentFilter")
                                .reasons(Arrays.asList("Potentially suggestive content"))
                                .build()
                ))
                .flaggedCategories(Arrays.asList("SuggestiveContentFilter"))
                .build();

        ModerationQueueItemDTO queueItem = ModerationQueueItemDTO.builder()
                .id(UUID.randomUUID())
                .contentType(ContentType.PHOTO)
                .contentId(photoId)
                .userId(userId)
                .status(ModerationStatus.PENDING)
                .build();

        when(contentFilterService.analyzeImage(photoUrl)).thenReturn(uncertainResult);
        when(contentFilterService.shouldAutoReject(uncertainResult)).thenReturn(false);
        when(contentFilterService.shouldAutoApprove(uncertainResult)).thenReturn(false);
        when(contentFilterService.requiresHumanReview(uncertainResult)).thenReturn(true);
        when(moderationQueueService.addToQueue(
                eq(ContentType.PHOTO),
                eq(photoId),
                eq(userId),
                anyString(),
                any(ModerationPriority.class),
                any(Map.class)
        )).thenReturn(queueItem);

        // Act
        ModerationResult result = pipelineService.processPhotoUpload(userId, photoId, photoUrl);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDecision()).isEqualTo(ModerationDecision.PENDING_REVIEW);
        assertThat(result.isAutomatic()).isFalse();
        assertThat(result.getQueueItemId()).isEqualTo(queueItem.getId());

        verify(contentFilterService, times(1)).analyzeImage(photoUrl);
        verify(contentFilterService, times(1)).requiresHumanReview(uncertainResult);
        verify(moderationQueueService, times(1)).addToQueue(
                eq(ContentType.PHOTO),
                eq(photoId),
                eq(userId),
                anyString(),
                any(ModerationPriority.class),
                any(Map.class)
        );
    }

    @Test
    void testProcessMessage_MutedUser() {
        // Arrange
        UUID matchId = UUID.randomUUID();
        String messageContent = "Hello there!";

        when(punishmentService.isMuted(userId)).thenReturn(true);

        // Act
        ModerationResult result = pipelineService.processMessage(userId, matchId, messageContent);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDecision()).isEqualTo(ModerationDecision.REJECT);
        assertThat(result.isAutomatic()).isTrue();
        assertThat(result.getRejectionReason()).contains("muted");

        verify(punishmentService, times(1)).isMuted(userId);
        verify(contentFilterService, never()).analyzeText(any());
    }

    @Test
    void testProcessMessage_AutoReject() {
        // Arrange
        UUID matchId = UUID.randomUUID();
        String messageContent = "Send money to www.scam.com NOW!!!";

        ContentAnalysisResult scamResult = ContentAnalysisResult.builder()
                .flagged(true)
                .overallScore(0.95)
                .filterResults(Arrays.asList(
                        FilterResult.builder()
                                .flagged(true)
                                .score(0.95)
                                .filterName("ScamPatternFilter")
                                .reasons(Arrays.asList("Scam pattern detected"))
                                .build()
                ))
                .flaggedCategories(Arrays.asList("ScamPatternFilter"))
                .build();

        when(punishmentService.isMuted(userId)).thenReturn(false);
        when(contentFilterService.analyzeText(messageContent)).thenReturn(scamResult);
        when(contentFilterService.shouldAutoReject(scamResult)).thenReturn(true);

        // Act
        ModerationResult result = pipelineService.processMessage(userId, matchId, messageContent);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDecision()).isEqualTo(ModerationDecision.REJECT);
        assertThat(result.isAutomatic()).isTrue();
        assertThat(result.getRejectionReason()).contains("inappropriate content");

        verify(punishmentService, times(1)).isMuted(userId);
        verify(contentFilterService, times(1)).analyzeText(messageContent);
        verify(contentFilterService, times(1)).shouldAutoReject(scamResult);
    }

    @Test
    void testProcessMessage_FlaggedButApproved() {
        // Arrange
        UUID matchId = UUID.randomUUID();
        String messageContent = "This message has some borderline content";

        ContentAnalysisResult borderlineResult = ContentAnalysisResult.builder()
                .flagged(true)
                .overallScore(0.45)
                .filterResults(Arrays.asList(
                        FilterResult.builder()
                                .flagged(true)
                                .score(0.45)
                                .filterName("ProfanityFilter")
                                .reasons(Arrays.asList("Mild profanity"))
                                .build()
                ))
                .flaggedCategories(Arrays.asList("ProfanityFilter"))
                .build();

        when(punishmentService.isMuted(userId)).thenReturn(false);
        when(contentFilterService.analyzeText(messageContent)).thenReturn(borderlineResult);
        when(contentFilterService.shouldAutoReject(borderlineResult)).thenReturn(false);
        when(contentFilterService.shouldAutoApprove(borderlineResult)).thenReturn(true);

        // Act
        ModerationResult result = pipelineService.processMessage(userId, matchId, messageContent);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDecision()).isEqualTo(ModerationDecision.APPROVE);
        assertThat(result.isAutomatic()).isTrue();
        assertThat(result.isFlagged()).isTrue();  // Flagged but still approved
        assertThat(result.getConfidenceScore()).isGreaterThan(0.0);

        verify(punishmentService, times(1)).isMuted(userId);
        verify(contentFilterService, times(1)).analyzeText(messageContent);
    }

    @Test
    void testProcessProfileUpdate() {
        // Arrange
        String bioContent = "Love hiking, photography, and meeting new people!";

        ContentAnalysisResult cleanResult = ContentAnalysisResult.builder()
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

        when(contentFilterService.analyzeText(bioContent)).thenReturn(cleanResult);
        when(contentFilterService.shouldAutoApprove(cleanResult)).thenReturn(true);

        // Act
        ModerationResult result = pipelineService.processProfileUpdate(userId, bioContent);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDecision()).isEqualTo(ModerationDecision.APPROVE);
        assertThat(result.isAutomatic()).isTrue();
        assertThat(result.isFlagged()).isFalse();

        verify(contentFilterService, times(1)).analyzeText(bioContent);
        verify(contentFilterService, times(1)).shouldAutoApprove(cleanResult);
    }
}
