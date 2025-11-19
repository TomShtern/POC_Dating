package com.dating.ui.views;

import com.dating.ui.service.UserService;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.extern.slf4j.Slf4j;

/**
 * ForgotPasswordView - Request password reset
 * Sends password reset link to email
 */
@Route("forgot-password")
@PageTitle("Forgot Password | POC Dating")
@AnonymousAllowed
@Slf4j
public class ForgotPasswordView extends VerticalLayout {

    private final UserService userService;

    private EmailField emailField;
    private Button submitButton;
    private Button backButton;

    public ForgotPasswordView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)");

        createUI();
    }

    private void createUI() {
        H1 title = new H1("Reset Password");
        title.getStyle().set("color", "white");

        // Form container
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setWidth("400px");
        formLayout.setPadding(true);
        formLayout.getStyle()
            .set("background", "white")
            .set("border-radius", "12px")
            .set("box-shadow", "0 8px 32px rgba(0,0,0,0.1)");

        Paragraph instructions = new Paragraph(
            "Enter your email address and we'll send you a link to reset your password."
        );
        instructions.getStyle()
            .set("color", "#666")
            .set("text-align", "center")
            .set("margin-bottom", "1rem");

        emailField = new EmailField("Email");
        emailField.setPlaceholder("you@example.com");
        emailField.setRequiredIndicatorVisible(true);
        emailField.setWidthFull();

        submitButton = new Button("Send Reset Link", e -> handleSubmit());
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.setWidthFull();

        backButton = new Button("Back to Login", e ->
            UI.getCurrent().navigate(LoginView.class));
        backButton.setWidthFull();

        formLayout.add(instructions, emailField, submitButton, backButton);

        add(title, formLayout);
    }

    private void handleSubmit() {
        String email = emailField.getValue();

        if (email == null || email.isEmpty() || !email.contains("@")) {
            Notification.show("Please enter a valid email address",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        // Disable button and show loading
        submitButton.setEnabled(false);
        submitButton.setText("Sending...");

        try {
            userService.forgotPassword(email);

            // Show success message
            removeAll();
            showSuccessMessage();

        } catch (Exception ex) {
            log.error("Failed to send reset email", ex);
            // Show generic success to prevent email enumeration
            removeAll();
            showSuccessMessage();
        } finally {
            // Re-enable button (in case user navigates back)
            submitButton.setEnabled(true);
            submitButton.setText("Send Reset Link");
        }
    }

    private void showSuccessMessage() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        VerticalLayout successLayout = new VerticalLayout();
        successLayout.setWidth("400px");
        successLayout.setPadding(true);
        successLayout.setAlignItems(Alignment.CENTER);
        successLayout.getStyle()
            .set("background", "white")
            .set("border-radius", "12px")
            .set("box-shadow", "0 8px 32px rgba(0,0,0,0.1)");

        H1 checkTitle = new H1("Check Your Email");
        checkTitle.getStyle().set("color", "#10b981");

        Paragraph message = new Paragraph(
            "If an account exists with that email, we've sent you a password reset link. " +
            "Please check your inbox and spam folder."
        );
        message.getStyle()
            .set("text-align", "center")
            .set("color", "#666");

        Button loginButton = new Button("Back to Login", e ->
            UI.getCurrent().navigate(LoginView.class));
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        successLayout.add(checkTitle, message, loginButton);
        add(successLayout);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        // Simple view - no listeners to clean up
    }
}
