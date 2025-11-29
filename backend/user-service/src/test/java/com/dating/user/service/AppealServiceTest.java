package com.dating.user.service;

import com.dating.user.dto.AppealDTO;
import com.dating.user.dto.CreateAppealRequest;
import com.dating.user.exception.AppealNotFoundException;
import com.dating.user.exception.DuplicateAppealException;
import com.dating.user.model.*;
import com.dating.user.repository.AppealRepository;
import com.dating.user.repository.PunishmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppealServiceTest {

    @Mock
    private AppealRepository appealRepository;

    @Mock
    private PunishmentRepository punishmentRepository;

    @Mock
    private PunishmentService punishmentService;

    @InjectMocks
    private AppealServiceImpl appealService;

    private User testUser;
    private User moderator;
    private Punishment testPunishment;
    private Appeal testAppeal;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .status(UserStatus.SUSPENDED)
                .build();

        moderator = User.builder()
                .id(UUID.randomUUID())
                .username("moderator")
                .email("mod@example.com")
                .role(UserRole.MODERATOR)
                .build();

        testPunishment = Punishment.builder()
                .id(UUID.randomUUID())
                .userId(testUser.getId())
                .punishmentType(PunishmentType.SUSPENSION)
                .reason("Multiple policy violations")
                .issuedById(moderator.getId())
                .issuedAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(6))
                .active(true)
                .build();

        testAppeal = Appeal.builder()
                .id(UUID.randomUUID())
                .punishmentId(testPunishment.getId())
                .userId(testUser.getId())
                .reason("I believe this was a misunderstanding")
                .status(AppealStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateAppeal_Success() {
        // Arrange
        CreateAppealRequest request = CreateAppealRequest.builder()
                .punishmentId(testPunishment.getId())
                .reason("I believe this was a misunderstanding")
                .additionalContext("I have reviewed the community guidelines")
                .build();

        when(punishmentRepository.findById(testPunishment.getId()))
                .thenReturn(Optional.of(testPunishment));
        when(appealRepository.existsByPunishmentIdAndStatusIn(
                eq(testPunishment.getId()),
                anyList()
        )).thenReturn(false);
        when(appealRepository.save(any(Appeal.class))).thenReturn(testAppeal);

        // Act
        AppealDTO result = appealService.createAppeal(testUser.getId(), request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getReason()).isEqualTo(request.getReason());

        ArgumentCaptor<Appeal> captor = ArgumentCaptor.forClass(Appeal.class);
        verify(appealRepository, times(1)).save(captor.capture());

        Appeal saved = captor.getValue();
        assertThat(saved.getPunishmentId()).isEqualTo(testPunishment.getId());
        assertThat(saved.getUserId()).isEqualTo(testUser.getId());
        assertThat(saved.getReason()).isEqualTo(request.getReason());
        assertThat(saved.getAdditionalContext()).isEqualTo(request.getAdditionalContext());
        assertThat(saved.getStatus()).isEqualTo(AppealStatus.PENDING);
    }

    @Test
    void testCreateAppeal_Duplicate() {
        // Arrange
        CreateAppealRequest request = CreateAppealRequest.builder()
                .punishmentId(testPunishment.getId())
                .reason("Duplicate appeal")
                .build();

        when(punishmentRepository.findById(testPunishment.getId()))
                .thenReturn(Optional.of(testPunishment));
        when(appealRepository.existsByPunishmentIdAndStatusIn(
                eq(testPunishment.getId()),
                anyList()
        )).thenReturn(true);  // Duplicate exists

        // Act & Assert
        assertThatThrownBy(() -> appealService.createAppeal(testUser.getId(), request))
                .isInstanceOf(DuplicateAppealException.class)
                .hasMessageContaining("already have a pending or under review appeal");

        verify(appealRepository, never()).save(any());
    }

    @Test
    void testUpholdDecision() {
        // Arrange
        String moderatorNotes = "After review, the original decision stands";

        when(appealRepository.findById(testAppeal.getId())).thenReturn(Optional.of(testAppeal));
        when(appealRepository.save(any(Appeal.class))).thenReturn(testAppeal);

        // Act
        AppealDTO result = appealService.upholdDecision(
                testAppeal.getId(),
                moderator.getId(),
                moderatorNotes
        );

        // Assert
        assertThat(result).isNotNull();

        ArgumentCaptor<Appeal> captor = ArgumentCaptor.forClass(Appeal.class);
        verify(appealRepository, times(1)).save(captor.capture());

        Appeal upheld = captor.getValue();
        assertThat(upheld.getStatus()).isEqualTo(AppealStatus.REJECTED);
        assertThat(upheld.getReviewedById()).isEqualTo(moderator.getId());
        assertThat(upheld.getModeratorNotes()).isEqualTo(moderatorNotes);
        assertThat(upheld.getReviewedAt()).isNotNull();

        // Verify punishment was NOT revoked
        verify(punishmentService, never()).revoke(any(), any(), any());
    }

    @Test
    void testOverturnDecision() {
        // Arrange
        String moderatorNotes = "Upon further review, the punishment was unwarranted";

        when(appealRepository.findById(testAppeal.getId())).thenReturn(Optional.of(testAppeal));
        when(punishmentRepository.findById(testPunishment.getId()))
                .thenReturn(Optional.of(testPunishment));
        when(appealRepository.save(any(Appeal.class))).thenReturn(testAppeal);

        // Act
        AppealDTO result = appealService.overturnDecision(
                testAppeal.getId(),
                moderator.getId(),
                moderatorNotes
        );

        // Assert
        assertThat(result).isNotNull();

        ArgumentCaptor<Appeal> captor = ArgumentCaptor.forClass(Appeal.class);
        verify(appealRepository, times(1)).save(captor.capture());

        Appeal overturned = captor.getValue();
        assertThat(overturned.getStatus()).isEqualTo(AppealStatus.APPROVED);
        assertThat(overturned.getReviewedById()).isEqualTo(moderator.getId());
        assertThat(overturned.getModeratorNotes()).isEqualTo(moderatorNotes);
        assertThat(overturned.getReviewedAt()).isNotNull();

        // Verify punishment WAS revoked
        verify(punishmentService, times(1)).revoke(
                eq(testPunishment.getId()),
                contains("Appeal approved"),
                eq(moderator.getId())
        );
    }

    @Test
    void testCanUserAppeal() {
        // Arrange
        when(punishmentRepository.findById(testPunishment.getId()))
                .thenReturn(Optional.of(testPunishment));
        when(appealRepository.countByPunishmentIdAndStatus(
                testPunishment.getId(),
                AppealStatus.REJECTED
        )).thenReturn(0L);  // No previous rejections

        // Act
        boolean result = appealService.canUserAppeal(testUser.getId(), testPunishment.getId());

        // Assert
        assertThat(result).isTrue();
        verify(punishmentRepository, times(1)).findById(testPunishment.getId());
        verify(appealRepository, times(1)).countByPunishmentIdAndStatus(
                testPunishment.getId(),
                AppealStatus.REJECTED
        );
    }

    @Test
    void testCanUserAppeal_ExceededMaxAttempts() {
        // Arrange
        when(punishmentRepository.findById(testPunishment.getId()))
                .thenReturn(Optional.of(testPunishment));
        when(appealRepository.countByPunishmentIdAndStatus(
                testPunishment.getId(),
                AppealStatus.REJECTED
        )).thenReturn(2L);  // Max attempts exceeded (assuming max is 2)

        // Act
        boolean result = appealService.canUserAppeal(testUser.getId(), testPunishment.getId());

        // Assert
        assertThat(result).isFalse();
        verify(punishmentRepository, times(1)).findById(testPunishment.getId());
        verify(appealRepository, times(1)).countByPunishmentIdAndStatus(
                testPunishment.getId(),
                AppealStatus.REJECTED
        );
    }

    @Test
    void testCanUserAppeal_InactivePunishment() {
        // Arrange
        testPunishment.setActive(false);  // Punishment is no longer active

        when(punishmentRepository.findById(testPunishment.getId()))
                .thenReturn(Optional.of(testPunishment));

        // Act
        boolean result = appealService.canUserAppeal(testUser.getId(), testPunishment.getId());

        // Assert
        assertThat(result).isFalse();  // Can't appeal inactive punishment
        verify(punishmentRepository, times(1)).findById(testPunishment.getId());
    }
}
