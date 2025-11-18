package com.dating.ui.views;

import com.dating.ui.dto.User;
import com.dating.ui.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
    private ComboBox<String> genderField;
    private TextField cityField;
    private TextField countryField;
    private TextField photoUrlField;
    private TextArea bioField;
    private Button saveButton;
    private Image profileImage;

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

        // Profile image preview
        profileImage = new Image();
        profileImage.setWidth("150px");
        profileImage.setHeight("150px");
        profileImage.getStyle()
            .set("border-radius", "50%")
            .set("object-fit", "cover")
            .set("border", "3px solid #667eea")
            .set("margin-bottom", "1rem");

        FormLayout formLayout = new FormLayout();
        formLayout.setWidth("600px");
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );

        firstNameField = new TextField("First Name");
        firstNameField.setRequired(true);

        lastNameField = new TextField("Last Name");
        lastNameField.setRequired(true);

        ageField = new IntegerField("Age");
        ageField.setMin(18);
        ageField.setMax(100);
        ageField.setRequired(true);

        genderField = new ComboBox<>("Gender");
        genderField.setItems("Male", "Female", "Other");
        genderField.setRequired(true);

        cityField = new TextField("City");
        cityField.setRequired(true);

        countryField = new TextField("Country");

        photoUrlField = new TextField("Photo URL");
        photoUrlField.setPlaceholder("https://example.com/photo.jpg");
        photoUrlField.setHelperText("Enter URL of your profile photo");
        photoUrlField.addValueChangeListener(e -> {
            String url = e.getValue();
            if (url != null && !url.isEmpty()) {
                profileImage.setSrc(url);
            } else {
                profileImage.setSrc("https://via.placeholder.com/150x150?text=No+Photo");
            }
        });

        bioField = new TextArea("Bio");
        bioField.setMaxLength(500);
        bioField.setHelperText("Tell others about yourself (max 500 characters)");

        saveButton = new Button("Save Changes", e -> handleSave());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setWidth("200px");

        formLayout.add(
            firstNameField,
            lastNameField,
            ageField,
            genderField,
            cityField,
            countryField,
            photoUrlField
        );
        formLayout.setColspan(bioField, 2);
        formLayout.add(bioField);

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton);
        buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        buttonLayout.setWidthFull();

        add(title, profileImage, formLayout, buttonLayout);
    }

    private void loadProfile() {
        try {
            User user = userService.getCurrentUser();

            firstNameField.setValue(user.getFirstName() != null ? user.getFirstName() : "");
            lastNameField.setValue(user.getLastName() != null ? user.getLastName() : "");
            ageField.setValue(user.getAge() != null ? user.getAge() : 18);
            genderField.setValue(user.getGender() != null ? user.getGender() : "");
            cityField.setValue(user.getCity() != null ? user.getCity() : "");
            countryField.setValue(user.getCountry() != null ? user.getCountry() : "");
            photoUrlField.setValue(user.getPhotoUrl() != null ? user.getPhotoUrl() : "");
            bioField.setValue(user.getBio() != null ? user.getBio() : "");

            // Set profile image
            if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                profileImage.setSrc(user.getPhotoUrl());
            } else {
                profileImage.setSrc("https://via.placeholder.com/150x150?text=No+Photo");
            }

        } catch (Exception ex) {
            log.error("Failed to load profile", ex);
            Notification.show("Failed to load profile", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void handleSave() {
        // Validate required fields
        if (firstNameField.isEmpty() || lastNameField.isEmpty() ||
            ageField.isEmpty() || genderField.isEmpty() || cityField.isEmpty()) {
            Notification.show("Please fill in all required fields",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            User user = User.builder()
                .firstName(firstNameField.getValue())
                .lastName(lastNameField.getValue())
                .age(ageField.getValue())
                .gender(genderField.getValue())
                .city(cityField.getValue())
                .country(countryField.getValue())
                .photoUrl(photoUrlField.getValue())
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
