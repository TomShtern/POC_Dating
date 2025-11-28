package com.dating.ui.service.admin;

import com.dating.ui.dto.admin.DashboardStatsDTO;
import com.dating.ui.dto.admin.TimeSeriesDataPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Service for admin analytics and statistics
 * In production, this would aggregate data from all microservices
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAnalyticsService {

    private final Random random = new Random();

    /**
     * Get dashboard statistics
     */
    public DashboardStatsDTO getDashboardStats() {
        // Mock data for POC - would aggregate from real services
        return DashboardStatsDTO.builder()
                .totalUsers(15847)
                .activeToday(3421)
                .newThisWeek(523)
                .matchesToday(1247)
                .messagesToday(18543)
                .pendingReports(12)
                .cacheHitRate(94.7)
                .activeServices(5)
                .totalServices(5)
                .build();
    }

    /**
     * Get user registration data over time
     */
    public List<TimeSeriesDataPoint> getUserGrowthData(int days) {
        List<TimeSeriesDataPoint> data = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long value = 50 + random.nextInt(100); // Mock: 50-150 new users per day
            data.add(TimeSeriesDataPoint.builder()
                    .date(date)
                    .value(value)
                    .label(date.toString())
                    .build());
        }

        return data;
    }

    /**
     * Get daily active users over time
     */
    public List<TimeSeriesDataPoint> getDailyActiveUsersData(int days) {
        List<TimeSeriesDataPoint> data = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long value = 2000 + random.nextInt(2000); // Mock: 2000-4000 DAU
            data.add(TimeSeriesDataPoint.builder()
                    .date(date)
                    .value(value)
                    .label(date.toString())
                    .build());
        }

        return data;
    }

    /**
     * Get matches per day
     */
    public List<TimeSeriesDataPoint> getMatchesData(int days) {
        List<TimeSeriesDataPoint> data = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long value = 800 + random.nextInt(800); // Mock: 800-1600 matches per day
            data.add(TimeSeriesDataPoint.builder()
                    .date(date)
                    .value(value)
                    .label(date.toString())
                    .build());
        }

        return data;
    }

    /**
     * Get messages per day
     */
    public List<TimeSeriesDataPoint> getMessagesData(int days) {
        List<TimeSeriesDataPoint> data = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long value = 15000 + random.nextInt(10000); // Mock: 15000-25000 messages per day
            data.add(TimeSeriesDataPoint.builder()
                    .date(date)
                    .value(value)
                    .label(date.toString())
                    .build());
        }

        return data;
    }

    /**
     * Get engagement metrics
     */
    public EngagementMetrics getEngagementMetrics() {
        return new EngagementMetrics(
                15.3,  // Average swipes per user
                2.1,   // Average matches per user
                8.7,   // Average messages per match
                45.2,  // Average session duration (minutes)
                3.2    // Average sessions per day
        );
    }

    /**
     * Get retention data (mock cohort analysis)
     */
    public List<RetentionCohort> getRetentionCohorts(int weeks) {
        List<RetentionCohort> cohorts = new ArrayList<>();

        for (int week = 0; week < weeks; week++) {
            double[] retention = new double[Math.min(8, weeks - week)];
            retention[0] = 100.0;

            for (int i = 1; i < retention.length; i++) {
                // Simulated retention curve
                retention[i] = retention[i - 1] * (0.7 + random.nextDouble() * 0.15);
            }

            cohorts.add(new RetentionCohort(
                    LocalDate.now().minusWeeks(weeks - 1 - week),
                    500 + random.nextInt(200),
                    retention
            ));
        }

        return cohorts;
    }

    /**
     * Get geographic distribution
     */
    public List<GeoDistribution> getGeographicDistribution() {
        return List.of(
                new GeoDistribution("New York", 2847, 18.0),
                new GeoDistribution("Los Angeles", 2156, 13.6),
                new GeoDistribution("Chicago", 1523, 9.6),
                new GeoDistribution("Houston", 1247, 7.9),
                new GeoDistribution("Phoenix", 987, 6.2),
                new GeoDistribution("Philadelphia", 876, 5.5),
                new GeoDistribution("San Antonio", 765, 4.8),
                new GeoDistribution("San Diego", 654, 4.1),
                new GeoDistribution("Dallas", 543, 3.4),
                new GeoDistribution("Other", 4249, 26.8)
        );
    }

    /**
     * Export analytics to CSV format
     */
    public String exportAnalyticsToCsv(String reportType, int days) {
        StringBuilder csv = new StringBuilder();

        switch (reportType) {
            case "users" -> {
                csv.append("Date,New Users\n");
                getUserGrowthData(days).forEach(point ->
                        csv.append(point.getDate()).append(",").append(point.getValue()).append("\n"));
            }
            case "dau" -> {
                csv.append("Date,Daily Active Users\n");
                getDailyActiveUsersData(days).forEach(point ->
                        csv.append(point.getDate()).append(",").append(point.getValue()).append("\n"));
            }
            case "matches" -> {
                csv.append("Date,Matches\n");
                getMatchesData(days).forEach(point ->
                        csv.append(point.getDate()).append(",").append(point.getValue()).append("\n"));
            }
            case "messages" -> {
                csv.append("Date,Messages\n");
                getMessagesData(days).forEach(point ->
                        csv.append(point.getDate()).append(",").append(point.getValue()).append("\n"));
            }
            default -> csv.append("Invalid report type");
        }

        return csv.toString();
    }

    // Inner record classes for complex return types
    public record EngagementMetrics(
            double avgSwipesPerUser,
            double avgMatchesPerUser,
            double avgMessagesPerMatch,
            double avgSessionDuration,
            double avgSessionsPerDay
    ) {}

    public record RetentionCohort(
            LocalDate weekStart,
            int cohortSize,
            double[] weeklyRetention
    ) {}

    public record GeoDistribution(
            String city,
            int users,
            double percentage
    ) {}
}
