package com.dating.ui.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for admin audit log entries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLogDTO {
    private String id;
    private String adminId;
    private String adminName;
    private String action;
    private String targetType;
    private String targetId;
    private Map<String, Object> details;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}
