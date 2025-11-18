package com.dating.ui.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    @NotBlank(message = "Reported user ID is required")
    private String reportedUserId;

    @NotBlank(message = "Report reason is required")
    @Size(max = 100, message = "Reason must not exceed 100 characters")
    private String reason;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}
