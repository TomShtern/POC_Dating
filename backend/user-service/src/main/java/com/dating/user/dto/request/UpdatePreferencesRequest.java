package com.dating.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating user preferences.
 * All fields are optional for partial updates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePreferencesRequest {

    @Min(value = 18, message = "Minimum age must be at least 18")
    @Max(value = 150, message = "Minimum age must not exceed 150")
    private Integer minAge;

    @Min(value = 18, message = "Maximum age must be at least 18")
    @Max(value = 150, message = "Maximum age must not exceed 150")
    private Integer maxAge;

    @Min(value = 1, message = "Maximum distance must be at least 1 km")
    @Max(value = 500, message = "Maximum distance must not exceed 500 km")
    private Integer maxDistanceKm;

    @Pattern(regexp = "^(MALE|FEMALE|BOTH)$", message = "Interested in must be MALE, FEMALE, or BOTH")
    private String interestedIn;

    private List<String> interests;

    private Boolean notificationEnabled;
}
