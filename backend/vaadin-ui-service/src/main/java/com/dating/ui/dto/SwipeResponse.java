package com.dating.ui.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SwipeResponse {
    private boolean match;
    private String matchId;
    private User matchedUser;
}
