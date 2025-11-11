package com.dating.common.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Entity
 *
 * PURPOSE: JPA entity representing a user in the dating application
 *
 * FIELDS TO IMPLEMENT:
 * - Core identity: id, email, username, passwordHash
 * - Profile data: firstName, lastName, dateOfBirth, gender, bio, profilePictureUrl
 * - Status tracking: status, createdAt, updatedAt, lastLogin
 *
 * VALIDATIONS TO ADD:
 * - Email format validation
 * - Age >= 18
 * - Username length constraints
 * - Password hash size (BCrypt = 60 chars)
 *
 * RELATIONSHIPS:
 * - OneToOne with UserPreferences
 * - OneToMany with Swipes (both directions: outgoing, incoming)
 * - OneToMany with Messages (as sender)
 * - ManyToMany with Matches (via matches table)
 *
 * NOTE: DO NOT include password in tostring() or equals()
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_username", columnList = "username"),
    @Index(name = "idx_users_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    // TODO: Implement all fields from database schema
    // TODO: Add JPA annotations (@Column, @GeneratedValue, etc.)
    // TODO: Add validation annotations
    // TODO: Define relationships with other entities
}
