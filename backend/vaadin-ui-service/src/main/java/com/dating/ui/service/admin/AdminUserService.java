package com.dating.ui.service.admin;

import com.dating.ui.dto.admin.*;
import com.dating.ui.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for admin user management operations
 * In production, this would call backend microservices
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final AdminAuditService auditService;

    // In-memory mock data for POC - would use Feign client in production
    private final Map<String, AdminUserDTO> mockUsers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        initializeMockData();
    }

    /**
     * Search users with criteria
     */
    public List<AdminUserDTO> searchUsers(AdminUserSearchCriteria criteria, int offset, int limit) {
        return mockUsers.values().stream()
                .filter(user -> matchesCriteria(user, criteria))
                .sorted(getComparator(criteria))
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get total count matching criteria
     */
    public long countUsers(AdminUserSearchCriteria criteria) {
        return mockUsers.values().stream()
                .filter(user -> matchesCriteria(user, criteria))
                .count();
    }

    /**
     * Get user by ID with full details
     */
    public Optional<AdminUserDTO> getUserById(String userId) {
        return Optional.ofNullable(mockUsers.get(userId));
    }

    /**
     * Update user status (suspend, ban, activate)
     */
    public AdminUserDTO updateUserStatus(String userId, String newStatus, String reason) {
        AdminUserDTO user = mockUsers.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        String oldStatus = user.getStatus();
        user.setStatus(newStatus);
        user.setUpdatedAt(LocalDateTime.now());

        // Log the action
        Map<String, Object> details = new HashMap<>();
        details.put("oldStatus", oldStatus);
        details.put("newStatus", newStatus);
        details.put("reason", reason);
        auditService.log("USER_STATUS_CHANGE", "USER", userId, details);

        log.info("User {} status changed from {} to {} by admin {}",
                userId, oldStatus, newStatus, SecurityUtils.getCurrentUserId());

        return user;
    }

    /**
     * Bulk update user status
     */
    public int bulkUpdateStatus(List<String> userIds, String newStatus, String reason) {
        int updated = 0;
        for (String userId : userIds) {
            try {
                updateUserStatus(userId, newStatus, reason);
                updated++;
            } catch (Exception e) {
                log.error("Failed to update status for user {}: {}", userId, e.getMessage());
            }
        }
        return updated;
    }

    /**
     * Get user activity history
     */
    public List<AdminAuditLogDTO> getUserActivityHistory(String userId, int limit) {
        return auditService.getAuditLogsByTarget("USER", userId, limit);
    }

    /**
     * Get reports against a user
     */
    public List<UserReportDTO> getReportsAgainstUser(String userId) {
        // Mock implementation
        return new ArrayList<>();
    }

    /**
     * Delete user (soft delete)
     */
    public void deleteUser(String userId, String reason) {
        updateUserStatus(userId, "DELETED", reason);
        auditService.log("USER_DELETED", "USER", userId, reason);
    }

    /**
     * Verify user
     */
    public AdminUserDTO verifyUser(String userId) {
        AdminUserDTO user = mockUsers.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        user.setVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        auditService.log("USER_VERIFIED", "USER", userId, "User verified by admin");

        return user;
    }

    /**
     * Add role to user
     */
    public AdminUserDTO addUserRole(String userId, String role) {
        AdminUserDTO user = mockUsers.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        if (!user.getRoles().contains(role)) {
            user.getRoles().add(role);
            user.setUpdatedAt(LocalDateTime.now());

            Map<String, Object> details = new HashMap<>();
            details.put("role", role);
            auditService.log("ROLE_ADDED", "USER", userId, details);
        }

        return user;
    }

    /**
     * Remove role from user
     */
    public AdminUserDTO removeUserRole(String userId, String role) {
        AdminUserDTO user = mockUsers.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        if (user.getRoles().remove(role)) {
            user.setUpdatedAt(LocalDateTime.now());

            Map<String, Object> details = new HashMap<>();
            details.put("role", role);
            auditService.log("ROLE_REMOVED", "USER", userId, details);
        }

        return user;
    }

    private boolean matchesCriteria(AdminUserDTO user, AdminUserSearchCriteria criteria) {
        if (criteria == null) return true;

        // Search text (email, username, name)
        if (criteria.getSearchText() != null && !criteria.getSearchText().isEmpty()) {
            String search = criteria.getSearchText().toLowerCase();
            boolean matches = (user.getEmail() != null && user.getEmail().toLowerCase().contains(search))
                    || (user.getUsername() != null && user.getUsername().toLowerCase().contains(search))
                    || (user.getFirstName() != null && user.getFirstName().toLowerCase().contains(search))
                    || (user.getLastName() != null && user.getLastName().toLowerCase().contains(search));
            if (!matches) return false;
        }

        // Status filter
        if (criteria.getStatus() != null && !criteria.getStatus().isEmpty()) {
            if (!criteria.getStatus().equals(user.getStatus())) return false;
        }

        // Date filters
        if (criteria.getRegisteredAfter() != null) {
            if (user.getCreatedAt().toLocalDate().isBefore(criteria.getRegisteredAfter())) return false;
        }
        if (criteria.getRegisteredBefore() != null) {
            if (user.getCreatedAt().toLocalDate().isAfter(criteria.getRegisteredBefore())) return false;
        }

        // Verified filter
        if (criteria.getVerified() != null) {
            if (criteria.getVerified() != user.isVerified()) return false;
        }

        // Age filters
        if (criteria.getMinAge() != null && user.getAge() != null) {
            if (user.getAge() < criteria.getMinAge()) return false;
        }
        if (criteria.getMaxAge() != null && user.getAge() != null) {
            if (user.getAge() > criteria.getMaxAge()) return false;
        }

        // Gender filter
        if (criteria.getGender() != null && !criteria.getGender().isEmpty()) {
            if (!criteria.getGender().equals(user.getGender())) return false;
        }

        return true;
    }

    private Comparator<AdminUserDTO> getComparator(AdminUserSearchCriteria criteria) {
        String sortBy = criteria != null && criteria.getSortBy() != null
                ? criteria.getSortBy() : "createdAt";
        boolean ascending = criteria != null && criteria.isAscending();

        Comparator<AdminUserDTO> comparator = switch (sortBy) {
            case "email" -> Comparator.comparing(AdminUserDTO::getEmail,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "username" -> Comparator.comparing(AdminUserDTO::getUsername,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "firstName" -> Comparator.comparing(AdminUserDTO::getFirstName,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "status" -> Comparator.comparing(AdminUserDTO::getStatus,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "lastLogin" -> Comparator.comparing(AdminUserDTO::getLastLogin,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(AdminUserDTO::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        };

        return ascending ? comparator : comparator.reversed();
    }

    private void initializeMockData() {
        // Create mock users for demo
        for (int i = 1; i <= 100; i++) {
            String id = UUID.randomUUID().toString();
            AdminUserDTO user = AdminUserDTO.builder()
                    .id(id)
                    .email("user" + i + "@example.com")
                    .username("user" + i)
                    .firstName("First" + i)
                    .lastName("Last" + i)
                    .age(18 + (i % 50))
                    .gender(i % 2 == 0 ? "MALE" : "FEMALE")
                    .bio("Bio for user " + i)
                    .city("City " + (i % 10))
                    .country("Country")
                    .status(i % 10 == 0 ? "SUSPENDED" : "ACTIVE")
                    .createdAt(LocalDateTime.now().minusDays(i))
                    .updatedAt(LocalDateTime.now().minusDays(i % 7))
                    .lastLogin(LocalDateTime.now().minusHours(i % 48))
                    .roles(new ArrayList<>(List.of("ROLE_USER")))
                    .verified(i % 3 != 0)
                    .reportCount(i % 5)
                    .matchCount(i * 2)
                    .messageCount(i * 10)
                    .build();
            mockUsers.put(id, user);
        }

        // Add an admin user
        String adminId = "admin-" + UUID.randomUUID().toString();
        AdminUserDTO admin = AdminUserDTO.builder()
                .id(adminId)
                .email("admin@dating.com")
                .username("admin")
                .firstName("Admin")
                .lastName("User")
                .age(30)
                .gender("OTHER")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now().minusMonths(6))
                .updatedAt(LocalDateTime.now())
                .lastLogin(LocalDateTime.now())
                .roles(new ArrayList<>(List.of("ROLE_USER", "ROLE_ADMIN")))
                .verified(true)
                .reportCount(0)
                .matchCount(0)
                .messageCount(0)
                .build();
        mockUsers.put(adminId, admin);
    }
}
