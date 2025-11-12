package com.dating.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Profile DTO
 *
 * Response payload containing complete user profile information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private Integer age;
    private String gender;
    private String bio;
    private String photoUrl;
    private String location;
    private String preferences;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
    private String status;
}
