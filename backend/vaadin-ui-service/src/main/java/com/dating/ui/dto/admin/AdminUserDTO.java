package com.dating.ui.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for user data in admin views
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDTO {
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
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
    private List<String> roles;
    private boolean verified;
    private int reportCount;
    private int matchCount;
    private int messageCount;
}
