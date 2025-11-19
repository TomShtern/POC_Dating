package com.dating.ui.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for application configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfigDTO {
    private String key;
    private String value;
    private String valueType;
    private String category;
    private String description;
    private boolean sensitive;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
