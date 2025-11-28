package com.dating.ui.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Criteria for searching users in admin views
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserSearchCriteria {
    private String searchText;
    private String status;
    private LocalDate registeredAfter;
    private LocalDate registeredBefore;
    private Boolean verified;
    private String role;
    private Integer minAge;
    private Integer maxAge;
    private String gender;
    private String sortBy;
    private boolean ascending;
}
