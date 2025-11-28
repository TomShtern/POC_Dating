package com.dating.recommendation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO representing a recommended user's basic profile information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedUserResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * User ID.
     */
    private UUID id;

    /**
     * User's display name.
     */
    private String name;

    /**
     * User's age.
     */
    private int age;

    /**
     * Profile picture URL.
     */
    private String profilePictureUrl;

    /**
     * User's bio.
     */
    private String bio;
}
