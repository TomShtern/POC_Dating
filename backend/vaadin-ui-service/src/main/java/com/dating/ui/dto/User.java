package com.dating.ui.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User DTO - represents user profile data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @NotBlank(message = "User ID is required")
    private String id;

    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 120, message = "Age must not exceed 120")
    private Integer age;

    @Size(max = 20, message = "Gender must not exceed 20 characters")
    private String gender;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    private String photoUrl;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    // Interests and additional info
    private java.util.List<String> interests;
    private Boolean isVerified;
    private Boolean isOnline;
    private java.time.Instant lastActiveAt;

    // Matching preferences
    @Min(value = 18, message = "Minimum age preference must be at least 18")
    @Max(value = 120, message = "Minimum age preference must not exceed 120")
    private Integer minAge;

    @Min(value = 18, message = "Maximum age preference must be at least 18")
    @Max(value = 120, message = "Maximum age preference must not exceed 120")
    private Integer maxAge;

    @Size(max = 20, message = "Interested in gender must not exceed 20 characters")
    private String interestedInGender;

    @Min(value = 1, message = "Maximum distance must be at least 1 km")
    @Max(value = 500, message = "Maximum distance must not exceed 500 km")
    private Integer maxDistance;

    // Notification preferences
    private Boolean matchNotifications;
    private Boolean messageNotifications;
    private Boolean likeNotifications;
    private Boolean emailNotifications;

    // Privacy settings
    @Size(max = 50, message = "Profile visibility must not exceed 50 characters")
    private String profileVisibility;  // "Everyone", "Matches Only", "Hidden"
    private Boolean showOnlineStatus;
    private Boolean showLastActive;
    private Boolean showDistance;
    private Boolean readReceipts;
}
