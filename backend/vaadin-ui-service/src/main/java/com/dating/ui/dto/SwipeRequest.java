package com.dating.ui.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SwipeRequest {
    @NotBlank(message = "Target user ID is required")
    private String targetUserId;

    @NotNull(message = "Swipe type is required")
    private SwipeType swipeType;
}
