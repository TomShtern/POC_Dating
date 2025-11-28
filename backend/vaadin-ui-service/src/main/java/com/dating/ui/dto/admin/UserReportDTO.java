package com.dating.ui.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user reports
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReportDTO {
    private String id;
    private String reporterId;
    private String reporterName;
    private String reportedUserId;
    private String reportedUserName;
    private String reason;
    private String description;
    private String status;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String actionTaken;
    private String adminNotes;
    private LocalDateTime createdAt;
}
