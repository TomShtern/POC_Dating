package com.dating.user.service;

import com.dating.user.dto.CreateReportRequest;
import com.dating.user.dto.ReportDTO;
import com.dating.user.dto.ResolveReportRequest;
import com.dating.user.exception.DuplicateReportException;
import com.dating.user.exception.ReportNotFoundException;
import com.dating.user.exception.SelfReportException;
import com.dating.user.model.*;
import com.dating.user.repository.ReportRepository;
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
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModerationQueueService moderationQueueService;

    @Mock
    private PunishmentService punishmentService;

    @InjectMocks
    private ReportServiceImpl reportService;

    private User reporter;
    private User reportedUser;
    private Report testReport;
    private User moderator;

    @BeforeEach
    void setUp() {
        reporter = User.builder()
                .id(UUID.randomUUID())
                .username("reporter")
                .email("reporter@example.com")
                .status(UserStatus.ACTIVE)
                .build();

        reportedUser = User.builder()
                .id(UUID.randomUUID())
                .username("reported")
                .email("reported@example.com")
                .status(UserStatus.ACTIVE)
                .build();

        moderator = User.builder()
                .id(UUID.randomUUID())
                .username("moderator")
                .email("mod@example.com")
                .role(UserRole.MODERATOR)
                .build();

        testReport = Report.builder()
                .id(UUID.randomUUID())
                .reporterId(reporter.getId())
                .reportedUserId(reportedUser.getId())
                .reportType(ReportType.HARASSMENT)
                .description("User sent inappropriate messages")
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateReport_Success() {
        // Arrange
        CreateReportRequest request = CreateReportRequest.builder()
                .reportedUserId(reportedUser.getId())
                .reportType(ReportType.HARASSMENT)
                .description("User sent inappropriate messages")
                .contentId(UUID.randomUUID())
                .build();

        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(userRepository.findById(reportedUser.getId())).thenReturn(Optional.of(reportedUser));
        when(reportRepository.existsByReporterIdAndReportedUserIdAndStatusIn(
                eq(reporter.getId()),
                eq(reportedUser.getId()),
                anyList()
        )).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenReturn(testReport);

        // Act
        ReportDTO result = reportService.createReport(reporter.getId(), request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getReportType()).isEqualTo(ReportType.HARASSMENT);

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository, times(1)).save(captor.capture());

        Report saved = captor.getValue();
        assertThat(saved.getReporterId()).isEqualTo(reporter.getId());
        assertThat(saved.getReportedUserId()).isEqualTo(reportedUser.getId());
        assertThat(saved.getReportType()).isEqualTo(ReportType.HARASSMENT);
        assertThat(saved.getStatus()).isEqualTo(ReportStatus.PENDING);

        // Verify moderation queue was updated for severe reports
        if (request.getReportType() == ReportType.EXPLICIT_CONTENT ||
            request.getReportType() == ReportType.SCAM) {
            verify(moderationQueueService, times(1)).addToQueue(
                    any(),
                    any(),
                    any(),
                    anyString(),
                    any(),
                    any()
            );
        }
    }

    @Test
    void testCreateReport_Duplicate() {
        // Arrange
        CreateReportRequest request = CreateReportRequest.builder()
                .reportedUserId(reportedUser.getId())
                .reportType(ReportType.HARASSMENT)
                .description("User sent inappropriate messages")
                .build();

        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(userRepository.findById(reportedUser.getId())).thenReturn(Optional.of(reportedUser));
        when(reportRepository.existsByReporterIdAndReportedUserIdAndStatusIn(
                eq(reporter.getId()),
                eq(reportedUser.getId()),
                anyList()
        )).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> reportService.createReport(reporter.getId(), request))
                .isInstanceOf(DuplicateReportException.class)
                .hasMessageContaining("already have a pending report");

        verify(reportRepository, never()).save(any());
    }

    @Test
    void testCreateReport_SelfReport() {
        // Arrange
        CreateReportRequest request = CreateReportRequest.builder()
                .reportedUserId(reporter.getId())  // Same as reporter
                .reportType(ReportType.HARASSMENT)
                .description("Self report test")
                .build();

        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));

        // Act & Assert
        assertThatThrownBy(() -> reportService.createReport(reporter.getId(), request))
                .isInstanceOf(SelfReportException.class)
                .hasMessageContaining("cannot report yourself");

        verify(reportRepository, never()).save(any());
    }

    @Test
    void testGetReports_WithFilters() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Report> reports = Arrays.asList(testReport);
        Page<Report> page = new PageImpl<>(reports, pageable, 1);

        when(reportRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // Act
        Page<ReportDTO> result = reportService.getReports(
                ReportStatus.PENDING,
                ReportType.HARASSMENT,
                reportedUser.getId(),
                null,
                pageable
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(testReport.getId());
        assertThat(result.getContent().get(0).getReportType()).isEqualTo(ReportType.HARASSMENT);
        verify(reportRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void testResolveReport() {
        // Arrange
        ResolveReportRequest request = ResolveReportRequest.builder()
                .action(ReportAction.WARN_USER)
                .moderatorNotes("Warning issued for inappropriate behavior")
                .punishmentDuration(null)
                .build();

        when(reportRepository.findById(testReport.getId())).thenReturn(Optional.of(testReport));
        when(reportRepository.save(any(Report.class))).thenReturn(testReport);

        // Act
        ReportDTO result = reportService.resolveReport(
                testReport.getId(),
                moderator.getId(),
                request
        );

        // Assert
        assertThat(result).isNotNull();

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository, times(1)).save(captor.capture());

        Report resolved = captor.getValue();
        assertThat(resolved.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(resolved.getAction()).isEqualTo(ReportAction.WARN_USER);
        assertThat(resolved.getModeratorId()).isEqualTo(moderator.getId());
        assertThat(resolved.getModeratorNotes()).isEqualTo("Warning issued for inappropriate behavior");
        assertThat(resolved.getResolvedAt()).isNotNull();

        // Verify punishment was applied
        verify(punishmentService, times(1)).warn(
                eq(reportedUser.getId()),
                anyString(),
                eq(moderator.getId())
        );
    }

    @Test
    void testDismissReport() {
        // Arrange
        String dismissReason = "Report unfounded after investigation";

        when(reportRepository.findById(testReport.getId())).thenReturn(Optional.of(testReport));
        when(reportRepository.save(any(Report.class))).thenReturn(testReport);

        // Act
        ReportDTO result = reportService.dismissReport(
                testReport.getId(),
                moderator.getId(),
                dismissReason
        );

        // Assert
        assertThat(result).isNotNull();

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository, times(1)).save(captor.capture());

        Report dismissed = captor.getValue();
        assertThat(dismissed.getStatus()).isEqualTo(ReportStatus.DISMISSED);
        assertThat(dismissed.getModeratorId()).isEqualTo(moderator.getId());
        assertThat(dismissed.getModeratorNotes()).isEqualTo(dismissReason);
        assertThat(dismissed.getResolvedAt()).isNotNull();

        // Verify no punishment was applied
        verify(punishmentService, never()).warn(any(), any(), any());
        verify(punishmentService, never()).mute(any(), any(), any(), any());
        verify(punishmentService, never()).suspend(any(), any(), any(), any());
        verify(punishmentService, never()).ban(any(), any(), any());
    }

    @Test
    void testGetReportCountAgainstUser() {
        // Arrange
        Long expectedCount = 5L;
        when(reportRepository.countByReportedUserIdAndStatus(
                reportedUser.getId(),
                ReportStatus.RESOLVED
        )).thenReturn(expectedCount);

        // Act
        Long result = reportService.getReportCountAgainstUser(reportedUser.getId());

        // Assert
        assertThat(result).isEqualTo(expectedCount);
        verify(reportRepository, times(1)).countByReportedUserIdAndStatus(
                reportedUser.getId(),
                ReportStatus.RESOLVED
        );
    }
}
