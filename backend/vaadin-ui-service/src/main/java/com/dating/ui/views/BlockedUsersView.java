package com.dating.ui.views;

import com.dating.ui.dto.BlockedUser;
import com.dating.ui.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * BlockedUsersView - Manage blocked users
 * View and unblock previously blocked users
 */
@Route(value = "blocked-users", layout = MainLayout.class)
@PageTitle("Blocked Users | POC Dating")
@PermitAll
@Slf4j
public class BlockedUsersView extends VerticalLayout {

    private final UserService userService;
    private Grid<BlockedUser> blockedUsersGrid;

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMM d, yyyy");

    public BlockedUsersView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setPadding(true);

        createUI();
        loadBlockedUsers();
    }

    private void createUI() {
        H2 title = new H2("Blocked Users");

        Paragraph description = new Paragraph(
            "Users you've blocked won't be able to see your profile or message you."
        );
        description.getStyle().set("color", "#666");

        blockedUsersGrid = new Grid<>(BlockedUser.class, false);

        // Name column
        blockedUsersGrid.addColumn(blocked -> {
            if (blocked.getBlockedUser() != null) {
                String firstName = blocked.getBlockedUser().getFirstName();
                String lastName = blocked.getBlockedUser().getLastName();
                String name = (firstName != null && !firstName.isEmpty()) ? firstName : "Unknown";
                if (lastName != null && !lastName.isEmpty()) {
                    name += " " + lastName;
                }
                return name;
            }
            return "Unknown User";
        }).setHeader("Name").setFlexGrow(1);

        // Blocked date column
        blockedUsersGrid.addColumn(blocked -> {
            if (blocked.getBlockedAt() != null) {
                return DATE_FORMATTER.format(blocked.getBlockedAt());
            }
            return "";
        }).setHeader("Blocked On").setWidth("150px").setFlexGrow(0);

        // Unblock button column
        blockedUsersGrid.addComponentColumn(blocked -> {
            Button unblockButton = new Button("Unblock");
            unblockButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            unblockButton.setIcon(new Icon(VaadinIcon.UNLOCK));

            unblockButton.addClickListener(e -> {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Unblock User");

                String userName = blocked.getBlockedUser() != null
                    ? blocked.getBlockedUser().getFirstName()
                    : "this user";

                dialog.setText("Are you sure you want to unblock " + userName + "? " +
                    "They will be able to see your profile and potentially match with you again.");

                dialog.setCancelable(true);
                dialog.setConfirmText("Unblock");
                dialog.setConfirmButtonTheme("error primary");

                dialog.addConfirmListener(event -> unblockUser(blocked));
                dialog.open();
            });

            return unblockButton;
        }).setHeader("Actions").setWidth("120px").setFlexGrow(0);

        blockedUsersGrid.setSizeFull();

        add(title, description, blockedUsersGrid);
    }

    private void loadBlockedUsers() {
        try {
            List<BlockedUser> blockedUsers = userService.getBlockedUsers();
            blockedUsersGrid.setItems(blockedUsers);

            if (blockedUsers.isEmpty()) {
                showEmptyState();
            }

        } catch (Exception ex) {
            log.error("Failed to load blocked users", ex);
            Notification.show("Failed to load blocked users",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void unblockUser(BlockedUser blocked) {
        try {
            if (blocked.getBlockedUser() != null) {
                userService.unblockUser(blocked.getBlockedUser().getId());

                Notification.show("User unblocked successfully",
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                // Refresh the list
                loadBlockedUsers();
            }

        } catch (Exception ex) {
            log.error("Failed to unblock user", ex);
            Notification.show("Failed to unblock user",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void showEmptyState() {
        VerticalLayout emptyState = new VerticalLayout();
        emptyState.setAlignItems(Alignment.CENTER);
        emptyState.setPadding(true);

        Icon icon = new Icon(VaadinIcon.CHECK_CIRCLE);
        icon.setSize("48px");
        icon.getStyle().set("color", "#10b981");

        H2 emptyTitle = new H2("No Blocked Users");
        emptyTitle.getStyle().set("color", "#666");

        Paragraph emptyText = new Paragraph("You haven't blocked anyone yet.");
        emptyText.getStyle().set("color", "#999");

        emptyState.add(icon, emptyTitle, emptyText);
        add(emptyState);
    }
}
