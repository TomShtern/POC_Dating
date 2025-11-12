package com.dating.recommendation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for updating user preferences.
 * Used in POST/PUT requests to update matching preferences.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePreferencesRequest {

    @NotNull(message = "Minimum age is required")
    @Min(value = 18, message = "Minimum age must be at least 18")
    @Max(value = 100, message = "Minimum age must not exceed 100")
    private Integer minAge;

    @NotNull(message = "Maximum age is required")
    @Min(value = 18, message = "Maximum age must be at least 18")
    @Max(value = 100, message = "Maximum age must not exceed 100")
    private Integer maxAge;

    @NotBlank(message = "Preferred gender is required")
    private String preferredGender;

    @NotNull(message = "Maximum distance is required")
    @Min(value = 1, message = "Maximum distance must be at least 1 km")
    @Max(value = 500, message = "Maximum distance must not exceed 500 km")
    private Integer maxDistance;

    private List<String> interests;

    private Boolean flexibleAgeRange;

    private Boolean flexibleDistance;
}
