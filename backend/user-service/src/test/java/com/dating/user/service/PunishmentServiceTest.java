package com.dating.user.service;

import com.dating.user.dto.PunishmentDTO;
import com.dating.user.exception.PunishmentNotFoundException;
import com.dating.user.model.*;
import com.dating.user.repository.PunishmentRepository;
import com.dating.user.repository.UserRepository;
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
class PunishmentServiceTest {

    @Mock
    private PunishmentRepository punishmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PunishmentServiceImpl punishmentService;

    private User testUser;
    private User moderator;
    private Punishment testPunishment;

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

        testPunishment = Punishment.builder()
                .id(UUID.randomUUID())
                .userId(testUser.getId())
                .punishmentType(PunishmentType.WARNING)
                .reason("Inappropriate content")
                .issuedById(moderator.getId())
                .issuedAt(LocalDateTime.now())
                .active(true)
                .build();
    }

    @Test
    void testWarn() {
        // Arrange
        String reason = "Inappropriate language in profile";
        when(punishmentRepository.save(any(Punishment.class))).thenReturn(testPunishment);

        // Act
        PunishmentDTO result = punishmentService.warn(testUser.getId(), reason, moderator.getId());

        // Assert
        assertThat(result).isNotNull();

        ArgumentCaptor<Punishment> captor = ArgumentCaptor.forClass(Punishment.class);
        verify(punishmentRepository, times(1)).save(captor.capture());

        Punishment saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(testUser.getId());
        assertThat(saved.getPunishmentType()).isEqualTo(PunishmentType.WARNING);
        assertThat(saved.getReason()).isEqualTo(reason);
        assertThat(saved.getIssuedById()).isEqualTo(moderator.getId());
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getExpiresAt()).isNull();  // Warnings don't expire

        // Verify user status was not changed
        verify(userRepository, never()).save(any());
    }

    @Test
    void testMute() {
        // Arrange
        String reason = "Spam messaging";
        Integer durationHours = 24;
        when(punishmentRepository.save(any(Punishment.class))).thenReturn(testPunishment);

        // Act
        PunishmentDTO result = punishmentService.mute(
                testUser.getId(),
                reason,
                durationHours,
                moderator.getId()
        );

        // Assert
        assertThat(result).isNotNull();

        ArgumentCaptor<Punishment> captor = ArgumentCaptor.forClass(Punishment.class);
        verify(punishmentRepository, times(1)).save(captor.capture());

        Punishment saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(testUser.getId());
        assertThat(saved.getPunishmentType()).isEqualTo(PunishmentType.MUTE);
        assertThat(saved.getReason()).isEqualTo(reason);
        assertThat(saved.getIssuedById()).isEqualTo(moderator.getId());
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getExpiresAt()).isNotNull();
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());

        // Verify user status was not changed (mute doesn't change status)
        verify(userRepository, never()).save(any());
    }

    @Test
    void testSuspend() {
        // Arrange
        String reason = "Multiple policy violations";
        Integer durationDays = 7;
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(punishmentRepository.save(any(Punishment.class))).thenReturn(testPunishment);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        PunishmentDTO result = punishmentService.suspend(
                testUser.getId(),
                reason,
                durationDays,
                moderator.getId()
        );

        // Assert
        assertThat(result).isNotNull();

        ArgumentCaptor<Punishment> captor = ArgumentCaptor.forClass(Punishment.class);
        verify(punishmentRepository, times(1)).save(captor.capture());

        Punishment saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(testUser.getId());
        assertThat(saved.getPunishmentType()).isEqualTo(PunishmentType.SUSPENSION);
        assertThat(saved.getReason()).isEqualTo(reason);
        assertThat(saved.getIssuedById()).isEqualTo(moderator.getId());
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getExpiresAt()).isNotNull();

        // Verify user status was changed to SUSPENDED
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }

    @Test
    void testBan() {
        // Arrange
        String reason = "Explicit content violation";
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(punishmentRepository.save(any(Punishment.class))).thenReturn(testPunishment);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        PunishmentDTO result = punishmentService.ban(testUser.getId(), reason, moderator.getId());

        // Assert
        assertThat(result).isNotNull();

        ArgumentCaptor<Punishment> captor = ArgumentCaptor.forClass(Punishment.class);
        verify(punishmentRepository, times(1)).save(captor.capture());

        Punishment saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(testUser.getId());
        assertThat(saved.getPunishmentType()).isEqualTo(PunishmentType.BAN);
        assertThat(saved.getReason()).isEqualTo(reason);
        assertThat(saved.getIssuedById()).isEqualTo(moderator.getId());
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getExpiresAt()).isNull();  // Bans are permanent

        // Verify user status was changed to BANNED
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.BANNED);
    }

    @Test
    void testRevoke() {
        // Arrange
        String reason = "Appeal approved";
        testUser.setStatus(UserStatus.SUSPENDED);

        when(punishmentRepository.findById(testPunishment.getId())).thenReturn(Optional.of(testPunishment));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(punishmentRepository.save(any(Punishment.class))).thenReturn(testPunishment);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        PunishmentDTO result = punishmentService.revoke(
                testPunishment.getId(),
                reason,
                moderator.getId()
        );

        // Assert
        assertThat(result).isNotNull();

        ArgumentCaptor<Punishment> captor = ArgumentCaptor.forClass(Punishment.class);
        verify(punishmentRepository, times(1)).save(captor.capture());

        Punishment revoked = captor.getValue();
        assertThat(revoked.isActive()).isFalse();
        assertThat(revoked.getRevokedAt()).isNotNull();
        assertThat(revoked.getRevokedById()).isEqualTo(moderator.getId());
        assertThat(revoked.getRevocationReason()).isEqualTo(reason);

        // Verify user status was restored to ACTIVE (if no other active punishments)
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void testGetActivePunishments() {
        // Arrange
        List<Punishment> punishments = Arrays.asList(testPunishment);
        when(punishmentRepository.findByUserIdAndActiveTrue(testUser.getId()))
                .thenReturn(punishments);

        // Act
        List<PunishmentDTO> result = punishmentService.getActivePunishments(testUser.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(testPunishment.getId());
        assertThat(result.get(0).getPunishmentType()).isEqualTo(PunishmentType.WARNING);
        verify(punishmentRepository, times(1)).findByUserIdAndActiveTrue(testUser.getId());
    }

    @Test
    void testIsBanned() {
        // Arrange
        Punishment ban = Punishment.builder()
                .id(UUID.randomUUID())
                .userId(testUser.getId())
                .punishmentType(PunishmentType.BAN)
                .active(true)
                .build();

        when(punishmentRepository.findFirstByUserIdAndPunishmentTypeAndActiveTrue(
                testUser.getId(),
                PunishmentType.BAN
        )).thenReturn(Optional.of(ban));

        // Act
        boolean result = punishmentService.isBanned(testUser.getId());

        // Assert
        assertThat(result).isTrue();
        verify(punishmentRepository, times(1))
                .findFirstByUserIdAndPunishmentTypeAndActiveTrue(
                        testUser.getId(),
                        PunishmentType.BAN
                );
    }

    @Test
    void testIsMuted() {
        // Arrange
        Punishment mute = Punishment.builder()
                .id(UUID.randomUUID())
                .userId(testUser.getId())
                .punishmentType(PunishmentType.MUTE)
                .active(true)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        when(punishmentRepository.findFirstByUserIdAndPunishmentTypeAndActiveTrueAndExpiresAtAfter(
                eq(testUser.getId()),
                eq(PunishmentType.MUTE),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(mute));

        // Act
        boolean result = punishmentService.isMuted(testUser.getId());

        // Assert
        assertThat(result).isTrue();
        verify(punishmentRepository, times(1))
                .findFirstByUserIdAndPunishmentTypeAndActiveTrueAndExpiresAtAfter(
                        eq(testUser.getId()),
                        eq(PunishmentType.MUTE),
                        any(LocalDateTime.class)
                );
    }

    @Test
    void testIsSuspended() {
        // Arrange
        Punishment suspension = Punishment.builder()
                .id(UUID.randomUUID())
                .userId(testUser.getId())
                .punishmentType(PunishmentType.SUSPENSION)
                .active(true)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(punishmentRepository.findFirstByUserIdAndPunishmentTypeAndActiveTrueAndExpiresAtAfter(
                eq(testUser.getId()),
                eq(PunishmentType.SUSPENSION),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(suspension));

        // Act
        boolean result = punishmentService.isSuspended(testUser.getId());

        // Assert
        assertThat(result).isTrue();
        verify(punishmentRepository, times(1))
                .findFirstByUserIdAndPunishmentTypeAndActiveTrueAndExpiresAtAfter(
                        eq(testUser.getId()),
                        eq(PunishmentType.SUSPENSION),
                        any(LocalDateTime.class)
                );
    }
}
