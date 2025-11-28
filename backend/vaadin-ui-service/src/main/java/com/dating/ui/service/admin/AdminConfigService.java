package com.dating.ui.service.admin;

import com.dating.ui.dto.admin.AppConfigDTO;
import com.dating.ui.security.SecurityUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing application configuration
 * In production, this would persist to database
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminConfigService {

    private final AdminAuditService auditService;

    // In-memory storage for POC
    private final Map<String, AppConfigDTO> configurations = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        initializeDefaultConfig();
    }

    /**
     * Get all configurations
     */
    public List<AppConfigDTO> getAllConfigurations() {
        return new ArrayList<>(configurations.values());
    }

    /**
     * Get configurations by category
     */
    public List<AppConfigDTO> getConfigurationsByCategory(String category) {
        return configurations.values().stream()
                .filter(config -> category.equals(config.getCategory()))
                .collect(Collectors.toList());
    }

    /**
     * Get all categories
     */
    public List<String> getAllCategories() {
        return configurations.values().stream()
                .map(AppConfigDTO::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get single configuration value
     */
    public Optional<AppConfigDTO> getConfiguration(String key) {
        return Optional.ofNullable(configurations.get(key));
    }

    /**
     * Get configuration value as String
     */
    public String getStringValue(String key, String defaultValue) {
        AppConfigDTO config = configurations.get(key);
        return config != null ? config.getValue() : defaultValue;
    }

    /**
     * Get configuration value as Integer
     */
    public int getIntValue(String key, int defaultValue) {
        AppConfigDTO config = configurations.get(key);
        if (config != null) {
            try {
                return Integer.parseInt(config.getValue());
            } catch (NumberFormatException e) {
                log.warn("Invalid integer value for config {}: {}", key, config.getValue());
            }
        }
        return defaultValue;
    }

    /**
     * Get configuration value as Boolean
     */
    public boolean getBooleanValue(String key, boolean defaultValue) {
        AppConfigDTO config = configurations.get(key);
        return config != null ? Boolean.parseBoolean(config.getValue()) : defaultValue;
    }

    /**
     * Get configuration value as Double
     */
    public double getDoubleValue(String key, double defaultValue) {
        AppConfigDTO config = configurations.get(key);
        if (config != null) {
            try {
                return Double.parseDouble(config.getValue());
            } catch (NumberFormatException e) {
                log.warn("Invalid double value for config {}: {}", key, config.getValue());
            }
        }
        return defaultValue;
    }

    /**
     * Update configuration value
     */
    public AppConfigDTO updateConfiguration(String key, String newValue) {
        AppConfigDTO config = configurations.get(key);
        if (config == null) {
            throw new IllegalArgumentException("Configuration not found: " + key);
        }

        String oldValue = config.getValue();
        config.setValue(newValue);
        config.setUpdatedBy(SecurityUtils.getCurrentUserId());
        config.setUpdatedAt(LocalDateTime.now());

        // Log the change
        Map<String, Object> details = new HashMap<>();
        details.put("oldValue", oldValue);
        details.put("newValue", newValue);
        auditService.log("CONFIG_UPDATED", "CONFIG", key, details);

        log.info("Configuration {} updated from '{}' to '{}' by admin {}",
                key, oldValue, newValue, SecurityUtils.getCurrentUserId());

        return config;
    }

    /**
     * Create new configuration
     */
    public AppConfigDTO createConfiguration(AppConfigDTO config) {
        if (configurations.containsKey(config.getKey())) {
            throw new IllegalArgumentException("Configuration already exists: " + config.getKey());
        }

        config.setUpdatedBy(SecurityUtils.getCurrentUserId());
        config.setUpdatedAt(LocalDateTime.now());
        configurations.put(config.getKey(), config);

        auditService.log("CONFIG_CREATED", "CONFIG", config.getKey(),
                "New configuration created: " + config.getDescription());

        return config;
    }

    /**
     * Delete configuration
     */
    public void deleteConfiguration(String key) {
        AppConfigDTO removed = configurations.remove(key);
        if (removed != null) {
            auditService.log("CONFIG_DELETED", "CONFIG", key,
                    "Configuration deleted: " + removed.getDescription());
        }
    }

    /**
     * Get configuration audit log
     */
    public List<com.dating.ui.dto.admin.AdminAuditLogDTO> getConfigurationAuditLog(int limit) {
        return auditService.getAuditLogsByTarget("CONFIG", null, limit).stream()
                .filter(log -> log.getTargetType() != null && log.getTargetType().equals("CONFIG"))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private void initializeDefaultConfig() {
        // Matching algorithm weights
        addConfig("match.algorithm.weight.interests", "40", "INTEGER", "matching",
                "Weight for interest matching (0-100)", false);
        addConfig("match.algorithm.weight.age", "30", "INTEGER", "matching",
                "Weight for age preference matching (0-100)", false);
        addConfig("match.algorithm.weight.distance", "30", "INTEGER", "matching",
                "Weight for distance preference matching (0-100)", false);

        // Rate limits
        addConfig("rate_limit.swipes_per_day", "100", "INTEGER", "rate_limits",
                "Maximum swipes per user per day", false);
        addConfig("rate_limit.super_likes_per_day", "5", "INTEGER", "rate_limits",
                "Maximum super likes per user per day", false);
        addConfig("rate_limit.messages_per_hour", "50", "INTEGER", "rate_limits",
                "Maximum messages per user per hour", false);

        // Feature flags
        addConfig("feature.super_like_enabled", "true", "BOOLEAN", "features",
                "Enable/disable super like feature", false);
        addConfig("feature.video_profiles_enabled", "false", "BOOLEAN", "features",
                "Enable/disable video profile uploads", false);
        addConfig("feature.read_receipts_enabled", "true", "BOOLEAN", "features",
                "Enable/disable message read receipts", false);

        // Security settings
        addConfig("security.max_login_attempts", "5", "INTEGER", "security",
                "Maximum login attempts before lockout", false);
        addConfig("security.lockout_duration_minutes", "30", "INTEGER", "security",
                "Account lockout duration in minutes", false);
        addConfig("security.jwt_expiry_minutes", "15", "INTEGER", "security",
                "JWT token expiration in minutes", true);

        // Notification settings
        addConfig("notification.email_enabled", "true", "BOOLEAN", "notifications",
                "Enable email notifications", false);
        addConfig("notification.push_enabled", "true", "BOOLEAN", "notifications",
                "Enable push notifications", false);
    }

    private void addConfig(String key, String value, String valueType, String category,
                           String description, boolean sensitive) {
        configurations.put(key, AppConfigDTO.builder()
                .key(key)
                .value(value)
                .valueType(valueType)
                .category(category)
                .description(description)
                .sensitive(sensitive)
                .updatedAt(LocalDateTime.now())
                .build());
    }
}
