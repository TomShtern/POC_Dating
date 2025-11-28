package com.dating.ui.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SwipeResponse {
    private boolean match;
    private String matchId;

    @Valid
    private User matchedUser;
}
