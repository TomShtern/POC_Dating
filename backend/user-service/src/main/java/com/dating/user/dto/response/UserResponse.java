package com.dating.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for user profile data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private UUID id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Integer age;
    private String gender;
    private String bio;
    private String profilePictureUrl;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLogin;
}
