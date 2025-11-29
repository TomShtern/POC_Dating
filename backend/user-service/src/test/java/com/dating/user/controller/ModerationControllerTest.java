package com.dating.user.controller;

import com.dating.user.dto.*;
import com.dating.user.model.*;
import com.dating.user.service.AppealService;
import com.dating.user.service.ModerationQueueService;
import com.dating.user.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModerationController.class)
class ModerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ModerationQueueService moderationQueueService;

    @MockBean
    private ReportService reportService;

    @MockBean
    private AppealService appealService;

    private UUID moderatorId;
    private UUID userId;
    private ModerationQueueItemDTO queueItem;
    private ReportDTO report;
    private AppealDTO appeal;

    @BeforeEach
    void setUp() {
        moderatorId = UUID.randomUUID();
        userId = UUID.randomUUID();

        queueItem = ModerationQueueItemDTO.builder()
                .id(UUID.randomUUID())
                .contentType(ContentType.PHOTO)
                .contentId(UUID.randomUUID())
                .userId(userId)
                .status(ModerationStatus.PENDING)
                .priority(ModerationPriority.MEDIUM)
                .flaggedReason("Inappropriate content")
                .createdAt(LocalDateTime.now())
                .build();

        report = ReportDTO.builder()
                .id(UUID.randomUUID())
                .reporterId(UUID.randomUUID())
                .reportedUserId(userId)
                .reportType(ReportType.HARASSMENT)
                .description("User sent inappropriate messages")
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        appeal = AppealDTO.builder()
                .id(UUID.randomUUID())
                .punishmentId(UUID.randomUUID())
                .userId(userId)
                .reason("I believe this was a misunderstanding")
                .status(AppealStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetQueue() throws Exception {
        // Arrange
        List<ModerationQueueItemDTO> items = Arrays.asList(queueItem);
        Page<ModerationQueueItemDTO> page = new PageImpl<>(items, PageRequest.of(0, 10), 1);

        when(moderationQueueService.getQueue(
                any(ModerationStatus.class),
                any(ContentType.class),
                any(ModerationPriority.class),
                any(),
                any()
        )).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/moderation/queue")
                        .param("status", "PENDING")
                        .param("contentType", "PHOTO")
                        .param("priority", "MEDIUM")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(queueItem.getId().toString())))
                .andExpect(jsonPath("$.content[0].status", is("PENDING")))
                .andExpect(jsonPath("$.content[0].contentType", is("PHOTO")))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(moderationQueueService, times(1)).getQueue(
                any(ModerationStatus.class),
                any(ContentType.class),
                any(ModerationPriority.class),
                any(),
                any()
        );
    }

    @Test
    void testGetQueueItem() throws Exception {
        // Arrange
        when(moderationQueueService.getItem(queueItem.getId())).thenReturn(queueItem);

        // Act & Assert
        mockMvc.perform(get("/api/moderation/queue/{id}", queueItem.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(queueItem.getId().toString())))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.contentType", is("PHOTO")))
                .andExpect(jsonPath("$.flaggedReason", is("Inappropriate content")));

        verify(moderationQueueService, times(1)).getItem(queueItem.getId());
    }

    @Test
    void testProcessDecision() throws Exception {
        // Arrange
        ModerationDecisionRequest request = ModerationDecisionRequest.builder()
                .decision(ModerationDecision.APPROVE)
                .moderatorNotes("Content is appropriate")
                .build();

        queueItem.setStatus(ModerationStatus.APPROVED);
        queueItem.setDecision(ModerationDecision.APPROVE);

        when(moderationQueueService.processDecision(
                eq(queueItem.getId()),
                eq(moderatorId),
                any(ModerationDecisionRequest.class)
        )).thenReturn(queueItem);

        // Act & Assert
        mockMvc.perform(post("/api/moderation/queue/{id}/decision", queueItem.getId())
                        .header("X-User-Id", moderatorId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(queueItem.getId().toString())))
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.decision", is("APPROVE")));

        verify(moderationQueueService, times(1)).processDecision(
                eq(queueItem.getId()),
                eq(moderatorId),
                any(ModerationDecisionRequest.class)
        );
    }

    @Test
    void testGetStatistics() throws Exception {
        // Arrange
        ModerationStatisticsDTO stats = ModerationStatisticsDTO.builder()
                .pendingCount(10L)
                .inReviewCount(5L)
                .approvedCount(100L)
                .rejectedCount(20L)
                .lowPriorityCount(3L)
                .mediumPriorityCount(7L)
                .highPriorityCount(5L)
                .averageReviewTimeMinutes(25.5)
                .build();

        when(moderationQueueService.getStatistics()).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/moderation/queue/statistics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCount", is(10)))
                .andExpect(jsonPath("$.inReviewCount", is(5)))
                .andExpect(jsonPath("$.approvedCount", is(100)))
                .andExpect(jsonPath("$.rejectedCount", is(20)))
                .andExpect(jsonPath("$.averageReviewTimeMinutes", is(25.5)));

        verify(moderationQueueService, times(1)).getStatistics();
    }

    @Test
    void testGetReports() throws Exception {
        // Arrange
        List<ReportDTO> reports = Arrays.asList(report);
        Page<ReportDTO> page = new PageImpl<>(reports, PageRequest.of(0, 10), 1);

        when(reportService.getReports(
                any(ReportStatus.class),
                any(ReportType.class),
                any(),
                any(),
                any()
        )).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/moderation/reports")
                        .param("status", "PENDING")
                        .param("reportType", "HARASSMENT")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(report.getId().toString())))
                .andExpect(jsonPath("$.content[0].reportType", is("HARASSMENT")))
                .andExpect(jsonPath("$.content[0].status", is("PENDING")))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(reportService, times(1)).getReports(
                any(ReportStatus.class),
                any(ReportType.class),
                any(),
                any(),
                any()
        );
    }

    @Test
    void testResolveReport() throws Exception {
        // Arrange
        ResolveReportRequest request = ResolveReportRequest.builder()
                .action(ReportAction.WARN_USER)
                .moderatorNotes("Warning issued for inappropriate behavior")
                .build();

        report.setStatus(ReportStatus.RESOLVED);
        report.setAction(ReportAction.WARN_USER);

        when(reportService.resolveReport(
                eq(report.getId()),
                eq(moderatorId),
                any(ResolveReportRequest.class)
        )).thenReturn(report);

        // Act & Assert
        mockMvc.perform(post("/api/moderation/reports/{id}/resolve", report.getId())
                        .header("X-User-Id", moderatorId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(report.getId().toString())))
                .andExpect(jsonPath("$.status", is("RESOLVED")))
                .andExpect(jsonPath("$.action", is("WARN_USER")));

        verify(reportService, times(1)).resolveReport(
                eq(report.getId()),
                eq(moderatorId),
                any(ResolveReportRequest.class)
        );
    }

    @Test
    void testGetAppeals() throws Exception {
        // Arrange
        List<AppealDTO> appeals = Arrays.asList(appeal);
        Page<AppealDTO> page = new PageImpl<>(appeals, PageRequest.of(0, 10), 1);

        when(appealService.getAppeals(
                any(AppealStatus.class),
                any(),
                any()
        )).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/moderation/appeals")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(appeal.getId().toString())))
                .andExpect(jsonPath("$.content[0].status", is("PENDING")))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(appealService, times(1)).getAppeals(
                any(AppealStatus.class),
                any(),
                any()
        );
    }

    @Test
    void testResolveAppeal_Uphold() throws Exception {
        // Arrange
        String moderatorNotes = "After review, the original decision stands";

        appeal.setStatus(AppealStatus.REJECTED);
        appeal.setModeratorNotes(moderatorNotes);

        when(appealService.upholdDecision(
                eq(appeal.getId()),
                eq(moderatorId),
                eq(moderatorNotes)
        )).thenReturn(appeal);

        // Act & Assert
        mockMvc.perform(post("/api/moderation/appeals/{id}/uphold", appeal.getId())
                        .header("X-User-Id", moderatorId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AppealDecisionRequest(moderatorNotes)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(appeal.getId().toString())))
                .andExpect(jsonPath("$.status", is("REJECTED")));

        verify(appealService, times(1)).upholdDecision(
                eq(appeal.getId()),
                eq(moderatorId),
                eq(moderatorNotes)
        );
    }

    @Test
    void testResolveAppeal_Overturn() throws Exception {
        // Arrange
        String moderatorNotes = "Upon further review, the punishment was unwarranted";

        appeal.setStatus(AppealStatus.APPROVED);
        appeal.setModeratorNotes(moderatorNotes);

        when(appealService.overturnDecision(
                eq(appeal.getId()),
                eq(moderatorId),
                eq(moderatorNotes)
        )).thenReturn(appeal);

        // Act & Assert
        mockMvc.perform(post("/api/moderation/appeals/{id}/overturn", appeal.getId())
                        .header("X-User-Id", moderatorId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AppealDecisionRequest(moderatorNotes)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(appeal.getId().toString())))
                .andExpect(jsonPath("$.status", is("APPROVED")));

        verify(appealService, times(1)).overturnDecision(
                eq(appeal.getId()),
                eq(moderatorId),
                eq(moderatorNotes)
        );
    }

    @Test
    void testProcessDecision_InvalidRequest() throws Exception {
        // Arrange
        ModerationDecisionRequest invalidRequest = ModerationDecisionRequest.builder()
                // Missing required decision field
                .moderatorNotes("Some notes")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/moderation/queue/{id}/decision", queueItem.getId())
                        .header("X-User-Id", moderatorId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(moderationQueueService, never()).processDecision(any(), any(), any());
    }

    // Helper class for appeal decision request
    static class AppealDecisionRequest {
        private String moderatorNotes;

        public AppealDecisionRequest(String moderatorNotes) {
            this.moderatorNotes = moderatorNotes;
        }

        public String getModeratorNotes() {
            return moderatorNotes;
        }

        public void setModeratorNotes(String moderatorNotes) {
            this.moderatorNotes = moderatorNotes;
        }
    }
}
