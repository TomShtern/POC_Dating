package com.dating.ui.dto;

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
    private String id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private Integer age;
    private String gender;
    private String bio;
    private String photoUrl;
    private String city;
    private String country;

    // Preferences
    private Integer minAge;
    private Integer maxAge;
    private String interestedInGender;
    private Integer maxDistance;
}
