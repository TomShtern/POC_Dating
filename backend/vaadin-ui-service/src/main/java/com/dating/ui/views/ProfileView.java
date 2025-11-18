package com.dating.ui.views;

import com.dating.ui.components.ImageUploadComponent;
import com.dating.ui.components.InterestTagsComponent;
import com.dating.ui.dto.User;
import com.dating.ui.service.UserService;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
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
    private ImageUploadComponent imageUpload;
    private TextArea bioField;
    private InterestTagsComponent interestTags;
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

        // Profile image upload
        imageUpload = new ImageUploadComponent();

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

        bioField = new TextArea("Bio");
        bioField.setMaxLength(500);
        bioField.setHelperText("Tell others about yourself (max 500 characters)");

        // Interests section
        H3 interestsTitle = new H3("Interests & Hobbies");
        interestsTitle.getStyle()
            .set("margin-top", "1rem")
            .set("margin-bottom", "0.5rem");

        interestTags = new InterestTagsComponent();
        interestTags.setMaxTags(10);

        saveButton = new Button("Save Changes", e -> handleSave());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setWidth("200px");

        formLayout.add(
            firstNameField,
            lastNameField,
            ageField,
            genderField,
            cityField,
            countryField
        );
        formLayout.setColspan(bioField, 2);
        formLayout.add(bioField);

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton);
        buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        buttonLayout.setWidthFull();

        add(title, imageUpload, formLayout, interestsTitle, interestTags, buttonLayout);
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
            bioField.setValue(user.getBio() != null ? user.getBio() : "");

            // Set profile image
            if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                imageUpload.setImageUrl(user.getPhotoUrl());
            }

            // Set interests
            if (user.getInterests() != null) {
                interestTags.setInterests(user.getInterests());
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

        // Check for null values from getValue() calls
        Integer age = ageField.getValue();
        String gender = genderField.getValue();
        if (age == null || gender == null) {
            Notification.show("Please fill in all required fields",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        // Disable button and show loading
        saveButton.setEnabled(false);
        saveButton.setText("Saving...");

        try {
            java.util.List<String> interests = interestTags.getInterestsAsList();

            User user = User.builder()
                .firstName(firstNameField.getValue())
                .lastName(lastNameField.getValue())
                .age(age)
                .gender(gender)
                .city(cityField.getValue())
                .country(countryField.getValue() != null ? countryField.getValue() : "")
                .photoUrl(imageUpload.getImageDataUrl())
                .bio(bioField.getValue() != null ? bioField.getValue() : "")
                .interests(interests != null ? interests : java.util.Collections.emptyList())
                .build();

            userService.updateProfile(user);

            Notification.show("Profile updated successfully!",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (Exception ex) {
            log.error("Failed to update profile", ex);
            Notification.show("Failed to update profile", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            // Re-enable button
            saveButton.setEnabled(true);
            saveButton.setText("Save Changes");
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        // Simple view - no listeners to clean up
    }
}
