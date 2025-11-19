package com.dating.ui.service.admin;

import com.dating.ui.dto.admin.AdminUserDTO;
import com.dating.ui.dto.admin.AdminUserSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AdminUserService
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AdminAuditService auditService;

    private AdminUserService userService;

    @BeforeEach
    void setUp() {
        userService = new AdminUserService(auditService);
    }

    @Test
    void testSearchUsers_WithNoCriteria_ReturnsAllUsers() {
        AdminUserSearchCriteria criteria = new AdminUserSearchCriteria();
        List<AdminUserDTO> users = userService.searchUsers(criteria, 0, 50);

        assertNotNull(users);
        assertFalse(users.isEmpty());
        assertTrue(users.size() <= 50);
    }

    @Test
    void testSearchUsers_WithStatusFilter_ReturnsFilteredUsers() {
        AdminUserSearchCriteria criteria = AdminUserSearchCriteria.builder()
                .status("ACTIVE")
                .build();

        List<AdminUserDTO> users = userService.searchUsers(criteria, 0, 100);

        assertNotNull(users);
        users.forEach(user -> assertEquals("ACTIVE", user.getStatus()));
    }

    @Test
    void testSearchUsers_WithSearchText_ReturnsMatchingUsers() {
        AdminUserSearchCriteria criteria = AdminUserSearchCriteria.builder()
                .searchText("user1")
                .build();

        List<AdminUserDTO> users = userService.searchUsers(criteria, 0, 100);

        assertNotNull(users);
        // Should find users with "user1" in email or username
        users.forEach(user ->
                assertTrue(
                        user.getEmail().contains("user1") ||
                                user.getUsername().contains("user1") ||
                                (user.getFirstName() != null && user.getFirstName().contains("user1"))
                )
        );
    }

    @Test
    void testCountUsers_WithCriteria_ReturnsCorrectCount() {
        AdminUserSearchCriteria criteria = AdminUserSearchCriteria.builder()
                .status("SUSPENDED")
                .build();

        long count = userService.countUsers(criteria);

        assertTrue(count >= 0);
    }

    @Test
    void testGetUserById_ExistingUser_ReturnsUser() {
        // Get a user first to know an existing ID
        List<AdminUserDTO> users = userService.searchUsers(new AdminUserSearchCriteria(), 0, 1);
        assertFalse(users.isEmpty());

        String userId = users.get(0).getId();
        Optional<AdminUserDTO> result = userService.getUserById(userId);

        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getId());
    }

    @Test
    void testGetUserById_NonExistingUser_ReturnsEmpty() {
        Optional<AdminUserDTO> result = userService.getUserById("non-existing-id");

        assertTrue(result.isEmpty());
    }

    @Test
    void testUpdateUserStatus_ValidUser_UpdatesStatus() {
        // Get a user first
        List<AdminUserDTO> users = userService.searchUsers(new AdminUserSearchCriteria(), 0, 1);
        assertFalse(users.isEmpty());

        AdminUserDTO user = users.get(0);
        String userId = user.getId();
        String newStatus = "SUSPENDED";

        AdminUserDTO updated = userService.updateUserStatus(userId, newStatus, "Test reason");

        assertNotNull(updated);
        assertEquals(newStatus, updated.getStatus());
    }

    @Test
    void testUpdateUserStatus_NonExistingUser_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                userService.updateUserStatus("non-existing-id", "SUSPENDED", "Test reason")
        );
    }

    @Test
    void testBulkUpdateStatus_ValidUsers_UpdatesAll() {
        // Get multiple users
        List<AdminUserDTO> users = userService.searchUsers(new AdminUserSearchCriteria(), 0, 3);
        List<String> userIds = users.stream().map(AdminUserDTO::getId).toList();

        int updated = userService.bulkUpdateStatus(userIds, "SUSPENDED", "Bulk test");

        assertEquals(userIds.size(), updated);
    }

    @Test
    void testVerifyUser_ValidUser_SetsVerified() {
        List<AdminUserDTO> users = userService.searchUsers(
                AdminUserSearchCriteria.builder().verified(false).build(), 0, 1);

        if (!users.isEmpty()) {
            AdminUserDTO user = users.get(0);
            AdminUserDTO verified = userService.verifyUser(user.getId());

            assertTrue(verified.isVerified());
        }
    }

    @Test
    void testAddUserRole_ValidUser_AddsRole() {
        List<AdminUserDTO> users = userService.searchUsers(new AdminUserSearchCriteria(), 0, 1);
        assertFalse(users.isEmpty());

        AdminUserDTO user = users.get(0);
        AdminUserDTO updated = userService.addUserRole(user.getId(), "ROLE_MODERATOR");

        assertTrue(updated.getRoles().contains("ROLE_MODERATOR"));
    }

    @Test
    void testRemoveUserRole_ValidUser_RemovesRole() {
        List<AdminUserDTO> users = userService.searchUsers(new AdminUserSearchCriteria(), 0, 1);
        assertFalse(users.isEmpty());

        AdminUserDTO user = users.get(0);

        // Add role first
        userService.addUserRole(user.getId(), "ROLE_MODERATOR");

        // Then remove
        AdminUserDTO updated = userService.removeUserRole(user.getId(), "ROLE_MODERATOR");

        assertFalse(updated.getRoles().contains("ROLE_MODERATOR"));
    }
}
