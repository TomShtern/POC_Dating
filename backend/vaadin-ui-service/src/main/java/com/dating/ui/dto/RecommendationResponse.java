package com.dating.ui.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for a single recommendation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {
    private UUID userId;
    private User user;
    private int score;
    private String reason;
}
