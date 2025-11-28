package com.dating.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for date and time operations.
 * Provides common date/time calculations and formatting
 * used across all microservices.
 */
public final class DateTimeUtil {

    private DateTimeUtil() {
        // Prevent instantiation
    }

    // ===========================================
    // DATE FORMATTERS
    // ===========================================

    /**
     * ISO date format (yyyy-MM-dd).
     */
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * ISO date-time format (yyyy-MM-dd'T'HH:mm:ss).
     */
    public static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Display date format (MMM d, yyyy).
     */
    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");

    /**
     * Display time format (h:mm a).
     */
    public static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("h:mm a");

    /**
     * Display date-time format (MMM d, yyyy h:mm a).
     */
    public static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    // ===========================================
    // AGE CALCULATIONS
    // ===========================================

    /**
     * Calculate age from date of birth.
     *
     * @param dateOfBirth Date of birth
     * @return Age in years
     * @throws IllegalArgumentException if dateOfBirth is null or in the future
     */
    public static int calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth cannot be null");
        }
        LocalDate today = LocalDate.now();
        if (dateOfBirth.isAfter(today)) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }
        return Period.between(dateOfBirth, today).getYears();
    }

    /**
     * Check if user is at least the minimum age.
     *
     * @param dateOfBirth Date of birth
     * @param minimumAge Minimum age required
     * @return true if user meets minimum age requirement
     */
    public static boolean isMinimumAge(LocalDate dateOfBirth, int minimumAge) {
        return calculateAge(dateOfBirth) >= minimumAge;
    }

    /**
     * Check if user is within an age range.
     *
     * @param dateOfBirth Date of birth
     * @param minAge Minimum age
     * @param maxAge Maximum age
     * @return true if age is within range (inclusive)
     */
    public static boolean isAgeInRange(LocalDate dateOfBirth, int minAge, int maxAge) {
        int age = calculateAge(dateOfBirth);
        return age >= minAge && age <= maxAge;
    }

    // ===========================================
    // TIME FORMATTING
    // ===========================================

    /**
     * Format instant for display as relative time (e.g., "5 minutes ago").
     *
     * @param instant Instant to format
     * @return Relative time string
     */
    public static String formatRelativeTime(Instant instant) {
        if (instant == null) {
            return "";
        }

        Instant now = Instant.now();
        long seconds = ChronoUnit.SECONDS.between(instant, now);

        if (seconds < 0) {
            return "just now";
        } else if (seconds < 60) {
            return "just now";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (seconds < 604800) {
            long days = seconds / 86400;
            return days + (days == 1 ? " day ago" : " days ago");
        } else if (seconds < 2592000) {
            long weeks = seconds / 604800;
            return weeks + (weeks == 1 ? " week ago" : " weeks ago");
        } else {
            LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
            return date.format(DISPLAY_DATE);
        }
    }

    /**
     * Format instant as display date-time.
     *
     * @param instant Instant to format
     * @return Formatted date-time string
     */
    public static String formatDateTime(Instant instant) {
        if (instant == null) {
            return "";
        }
        return instant.atZone(ZoneId.systemDefault())
                .format(DISPLAY_DATE_TIME);
    }

    /**
     * Format instant as display date.
     *
     * @param instant Instant to format
     * @return Formatted date string
     */
    public static String formatDate(Instant instant) {
        if (instant == null) {
            return "";
        }
        return instant.atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DISPLAY_DATE);
    }

    /**
     * Format instant as display time.
     *
     * @param instant Instant to format
     * @return Formatted time string
     */
    public static String formatTime(Instant instant) {
        if (instant == null) {
            return "";
        }
        return instant.atZone(ZoneId.systemDefault())
                .format(DISPLAY_TIME);
    }

    // ===========================================
    // CONVERSIONS
    // ===========================================

    /**
     * Convert LocalDate to Instant (start of day UTC).
     *
     * @param localDate Local date
     * @return Instant at start of day UTC
     */
    public static Instant toInstant(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * Convert LocalDateTime to Instant (UTC).
     *
     * @param localDateTime Local date-time
     * @return Instant in UTC
     */
    public static Instant toInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    /**
     * Convert Instant to LocalDate in system timezone.
     *
     * @param instant Instant
     * @return LocalDate in system timezone
     */
    public static LocalDate toLocalDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * Convert Instant to LocalDateTime in system timezone.
     *
     * @param instant Instant
     * @return LocalDateTime in system timezone
     */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    // ===========================================
    // TIME CHECKS
    // ===========================================

    /**
     * Check if instant is within the last N hours.
     *
     * @param instant Instant to check
     * @param hours Number of hours
     * @return true if within time window
     */
    public static boolean isWithinHours(Instant instant, int hours) {
        if (instant == null) {
            return false;
        }
        return instant.isAfter(Instant.now().minus(hours, ChronoUnit.HOURS));
    }

    /**
     * Check if instant is within the last N days.
     *
     * @param instant Instant to check
     * @param days Number of days
     * @return true if within time window
     */
    public static boolean isWithinDays(Instant instant, int days) {
        if (instant == null) {
            return false;
        }
        return instant.isAfter(Instant.now().minus(days, ChronoUnit.DAYS));
    }

    /**
     * Check if date is today.
     *
     * @param instant Instant to check
     * @return true if today
     */
    public static boolean isToday(Instant instant) {
        if (instant == null) {
            return false;
        }
        LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        return date.equals(LocalDate.now());
    }

    /**
     * Get start of current day.
     *
     * @return Instant at start of today in system timezone
     */
    public static Instant startOfToday() {
        return LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
    }

    /**
     * Get start of current week (Monday).
     *
     * @return Instant at start of current week
     */
    public static Instant startOfWeek() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        return monday.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
