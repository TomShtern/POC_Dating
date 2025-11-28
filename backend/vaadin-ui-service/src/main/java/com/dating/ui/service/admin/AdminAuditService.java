package com.dating.ui.service.admin;

import com.dating.ui.dto.admin.AdminAuditLogDTO;
import com.dating.ui.security.SecurityUtils;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Service for logging and retrieving admin audit logs
 * In production, this would persist to database via repository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuditService {

    // In-memory storage for POC - would use database in production
    private final Deque<AdminAuditLogDTO> auditLogs = new ConcurrentLinkedDeque<>();
    private static final int MAX_IN_MEMORY_LOGS = 10000;

    /**
     * Log an admin action
     */
    public void log(String action, String targetType, String targetId, Map<String, Object> details) {
        String adminId = SecurityUtils.getCurrentUserId();
        String adminName = SecurityUtils.getCurrentUserName();

        if (adminId == null) {
            log.warn("Attempted to log admin action without authenticated admin");
            return;
        }

        String ipAddress = getClientIpAddress();
        String userAgent = getUserAgent();

        AdminAuditLogDTO auditLog = AdminAuditLogDTO.builder()
                .id(UUID.randomUUID().toString())
                .adminId(adminId)
                .adminName(adminName != null ? adminName : "Unknown")
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details != null ? details : new HashMap<>())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(LocalDateTime.now())
                .build();

        // Add to front of deque for reverse chronological order
        auditLogs.addFirst(auditLog);

        // Trim old logs if exceeding max
        while (auditLogs.size() > MAX_IN_MEMORY_LOGS) {
            auditLogs.removeLast();
        }

        log.info("Admin action logged: {} by {} on {} {}",
                action, adminName, targetType, targetId);
    }

    /**
     * Log action with simple string details
     */
    public void log(String action, String targetType, String targetId, String details) {
        Map<String, Object> detailsMap = new HashMap<>();
        detailsMap.put("message", details);
        log(action, targetType, targetId, detailsMap);
    }

    /**
     * Get all audit logs (paginated)
     */
    public List<AdminAuditLogDTO> getAuditLogs(int offset, int limit) {
        return auditLogs.stream()
                .skip(offset)
                .limit(limit)
                .toList();
    }

    /**
     * Get audit logs for a specific admin
     */
    public List<AdminAuditLogDTO> getAuditLogsByAdmin(String adminId, int limit) {
        return auditLogs.stream()
                .filter(log -> log.getAdminId().equals(adminId))
                .limit(limit)
                .toList();
    }

    /**
     * Get audit logs for a specific target
     */
    public List<AdminAuditLogDTO> getAuditLogsByTarget(String targetType, String targetId, int limit) {
        return auditLogs.stream()
                .filter(log -> targetType.equals(log.getTargetType())
                        && targetId.equals(log.getTargetId()))
                .limit(limit)
                .toList();
    }

    /**
     * Get audit logs by action type
     */
    public List<AdminAuditLogDTO> getAuditLogsByAction(String action, int limit) {
        return auditLogs.stream()
                .filter(log -> action.equals(log.getAction()))
                .limit(limit)
                .toList();
    }

    /**
     * Get recent audit logs
     */
    public List<AdminAuditLogDTO> getRecentLogs(int limit) {
        return auditLogs.stream()
                .limit(limit)
                .toList();
    }

    /**
     * Get total count of audit logs
     */
    public int getTotalCount() {
        return auditLogs.size();
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress() {
        try {
            VaadinRequest request = VaadinService.getCurrentRequest();
            if (request != null) {
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not get client IP address", e);
        }
        return "unknown";
    }

    /**
     * Get user agent from request
     */
    private String getUserAgent() {
        try {
            VaadinRequest request = VaadinService.getCurrentRequest();
            if (request != null) {
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.debug("Could not get user agent", e);
        }
        return "unknown";
    }
}
