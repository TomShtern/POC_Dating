package com.dating.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for user preferences.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferencesResponse {

    private String id;
    private String userId;
    private Integer minAge;
    private Integer maxAge;
    private Integer maxDistanceKm;
    private String interestedIn;
    private List<String> interests;
    private Boolean notificationEnabled;
    private Instant createdAt;
    private Instant updatedAt;
}
