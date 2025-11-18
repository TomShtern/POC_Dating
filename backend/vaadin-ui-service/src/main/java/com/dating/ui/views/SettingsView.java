package com.dating.ui.views;

import com.dating.ui.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

/**
 * Settings view - Account management
 * Change password, logout, delete account
 */
@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Settings | POC Dating")
@PermitAll
@Slf4j
public class SettingsView extends VerticalLayout {

    private final UserService userService;

    public SettingsView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setPadding(true);
        setAlignItems(Alignment.CENTER);

        createUI();
    }

    private void createUI() {
        H2 title = new H2("Settings");

        // Account section
        Div accountSection = createSection(
            "Account",
            "Manage your account settings",
            VaadinIcon.USER
        );

        // Change password
        Div changePasswordCard = createCard();
        H3 passwordTitle = new H3("Change Password");
        passwordTitle.getStyle().set("margin-top", "0");

        PasswordField currentPasswordField = new PasswordField("Current Password");
        currentPasswordField.setWidthFull();

        PasswordField newPasswordField = new PasswordField("New Password");
        newPasswordField.setWidthFull();
        newPasswordField.setHelperText("Minimum 8 characters, must include uppercase and number");

        PasswordField confirmPasswordField = new PasswordField("Confirm New Password");
        confirmPasswordField.setWidthFull();

        Button changePasswordButton = new Button("Change Password", e -> {
            String current = currentPasswordField.getValue();
            String newPass = newPasswordField.getValue();
            String confirm = confirmPasswordField.getValue();

            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                Notification.show("Please fill in all password fields",
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if (!newPass.equals(confirm)) {
                Notification.show("New passwords do not match",
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if (newPass.length() < 8) {
                Notification.show("Password must be at least 8 characters",
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                userService.changePassword(current, newPass);

                Notification.show("Password changed successfully",
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                // Clear fields
                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();

            } catch (Exception ex) {
                log.error("Failed to change password", ex);
                Notification.show("Failed to change password. Please check your current password.",
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        changePasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        changePasswordCard.add(passwordTitle, currentPasswordField,
            newPasswordField, confirmPasswordField, changePasswordButton);
        accountSection.add(changePasswordCard);

        // Navigation section
        Div navigationSection = createSection(
            "Quick Links",
            "Navigate to other sections",
            VaadinIcon.MENU
        );

        Div navCard = createCard();

        Button editProfileButton = new Button("Edit Profile", e ->
            UI.getCurrent().navigate(ProfileView.class));
        editProfileButton.setIcon(new Icon(VaadinIcon.USER_CARD));
        editProfileButton.setWidthFull();
        editProfileButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button preferencesButton = new Button("Matching Preferences", e ->
            UI.getCurrent().navigate(PreferencesView.class));
        preferencesButton.setIcon(new Icon(VaadinIcon.SLIDER));
        preferencesButton.setWidthFull();
        preferencesButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        navCard.add(editProfileButton, preferencesButton);
        navigationSection.add(navCard);

        // Notifications section
        Div notificationsSection = createSection(
            "Notifications",
            "Control how you receive notifications",
            VaadinIcon.BELL
        );

        Div notificationsCard = createCard();

        Checkbox matchNotifications = new Checkbox("New match notifications");
        matchNotifications.setValue(true);
        matchNotifications.addValueChangeListener(e ->
            Notification.show("Notification preference updated",
                2000, Notification.Position.TOP_CENTER));

        Checkbox messageNotifications = new Checkbox("New message notifications");
        messageNotifications.setValue(true);
        messageNotifications.addValueChangeListener(e ->
            Notification.show("Notification preference updated",
                2000, Notification.Position.TOP_CENTER));

        Checkbox likeNotifications = new Checkbox("Someone liked you notifications");
        likeNotifications.setValue(true);
        likeNotifications.addValueChangeListener(e ->
            Notification.show("Notification preference updated",
                2000, Notification.Position.TOP_CENTER));

        Checkbox emailNotifications = new Checkbox("Email notifications");
        emailNotifications.setValue(false);
        emailNotifications.addValueChangeListener(e ->
            Notification.show("Email preference updated",
                2000, Notification.Position.TOP_CENTER));

        notificationsCard.add(matchNotifications, messageNotifications,
            likeNotifications, emailNotifications);
        notificationsSection.add(notificationsCard);

        // Privacy section
        Div privacySection = createSection(
            "Privacy",
            "Control your profile visibility",
            VaadinIcon.LOCK
        );

        Div privacyCard = createCard();

        Select<String> profileVisibility = new Select<>();
        profileVisibility.setLabel("Profile Visibility");
        profileVisibility.setItems("Everyone", "Matches Only", "Hidden");
        profileVisibility.setValue("Everyone");
        profileVisibility.setWidthFull();
        profileVisibility.addValueChangeListener(e ->
            Notification.show("Profile visibility updated",
                2000, Notification.Position.TOP_CENTER));

        Checkbox showOnlineStatus = new Checkbox("Show when I'm online");
        showOnlineStatus.setValue(true);
        showOnlineStatus.addValueChangeListener(e ->
            Notification.show("Online status preference updated",
                2000, Notification.Position.TOP_CENTER));

        Checkbox showLastActive = new Checkbox("Show last active time");
        showLastActive.setValue(true);
        showLastActive.addValueChangeListener(e ->
            Notification.show("Last active preference updated",
                2000, Notification.Position.TOP_CENTER));

        Checkbox showDistance = new Checkbox("Show my distance to others");
        showDistance.setValue(true);
        showDistance.addValueChangeListener(e ->
            Notification.show("Distance preference updated",
                2000, Notification.Position.TOP_CENTER));

        Checkbox readReceipts = new Checkbox("Show read receipts");
        readReceipts.setValue(true);
        readReceipts.addValueChangeListener(e ->
            Notification.show("Read receipts preference updated",
                2000, Notification.Position.TOP_CENTER));

        privacyCard.add(profileVisibility, showOnlineStatus, showLastActive,
            showDistance, readReceipts);
        privacySection.add(privacyCard);

        // Session section
        Div sessionSection = createSection(
            "Session",
            "Manage your current session",
            VaadinIcon.SIGN_OUT
        );

        Div sessionCard = createCard();

        Button logoutButton = new Button("Logout", e -> {
            userService.logout();
            UI.getCurrent().navigate(LoginView.class);
            UI.getCurrent().getPage().reload();
        });
        logoutButton.setIcon(new Icon(VaadinIcon.SIGN_OUT));
        logoutButton.setWidthFull();
        logoutButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        sessionCard.add(logoutButton);
        sessionSection.add(sessionCard);

        // Danger zone
        Div dangerSection = createSection(
            "Danger Zone",
            "Irreversible actions",
            VaadinIcon.EXCLAMATION_CIRCLE
        );
        dangerSection.getStyle().set("--section-color", "#ef4444");

        Div dangerCard = createCard();
        dangerCard.getStyle().set("border-color", "#fecaca");

        Button deleteAccountButton = new Button("Delete Account", e -> {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Delete Account");
            dialog.setText("Are you sure you want to delete your account? " +
                "This action cannot be undone and all your data will be permanently removed.");
            dialog.setCancelable(true);
            dialog.setConfirmText("Delete");
            dialog.setConfirmButtonTheme("error primary");

            dialog.addConfirmListener(event -> {
                try {
                    userService.deleteAccount();

                    Notification.show("Account deleted successfully",
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                    // Navigate to login
                    UI.getCurrent().navigate(LoginView.class);
                    UI.getCurrent().getPage().reload();

                } catch (Exception ex) {
                    log.error("Failed to delete account", ex);
                    Notification.show("Failed to delete account",
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });

            dialog.open();
        });
        deleteAccountButton.setIcon(new Icon(VaadinIcon.TRASH));
        deleteAccountButton.setWidthFull();
        deleteAccountButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

        dangerCard.add(deleteAccountButton);
        dangerSection.add(dangerCard);

        // Container for sections
        VerticalLayout container = new VerticalLayout();
        container.setWidth("500px");
        container.setSpacing(true);
        container.setPadding(false);
        container.add(accountSection, navigationSection, notificationsSection,
            privacySection, sessionSection, dangerSection);

        add(title, container);
    }

    private Div createSection(String title, String description, VaadinIcon icon) {
        Div section = new Div();
        section.setWidthFull();
        section.getStyle().set("margin-bottom", "1.5rem");

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(Alignment.CENTER);
        header.setSpacing(true);

        Icon sectionIcon = new Icon(icon);
        sectionIcon.setSize("20px");
        sectionIcon.getStyle().set("color", "#667eea");

        Div titleDiv = new Div();
        H3 sectionTitle = new H3(title);
        sectionTitle.getStyle()
            .set("margin", "0")
            .set("font-size", "1.1rem");

        Paragraph sectionDesc = new Paragraph(description);
        sectionDesc.getStyle()
            .set("margin", "0")
            .set("font-size", "0.85rem")
            .set("color", "#666");

        titleDiv.add(sectionTitle, sectionDesc);
        header.add(sectionIcon, titleDiv);

        section.add(header);
        return section;
    }

    private Div createCard() {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
            .set("background", "white")
            .set("border-radius", "8px")
            .set("padding", "1rem")
            .set("box-shadow", "0 1px 3px rgba(0,0,0,0.1)")
            .set("border", "1px solid #e5e7eb")
            .set("margin-top", "0.5rem")
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("gap", "0.75rem");
        return card;
    }
}
