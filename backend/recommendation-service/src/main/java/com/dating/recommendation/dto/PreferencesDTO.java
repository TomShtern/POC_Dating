package com.dating.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing user preferences for matching.
 * Used for API responses when retrieving user preferences.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferencesDTO {
    private Long id;
    private Long userId;
    private Integer minAge;
    private Integer maxAge;
    private String preferredGender;
    private Integer maxDistance;
    private List<String> interests;
    private Boolean flexibleAgeRange;
    private Boolean flexibleDistance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
