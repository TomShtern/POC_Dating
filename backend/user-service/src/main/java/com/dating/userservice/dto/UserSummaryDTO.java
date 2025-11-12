package com.dating.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * User Summary DTO
 *
 * Lightweight user information for other services.
 * Used for displaying basic user info in matches, chats, etc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryDTO {

    private UUID id;
    private String firstName;
    private String lastName;
    private Integer age;
    private String gender;
    private String photoUrl;
    private String location;
}
