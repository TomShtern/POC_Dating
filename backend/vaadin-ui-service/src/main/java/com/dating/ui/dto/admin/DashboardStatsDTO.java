package com.dating.ui.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for dashboard statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalUsers;
    private long activeToday;
    private long newThisWeek;
    private long matchesToday;
    private long messagesToday;
    private long pendingReports;
    private double cacheHitRate;
    private int activeServices;
    private int totalServices;
}
