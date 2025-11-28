package com.dating.ui.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedUser {
    @NotBlank(message = "Block record ID is required")
    private String id;

    @NotNull(message = "Blocked user information is required")
    @Valid
    private User blockedUser;

    @NotNull(message = "Blocked timestamp is required")
    private LocalDateTime blockedAt;
}
