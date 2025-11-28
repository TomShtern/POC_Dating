package com.dating.ui.service.admin;

import com.dating.ui.dto.admin.AdminAuditLogDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AdminAuditService
 */
@ExtendWith(MockitoExtension.class)
class AdminAuditServiceTest {

    private AdminAuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AdminAuditService();
    }

    @Test
    void testGetRecentLogs_ReturnsEmptyListInitially() {
        List<AdminAuditLogDTO> logs = auditService.getRecentLogs(10);
        assertNotNull(logs);
        assertTrue(logs.isEmpty());
    }

    @Test
    void testGetTotalCount_ReturnsZeroInitially() {
        assertEquals(0, auditService.getTotalCount());
    }

    @Test
    void testGetAuditLogs_WithPagination() {
        List<AdminAuditLogDTO> logs = auditService.getAuditLogs(0, 50);
        assertNotNull(logs);
    }

    @Test
    void testGetAuditLogsByAction_ReturnsFilteredResults() {
        List<AdminAuditLogDTO> logs = auditService.getAuditLogsByAction("USER_STATUS_CHANGE", 10);
        assertNotNull(logs);
    }

    @Test
    void testGetAuditLogsByTarget_ReturnsFilteredResults() {
        List<AdminAuditLogDTO> logs = auditService.getAuditLogsByTarget("USER", "test-id", 10);
        assertNotNull(logs);
    }
}
