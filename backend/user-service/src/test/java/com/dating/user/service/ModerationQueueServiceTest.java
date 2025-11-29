package com.dating.user.service;

import com.dating.user.dto.ModerationDecisionRequest;
import com.dating.user.dto.ModerationQueueItemDTO;
import com.dating.user.dto.ModerationStatisticsDTO;
import com.dating.user.exception.ModerationItemNotFoundException;
import com.dating.user.model.*;
import com.dating.user.repository.ModerationQueueRepository;
import com.dating.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModerationQueueServiceTest {

    @Mock
    private ModerationQueueRepository queueRepository;

    @Mock
    private PunishmentService punishmentService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ModerationQueueServiceImpl queueService;

    private ModerationQueueItem testItem;
    private User testUser;
    private User moderator;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .status(UserStatus.ACTIVE)
                .build();

        moderator = User.builder()
                .id(UUID.randomUUID())
                .username("moderator")
                .email("mod@example.com")
                .role(UserRole.MODERATOR)
                .build();

        testItem = ModerationQueueItem.builder()
                .id(UUID.randomUUID())
                .contentType(ContentType.PHOTO)
                .contentId(UUID.randomUUID())
                .userId(testUser.getId())
                .status(ModerationStatus.PENDING)
                .priority(ModerationPriority.MEDIUM)
                .flaggedReason("Inappropriate content")
                .createdAt(LocalDateTime.now())
                .metadata("{\"photoUrl\": \"http://example.com/photo.jpg\"}")
                .build();
    }

    @Test
    void testGetQueue_WithFilters() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<ModerationQueueItem> items = Arrays.asList(testItem);
        Page<ModerationQueueItem> page = new PageImpl<>(items, pageable, 1);

        when(queueRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // Act
        Page<ModerationQueueItemDTO> result = queueService.getQueue(
                ModerationStatus.PENDING,
                ContentType.PHOTO,
                ModerationPriority.MEDIUM,
                null,
                pageable
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(testItem.getId());
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ModerationStatus.PENDING);
        verify(queueRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void testGetItem_Found() {
        // Arrange
        when(queueRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));

        // Act
        ModerationQueueItemDTO result = queueService.getItem(testItem.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testItem.getId());
        assertThat(result.getContentType()).isEqualTo(ContentType.PHOTO);
        assertThat(result.getStatus()).isEqualTo(ModerationStatus.PENDING);
        assertThat(result.getFlaggedReason()).isEqualTo("Inappropriate content");
        verify(queueRepository, times(1)).findById(testItem.getId());
    }

    @Test
    void testGetItem_NotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(queueRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> queueService.getItem(nonExistentId))
                .isInstanceOf(ModerationItemNotFoundException.class)
                .hasMessageContaining(nonExistentId.toString());
        verify(queueRepository, times(1)).findById(nonExistentId);
    }

    @Test
    void testAddToQueue() {
        // Arrange
        UUID contentId = UUID.randomUUID();
        when(queueRepository.save(any(ModerationQueueItem.class))).thenReturn(testItem);

        // Act
        ModerationQueueItemDTO result = queueService.addToQueue(
                ContentType.PHOTO,
                contentId,
                testUser.getId(),
                "Flagged by AI filter",
                ModerationPriority.MEDIUM,
                Map.of("photoUrl", "http://example.com/photo.jpg")
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContentType()).isEqualTo(ContentType.PHOTO);

        ArgumentCaptor<ModerationQueueItem> captor = ArgumentCaptor.forClass(ModerationQueueItem.class);
        verify(queueRepository, times(1)).save(captor.capture());

        ModerationQueueItem saved = captor.getValue();
        assertThat(saved.getContentType()).isEqualTo(ContentType.PHOTO);
        assertThat(saved.getContentId()).isEqualTo(contentId);
        assertThat(saved.getUserId()).isEqualTo(testUser.getId());
        assertThat(saved.getStatus()).isEqualTo(ModerationStatus.PENDING);
        assertThat(saved.getPriority()).isEqualTo(ModerationPriority.MEDIUM);
    }

    @Test
    void testAssignToModerator() {
        // Arrange
        when(queueRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));
        when(userRepository.findById(moderator.getId())).thenReturn(Optional.of(moderator));
        when(queueRepository.save(any(ModerationQueueItem.class))).thenReturn(testItem);

        // Act
        ModerationQueueItemDTO result = queueService.assignToModerator(testItem.getId(), moderator.getId());

        // Assert
        assertThat(result).isNotNull();

        ArgumentCaptor<ModerationQueueItem> captor = ArgumentCaptor.forClass(ModerationQueueItem.class);
        verify(queueRepository, times(1)).save(captor.capture());

        ModerationQueueItem assigned = captor.getValue();
        assertThat(assigned.getAssignedModeratorId()).isEqualTo(moderator.getId());
        assertThat(assigned.getStatus()).isEqualTo(ModerationStatus.IN_REVIEW);
        assertThat(assigned.getAssignedAt()).isNotNull();
    }

    @Test
    void testProcessDecision_Approve() {
        // Arrange
        testItem.setAssignedModeratorId(moderator.getId());
        ModerationDecisionRequest request = ModerationDecisionRequest.builder()
                .decision(ModerationDecision.APPROVE)
                .moderatorNotes("Content is appropriate")
                .build();

        when(queueRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));
        when(queueRepository.save(any(ModerationQueueItem.class))).thenReturn(testItem);

        // Act
        ModerationQueueItemDTO result = queueService.processDecision(
                testItem.getId(),
                moderator.getId(),
                request
        );

        // Assert
        assertThat(result).isNotNull();

        ArgumentCaptor<ModerationQueueItem> captor = ArgumentCaptor.forClass(ModerationQueueItem.class);
        verify(queueRepository, times(1)).save(captor.capture());

        ModerationQueueItem processed = captor.getValue();
        assertThat(processed.getStatus()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(processed.getDecision()).isEqualTo(ModerationDecision.APPROVE);
        assertThat(processed.getModeratorNotes()).isEqualTo("Content is appropriate");
        assertThat(processed.getReviewedAt()).isNotNull();

        // Verify no punishment was applied
        verify(punishmentService, never()).warn(any(), any(), any());
        verify(punishmentService, never()).suspend(any(), any(), any(), any());
    }

    @Test
    void testProcessDecision_RejectWithBan() {
        // Arrange
        testItem.setAssignedModeratorId(moderator.getId());
        ModerationDecisionRequest request = ModerationDecisionRequest.builder()
                .decision(ModerationDecision.REJECT)
                .moderatorNotes("Explicit content violation")
                .punishmentType(PunishmentType.BAN)
                .punishmentReason("Explicit content")
                .build();

        when(queueRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));
        when(queueRepository.save(any(ModerationQueueItem.class))).thenReturn(testItem);

        // Act
        ModerationQueueItemDTO result = queueService.processDecision(
                testItem.getId(),
                moderator.getId(),
                request
        );

        // Assert
        assertThat(result).isNotNull();

        ArgumentCaptor<ModerationQueueItem> captor = ArgumentCaptor.forClass(ModerationQueueItem.class);
        verify(queueRepository, times(1)).save(captor.capture());

        ModerationQueueItem processed = captor.getValue();
        assertThat(processed.getStatus()).isEqualTo(ModerationStatus.REJECTED);
        assertThat(processed.getDecision()).isEqualTo(ModerationDecision.REJECT);

        // Verify punishment was applied
        verify(punishmentService, times(1)).ban(
                eq(testUser.getId()),
                eq("Explicit content"),
                eq(moderator.getId())
        );
    }

    @Test
    void testEscalate() {
        // Arrange
        when(queueRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));
        when(queueRepository.save(any(ModerationQueueItem.class))).thenReturn(testItem);

        String escalationReason = "Complex case requiring senior review";

        // Act
        ModerationQueueItemDTO result = queueService.escalate(testItem.getId(), escalationReason);

        // Assert
        assertThat(result).isNotNull();

        ArgumentCaptor<ModerationQueueItem> captor = ArgumentCaptor.forClass(ModerationQueueItem.class);
        verify(queueRepository, times(1)).save(captor.capture());

        ModerationQueueItem escalated = captor.getValue();
        assertThat(escalated.getPriority()).isEqualTo(ModerationPriority.HIGH);
        assertThat(escalated.getEscalatedReason()).isEqualTo(escalationReason);
        assertThat(escalated.getEscalatedAt()).isNotNull();
    }

    @Test
    void testGetStatistics() {
        // Arrange
        when(queueRepository.countByStatus(ModerationStatus.PENDING)).thenReturn(10L);
        when(queueRepository.countByStatus(ModerationStatus.IN_REVIEW)).thenReturn(5L);
        when(queueRepository.countByStatus(ModerationStatus.APPROVED)).thenReturn(100L);
        when(queueRepository.countByStatus(ModerationStatus.REJECTED)).thenReturn(20L);
        when(queueRepository.countByPriority(ModerationPriority.LOW)).thenReturn(3L);
        when(queueRepository.countByPriority(ModerationPriority.MEDIUM)).thenReturn(7L);
        when(queueRepository.countByPriority(ModerationPriority.HIGH)).thenReturn(5L);
        when(queueRepository.findAverageReviewTime()).thenReturn(25.5);

        // Act
        ModerationStatisticsDTO stats = queueService.getStatistics();

        // Assert
        assertThat(stats).isNotNull();
        assertThat(stats.getPendingCount()).isEqualTo(10L);
        assertThat(stats.getInReviewCount()).isEqualTo(5L);
        assertThat(stats.getApprovedCount()).isEqualTo(100L);
        assertThat(stats.getRejectedCount()).isEqualTo(20L);
        assertThat(stats.getLowPriorityCount()).isEqualTo(3L);
        assertThat(stats.getMediumPriorityCount()).isEqualTo(7L);
        assertThat(stats.getHighPriorityCount()).isEqualTo(5L);
        assertThat(stats.getAverageReviewTimeMinutes()).isEqualTo(25.5);

        verify(queueRepository, times(1)).countByStatus(ModerationStatus.PENDING);
        verify(queueRepository, times(1)).countByStatus(ModerationStatus.IN_REVIEW);
        verify(queueRepository, times(1)).countByStatus(ModerationStatus.APPROVED);
        verify(queueRepository, times(1)).countByStatus(ModerationStatus.REJECTED);
        verify(queueRepository, times(1)).findAverageReviewTime();
    }
}
