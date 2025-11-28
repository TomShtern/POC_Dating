package com.dating.ui.service.admin;

import com.dating.ui.dto.admin.DashboardStatsDTO;
import com.dating.ui.dto.admin.TimeSeriesDataPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AdminAnalyticsService
 */
class AdminAnalyticsServiceTest {

    private AdminAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AdminAnalyticsService();
    }

    @Test
    void testGetDashboardStats_ReturnsValidStats() {
        DashboardStatsDTO stats = analyticsService.getDashboardStats();

        assertNotNull(stats);
        assertTrue(stats.getTotalUsers() > 0);
        assertTrue(stats.getActiveToday() >= 0);
        assertTrue(stats.getNewThisWeek() >= 0);
        assertTrue(stats.getMatchesToday() >= 0);
        assertTrue(stats.getMessagesToday() >= 0);
        assertTrue(stats.getCacheHitRate() >= 0 && stats.getCacheHitRate() <= 100);
    }

    @Test
    void testGetUserGrowthData_ReturnsCorrectNumberOfDays() {
        int days = 30;
        List<TimeSeriesDataPoint> data = analyticsService.getUserGrowthData(days);

        assertNotNull(data);
        assertEquals(days, data.size());
    }

    @Test
    void testGetUserGrowthData_DataPointsHaveRequiredFields() {
        List<TimeSeriesDataPoint> data = analyticsService.getUserGrowthData(7);

        assertFalse(data.isEmpty());
        data.forEach(point -> {
            assertNotNull(point.getDate());
            assertTrue(point.getValue() >= 0);
        });
    }

    @Test
    void testGetDailyActiveUsersData_ReturnsData() {
        List<TimeSeriesDataPoint> data = analyticsService.getDailyActiveUsersData(14);

        assertNotNull(data);
        assertEquals(14, data.size());
    }

    @Test
    void testGetMatchesData_ReturnsData() {
        List<TimeSeriesDataPoint> data = analyticsService.getMatchesData(7);

        assertNotNull(data);
        assertEquals(7, data.size());
    }

    @Test
    void testGetMessagesData_ReturnsData() {
        List<TimeSeriesDataPoint> data = analyticsService.getMessagesData(7);

        assertNotNull(data);
        assertEquals(7, data.size());
    }

    @Test
    void testGetEngagementMetrics_ReturnsValidMetrics() {
        AdminAnalyticsService.EngagementMetrics metrics = analyticsService.getEngagementMetrics();

        assertNotNull(metrics);
        assertTrue(metrics.avgSwipesPerUser() >= 0);
        assertTrue(metrics.avgMatchesPerUser() >= 0);
        assertTrue(metrics.avgMessagesPerMatch() >= 0);
        assertTrue(metrics.avgSessionDuration() >= 0);
        assertTrue(metrics.avgSessionsPerDay() >= 0);
    }

    @Test
    void testGetRetentionCohorts_ReturnsCorrectNumberOfCohorts() {
        int weeks = 4;
        List<AdminAnalyticsService.RetentionCohort> cohorts = analyticsService.getRetentionCohorts(weeks);

        assertNotNull(cohorts);
        assertEquals(weeks, cohorts.size());
    }

    @Test
    void testGetRetentionCohorts_CohortHasValidRetentionData() {
        List<AdminAnalyticsService.RetentionCohort> cohorts = analyticsService.getRetentionCohorts(4);

        assertFalse(cohorts.isEmpty());
        cohorts.forEach(cohort -> {
            assertNotNull(cohort.weekStart());
            assertTrue(cohort.cohortSize() > 0);
            assertNotNull(cohort.weeklyRetention());
            assertEquals(100.0, cohort.weeklyRetention()[0]); // Week 0 is always 100%
        });
    }

    @Test
    void testGetGeographicDistribution_ReturnsData() {
        List<AdminAnalyticsService.GeoDistribution> distribution = analyticsService.getGeographicDistribution();

        assertNotNull(distribution);
        assertFalse(distribution.isEmpty());

        distribution.forEach(geo -> {
            assertNotNull(geo.city());
            assertTrue(geo.users() >= 0);
            assertTrue(geo.percentage() >= 0 && geo.percentage() <= 100);
        });
    }

    @Test
    void testExportAnalyticsToCsv_UsersReport_ReturnsCsvFormat() {
        String csv = analyticsService.exportAnalyticsToCsv("users", 7);

        assertNotNull(csv);
        assertTrue(csv.startsWith("Date,New Users"));
        assertTrue(csv.contains("\n"));
    }

    @Test
    void testExportAnalyticsToCsv_DauReport_ReturnsCsvFormat() {
        String csv = analyticsService.exportAnalyticsToCsv("dau", 7);

        assertNotNull(csv);
        assertTrue(csv.startsWith("Date,Daily Active Users"));
    }

    @Test
    void testExportAnalyticsToCsv_InvalidReport_ReturnsErrorMessage() {
        String csv = analyticsService.exportAnalyticsToCsv("invalid", 7);

        assertNotNull(csv);
        assertEquals("Invalid report type", csv);
    }
}
