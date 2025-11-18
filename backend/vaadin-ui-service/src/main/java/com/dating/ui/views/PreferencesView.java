package com.dating.ui.views;

import com.dating.ui.dto.User;
import com.dating.ui.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

/**
 * Preferences view - Edit matching preferences
 * Age range, gender preference, maximum distance
 */
@Route(value = "preferences", layout = MainLayout.class)
@PageTitle("Preferences | POC Dating")
@PermitAll
@Slf4j
public class PreferencesView extends VerticalLayout {

    private final UserService userService;

    private IntegerField minAgeField;
    private IntegerField maxAgeField;
    private ComboBox<String> interestedInGenderField;
    private IntegerField maxDistanceField;
    private Button saveButton;

    public PreferencesView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setPadding(true);
        setAlignItems(Alignment.CENTER);

        createUI();
        loadPreferences();
    }

    private void createUI() {
        H2 title = new H2("Matching Preferences");

        Paragraph description = new Paragraph(
            "Set your preferences to find better matches. " +
            "These settings control who appears in your discovery feed."
        );
        description.getStyle()
            .set("color", "#666")
            .set("text-align", "center")
            .set("max-width", "500px");

        FormLayout formLayout = new FormLayout();
        formLayout.setWidth("500px");
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("400px", 2)
        );

        // Age range
        minAgeField = new IntegerField("Minimum Age");
        minAgeField.setMin(18);
        minAgeField.setMax(100);
        minAgeField.setValue(18);
        minAgeField.setStepButtonsVisible(true);
        minAgeField.setHelperText("Minimum age of potential matches");

        maxAgeField = new IntegerField("Maximum Age");
        maxAgeField.setMin(18);
        maxAgeField.setMax(100);
        maxAgeField.setValue(99);
        maxAgeField.setStepButtonsVisible(true);
        maxAgeField.setHelperText("Maximum age of potential matches");

        // Validate age range
        minAgeField.addValueChangeListener(e -> {
            if (e.getValue() != null && maxAgeField.getValue() != null &&
                e.getValue() > maxAgeField.getValue()) {
                maxAgeField.setValue(e.getValue());
            }
        });

        maxAgeField.addValueChangeListener(e -> {
            if (e.getValue() != null && minAgeField.getValue() != null &&
                e.getValue() < minAgeField.getValue()) {
                minAgeField.setValue(e.getValue());
            }
        });

        // Gender preference
        interestedInGenderField = new ComboBox<>("Interested In");
        interestedInGenderField.setItems("Male", "Female", "Both");
        interestedInGenderField.setRequired(true);
        interestedInGenderField.setHelperText("Who do you want to match with?");

        // Distance
        maxDistanceField = new IntegerField("Maximum Distance (km)");
        maxDistanceField.setMin(1);
        maxDistanceField.setMax(500);
        maxDistanceField.setValue(50);
        maxDistanceField.setStepButtonsVisible(true);
        maxDistanceField.setHelperText("Maximum distance from your location");

        saveButton = new Button("Save Preferences", e -> handleSave());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setWidth("200px");

        formLayout.add(
            minAgeField,
            maxAgeField,
            interestedInGenderField,
            maxDistanceField
        );

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton);
        buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        buttonLayout.setWidthFull();

        add(title, description, formLayout, buttonLayout);
    }

    private void loadPreferences() {
        try {
            User preferences = userService.getPreferences();

            if (preferences.getMinAge() != null) {
                minAgeField.setValue(preferences.getMinAge());
            }
            if (preferences.getMaxAge() != null) {
                maxAgeField.setValue(preferences.getMaxAge());
            }
            if (preferences.getInterestedInGender() != null) {
                interestedInGenderField.setValue(preferences.getInterestedInGender());
            }
            if (preferences.getMaxDistance() != null) {
                maxDistanceField.setValue(preferences.getMaxDistance());
            }

        } catch (Exception ex) {
            log.error("Failed to load preferences", ex);
            Notification.show("Failed to load preferences", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void handleSave() {
        // Validate
        if (interestedInGenderField.isEmpty()) {
            Notification.show("Please select who you're interested in",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            User preferences = User.builder()
                .minAge(minAgeField.getValue())
                .maxAge(maxAgeField.getValue())
                .interestedInGender(interestedInGenderField.getValue())
                .maxDistance(maxDistanceField.getValue())
                .build();

            userService.updatePreferences(preferences);

            Notification.show("Preferences saved successfully!",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (Exception ex) {
            log.error("Failed to save preferences", ex);
            Notification.show("Failed to save preferences", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
