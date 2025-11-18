package com.dating.ui.views;

import com.dating.ui.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import lombok.extern.slf4j.Slf4j;

/**
 * ReportUserDialog - Dialog for reporting users
 * Allows users to report inappropriate behavior
 */
@Slf4j
public class ReportUserDialog extends Dialog {

    private final UserService userService;
    private final String reportedUserId;
    private final String reportedUserName;

    private ComboBox<String> reasonComboBox;
    private TextArea descriptionField;

    public ReportUserDialog(UserService userService, String reportedUserId, String reportedUserName) {
        this.userService = userService;
        this.reportedUserId = reportedUserId;
        this.reportedUserName = reportedUserName;

        setWidth("450px");
        setCloseOnOutsideClick(false);
        setCloseOnEsc(true);

        createUI();
    }

    private void createUI() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        H3 title = new H3("Report User");
        title.getStyle().set("margin-top", "0");

        Paragraph description = new Paragraph(
            "Report " + reportedUserName + " for inappropriate behavior. " +
            "Our team will review your report and take appropriate action."
        );
        description.getStyle().set("color", "#666");

        // Reason selection
        reasonComboBox = new ComboBox<>("Reason for Report");
        reasonComboBox.setItems(
            "Inappropriate Photos",
            "Harassment or Bullying",
            "Spam or Scam",
            "Fake Profile",
            "Underage User",
            "Threatening Behavior",
            "Hate Speech",
            "Other"
        );
        reasonComboBox.setWidthFull();
        reasonComboBox.setRequired(true);
        reasonComboBox.setPlaceholder("Select a reason");

        // Description field
        descriptionField = new TextArea("Additional Details (Optional)");
        descriptionField.setPlaceholder("Provide any additional context about this report...");
        descriptionField.setWidthFull();
        descriptionField.setMaxLength(500);
        descriptionField.setHelperText("Max 500 characters");
        descriptionField.setMinHeight("100px");

        // Buttons
        Button cancelButton = new Button("Cancel", e -> close());

        Button submitButton = new Button("Submit Report", e -> handleSubmit());
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, submitButton);
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        content.add(title, description, reasonComboBox, descriptionField, buttonLayout);
        add(content);
    }

    private void handleSubmit() {
        String reason = reasonComboBox.getValue();

        if (reason == null || reason.isEmpty()) {
            Notification.show("Please select a reason for the report",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            userService.reportUser(reportedUserId, reason, descriptionField.getValue());

            Notification.show("Report submitted successfully. Thank you for helping keep our community safe.",
                4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            close();

        } catch (Exception ex) {
            log.error("Failed to submit report", ex);
            Notification.show("Failed to submit report. Please try again.",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
