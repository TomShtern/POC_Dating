package com.dating.ui.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockRequest {
    @NotBlank(message = "Blocked user ID is required")
    private String blockedUserId;
}
