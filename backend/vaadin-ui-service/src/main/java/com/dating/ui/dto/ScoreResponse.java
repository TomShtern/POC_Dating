package com.dating.ui.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * DTO for compatibility score response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreResponse {
    private UUID userId;
    private UUID targetUserId;
    private int score;
    private Map<String, Integer> factors;
}
