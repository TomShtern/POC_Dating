package com.dating.ui.service.admin;

import com.dating.ui.dto.admin.AppConfigDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AdminConfigService
 */
@ExtendWith(MockitoExtension.class)
class AdminConfigServiceTest {

    @Mock
    private AdminAuditService auditService;

    private AdminConfigService configService;

    @BeforeEach
    void setUp() {
        configService = new AdminConfigService(auditService);
    }

    @Test
    void testGetAllConfigurations_ReturnsDefaultConfigs() {
        List<AppConfigDTO> configs = configService.getAllConfigurations();

        assertNotNull(configs);
        assertFalse(configs.isEmpty());
    }

    @Test
    void testGetConfigurationsByCategory_ReturnsFilteredConfigs() {
        List<AppConfigDTO> configs = configService.getConfigurationsByCategory("matching");

        assertNotNull(configs);
        configs.forEach(config -> assertEquals("matching", config.getCategory()));
    }

    @Test
    void testGetAllCategories_ReturnsDistinctCategories() {
        List<String> categories = configService.getAllCategories();

        assertNotNull(categories);
        assertFalse(categories.isEmpty());

        // Check for expected categories
        assertTrue(categories.contains("matching"));
        assertTrue(categories.contains("rate_limits"));
        assertTrue(categories.contains("features"));
        assertTrue(categories.contains("security"));
    }

    @Test
    void testGetConfiguration_ExistingKey_ReturnsConfig() {
        Optional<AppConfigDTO> config = configService.getConfiguration("rate_limit.swipes_per_day");

        assertTrue(config.isPresent());
        assertEquals("rate_limit.swipes_per_day", config.get().getKey());
    }

    @Test
    void testGetConfiguration_NonExistingKey_ReturnsEmpty() {
        Optional<AppConfigDTO> config = configService.getConfiguration("non.existing.key");

        assertTrue(config.isEmpty());
    }

    @Test
    void testGetStringValue_ExistingKey_ReturnsValue() {
        String value = configService.getStringValue("feature.super_like_enabled", "false");

        assertEquals("true", value);
    }

    @Test
    void testGetStringValue_NonExistingKey_ReturnsDefault() {
        String value = configService.getStringValue("non.existing", "default");

        assertEquals("default", value);
    }

    @Test
    void testGetIntValue_ExistingKey_ReturnsValue() {
        int value = configService.getIntValue("rate_limit.swipes_per_day", 0);

        assertEquals(100, value);
    }

    @Test
    void testGetIntValue_NonExistingKey_ReturnsDefault() {
        int value = configService.getIntValue("non.existing", 42);

        assertEquals(42, value);
    }

    @Test
    void testGetBooleanValue_ExistingKey_ReturnsValue() {
        boolean value = configService.getBooleanValue("feature.super_like_enabled", false);

        assertTrue(value);
    }

    @Test
    void testGetBooleanValue_NonExistingKey_ReturnsDefault() {
        boolean value = configService.getBooleanValue("non.existing", true);

        assertTrue(value);
    }

    @Test
    void testUpdateConfiguration_ExistingKey_UpdatesValue() {
        String key = "rate_limit.swipes_per_day";
        String newValue = "200";

        AppConfigDTO updated = configService.updateConfiguration(key, newValue);

        assertNotNull(updated);
        assertEquals(newValue, updated.getValue());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    void testUpdateConfiguration_NonExistingKey_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                configService.updateConfiguration("non.existing", "value")
        );
    }

    @Test
    void testCreateConfiguration_NewKey_CreatesConfig() {
        AppConfigDTO newConfig = AppConfigDTO.builder()
                .key("test.new.config")
                .value("test_value")
                .valueType("STRING")
                .category("test")
                .description("Test configuration")
                .sensitive(false)
                .build();

        AppConfigDTO created = configService.createConfiguration(newConfig);

        assertNotNull(created);
        assertEquals("test.new.config", created.getKey());

        // Verify it can be retrieved
        Optional<AppConfigDTO> retrieved = configService.getConfiguration("test.new.config");
        assertTrue(retrieved.isPresent());
    }

    @Test
    void testCreateConfiguration_ExistingKey_ThrowsException() {
        AppConfigDTO duplicate = AppConfigDTO.builder()
                .key("rate_limit.swipes_per_day")
                .value("999")
                .valueType("INTEGER")
                .category("rate_limits")
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                configService.createConfiguration(duplicate)
        );
    }

    @Test
    void testDeleteConfiguration_ExistingKey_RemovesConfig() {
        // Create a config to delete
        AppConfigDTO toDelete = AppConfigDTO.builder()
                .key("test.to.delete")
                .value("value")
                .valueType("STRING")
                .category("test")
                .build();
        configService.createConfiguration(toDelete);

        // Delete it
        configService.deleteConfiguration("test.to.delete");

        // Verify it's gone
        Optional<AppConfigDTO> result = configService.getConfiguration("test.to.delete");
        assertTrue(result.isEmpty());
    }
}
