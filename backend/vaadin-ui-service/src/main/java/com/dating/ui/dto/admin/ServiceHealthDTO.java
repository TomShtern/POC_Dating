package com.dating.ui.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for service health status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceHealthDTO {
    private String serviceName;
    private String status;
    private String url;
    private long responseTimeMs;
    private LocalDateTime lastChecked;
    private String version;
    private String errorMessage;
}
