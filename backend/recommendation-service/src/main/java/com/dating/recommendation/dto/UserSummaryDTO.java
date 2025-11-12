package com.dating.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO representing a user profile summary from the user-service.
 * This is what we expect to receive from the UserServiceClient.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryDTO {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Integer age;
    private String gender;
    private String bio;

    // Profile information
    private List<String> photos;
    private List<String> interests;

    // Location information
    private Double latitude;
    private Double longitude;
    private String city;
    private String country;

    // Account status
    private Boolean active;
    private Boolean verified;
}
