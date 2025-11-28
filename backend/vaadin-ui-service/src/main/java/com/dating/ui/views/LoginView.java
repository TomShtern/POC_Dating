package com.dating.ui.views;

import com.dating.ui.dto.AuthResponse;
import com.dating.ui.service.PageViewMetricsService;
import com.dating.ui.service.UserService;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.extern.slf4j.Slf4j;

/**
 * Login view - Entry point for the application
 * Allows users to authenticate
 */
@Route("login")
@PageTitle("Login | POC Dating")
@AnonymousAllowed
@Slf4j
public class LoginView extends VerticalLayout {

    private final UserService userService;
    private final PageViewMetricsService pageViewMetrics;

    private EmailField emailField;
    private PasswordField passwordField;
    private Button loginButton;
    private Button registerButton;

    public LoginView(UserService userService, PageViewMetricsService pageViewMetrics) {
        this.userService = userService;
        this.pageViewMetrics = pageViewMetrics;

        // Record page view metric
        pageViewMetrics.recordPageView("login");

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)");

        createUI();
    }

    private void createUI() {
        // Title
        H1 title = new H1("❤️ POC Dating");
        title.getStyle()
            .set("color", "white")
            .set("margin-bottom", "2rem");

        Span subtitle = new Span("Find your perfect match");
        subtitle.getStyle()
            .set("color", "rgba(255,255,255,0.8)")
            .set("font-size", "1.2rem");

        // Form container
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setWidth("400px");
        formLayout.setPadding(true);
        formLayout.getStyle()
            .set("background", "white")
            .set("border-radius", "12px")
            .set("box-shadow", "0 8px 32px rgba(0,0,0,0.1)");

        // Email field
        emailField = new EmailField("Email");
        emailField.setPlaceholder("you@example.com");
        emailField.setRequiredIndicatorVisible(true);
        emailField.setWidthFull();

        // Password field
        passwordField = new PasswordField("Password");
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setWidthFull();

        // Login button
        loginButton = new Button("Login", e -> handleLogin());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();

        // Forgot password link
        RouterLink forgotPasswordLink = new RouterLink("Forgot Password?", ForgotPasswordView.class);
        forgotPasswordLink.getStyle()
            .set("font-size", "0.9rem")
            .set("color", "#667eea");

        HorizontalLayout forgotLayout = new HorizontalLayout(forgotPasswordLink);
        forgotLayout.setJustifyContentMode(JustifyContentMode.END);
        forgotLayout.setWidthFull();

        // Register button
        registerButton = new Button("Create Account", e -> handleRegister());
        registerButton.setWidthFull();

        formLayout.add(emailField, passwordField, forgotLayout, loginButton, registerButton);

        add(title, subtitle, formLayout);
    }

    private void handleLogin() {
        String email = emailField.getValue();
        String password = passwordField.getValue();

        // Validation
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        if (!email.contains("@")) {
            emailField.setInvalid(true);
            emailField.setErrorMessage("Please enter a valid email");
            return;
        }

        // Disable button and show loading
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        try {
            // Call user service to login
            AuthResponse response = userService.login(email, password);

            if (response == null || response.getUser() == null) {
                showError("Login failed - invalid response");
                return;
            }

            log.info("Login successful for user: {}", response.getUser().getId());

            // Show success message
            String firstName = response.getUser().getFirstName();
            String welcomeName = (firstName != null && !firstName.isEmpty()) ? firstName : "User";
            Notification.show("Welcome back, " + welcomeName + "!",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Navigate to main app
            UI.getCurrent().navigate(SwipeView.class);

        } catch (Exception ex) {
            log.error("Login failed for email: {}", email, ex);
            userService.recordLoginFailure();
            showError("Invalid email or password");
        } finally {
            // Re-enable button
            loginButton.setEnabled(true);
            loginButton.setText("Login");
        }
    }

    private void handleRegister() {
        UI.getCurrent().navigate(RegisterView.class);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        // Simple view - no listeners to clean up
    }
}
