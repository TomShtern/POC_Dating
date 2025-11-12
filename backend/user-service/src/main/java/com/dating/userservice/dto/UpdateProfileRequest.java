package com.dating.userservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update Profile Request DTO
 *
 * Request payload for updating user profile.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    private String firstName;

    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;

    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 120, message = "Age must be less than 120")
    private Integer age;

    private String gender;

    @Size(max = 500, message = "Bio must be less than 500 characters")
    private String bio;

    @Size(max = 512, message = "Photo URL must be less than 512 characters")
    private String photoUrl;

    @Size(max = 100, message = "Location must be less than 100 characters")
    private String location;

    private String preferences;
}
