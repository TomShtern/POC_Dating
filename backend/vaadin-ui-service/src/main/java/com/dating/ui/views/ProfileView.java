package com.dating.ui.views;

import com.dating.ui.dto.User;
import com.dating.ui.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

/**
 * Profile view - Edit user profile
 */
@Route(value = "profile", layout = MainLayout.class)
@PageTitle("My Profile | POC Dating")
@PermitAll
@Slf4j
public class ProfileView extends VerticalLayout {

    private final UserService userService;

    private TextField firstNameField;
    private TextField lastNameField;
    private IntegerField ageField;
    private TextField cityField;
    private TextArea bioField;
    private Button saveButton;

    public ProfileView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setPadding(true);
        setAlignItems(Alignment.CENTER);

        createUI();
        loadProfile();
    }

    private void createUI() {
        H2 title = new H2("My Profile");

        FormLayout formLayout = new FormLayout();
        formLayout.setWidth("600px");

        firstNameField = new TextField("First Name");
        lastNameField = new TextField("Last Name");
        ageField = new IntegerField("Age");
        ageField.setMin(18);
        ageField.setMax(100);

        cityField = new TextField("City");

        bioField = new TextArea("Bio");
        bioField.setMaxLength(500);
        bioField.setHelperText("Tell others about yourself");

        saveButton = new Button("Save Changes", e -> handleSave());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        formLayout.add(
            firstNameField,
            lastNameField,
            ageField,
            cityField,
            bioField
        );

        add(title, formLayout, saveButton);
    }

    private void loadProfile() {
        try {
            User user = userService.getCurrentUser();

            firstNameField.setValue(user.getFirstName() != null ? user.getFirstName() : "");
            lastNameField.setValue(user.getLastName() != null ? user.getLastName() : "");
            ageField.setValue(user.getAge());
            cityField.setValue(user.getCity() != null ? user.getCity() : "");
            bioField.setValue(user.getBio() != null ? user.getBio() : "");

        } catch (Exception ex) {
            log.error("Failed to load profile", ex);
            Notification.show("Failed to load profile", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void handleSave() {
        try {
            User user = User.builder()
                .firstName(firstNameField.getValue())
                .lastName(lastNameField.getValue())
                .age(ageField.getValue())
                .city(cityField.getValue())
                .bio(bioField.getValue())
                .build();

            userService.updateProfile(user);

            Notification.show("Profile updated successfully!",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (Exception ex) {
            log.error("Failed to update profile", ex);
            Notification.show("Failed to update profile", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
