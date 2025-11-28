package com.dating.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for authentication operations (login, register, refresh).
 * Flat structure matching API specification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private UUID userId;
    private String email;
    private String username;
    private String token;
    private String refreshToken;
    private Long expiresIn;

    /**
     * Create an AuthResponse with user details and tokens.
     */
    public static AuthResponse of(UUID userId, String email, String username,
                                   String token, String refreshToken, Long expiresIn) {
        return AuthResponse.builder()
                .userId(userId)
                .email(email)
                .username(username)
                .token(token)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .build();
    }
}
