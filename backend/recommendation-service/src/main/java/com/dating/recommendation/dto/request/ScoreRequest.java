package com.dating.recommendation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for scoring profiles (internal API for match-service).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreRequest {

    /**
     * ID of the user requesting scores.
     */
    @NotNull(message = "User ID is required")
    private UUID userId;

    /**
     * List of candidate user IDs to score.
     */
    @NotNull(message = "Candidate IDs are required")
    private List<UUID> candidateIds;

    /**
     * Algorithm version to use (optional, defaults to v1).
     */
    private String algorithm;
}
