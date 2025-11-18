package com.dating.ui.views;

import com.dating.ui.dto.RegisterRequest;
import com.dating.ui.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.extern.slf4j.Slf4j;

/**
 * Registration view - Create new account
 */
@Route("register")
@PageTitle("Register | POC Dating")
@AnonymousAllowed
@Slf4j
public class RegisterView extends VerticalLayout {

    private final UserService userService;

    private EmailField emailField;
    private TextField usernameField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private TextField firstNameField;
    private TextField lastNameField;
    private IntegerField ageField;
    private ComboBox<String> genderField;
    private TextField cityField;
    private Checkbox termsCheckbox;
    private Button registerButton;
    private Button backButton;

    public RegisterView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)");

        createUI();
    }

    private void createUI() {
        H1 title = new H1("Create Account");
        title.getStyle().set("color", "white");

        // Form container
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setWidth("400px");
        formLayout.setPadding(true);
        formLayout.getStyle()
            .set("background", "white")
            .set("border-radius", "12px")
            .set("box-shadow", "0 8px 32px rgba(0,0,0,0.1)");

        // Form fields
        emailField = new EmailField("Email");
        emailField.setRequiredIndicatorVisible(true);
        emailField.setWidthFull();

        usernameField = new TextField("Username");
        usernameField.setRequiredIndicatorVisible(true);
        usernameField.setWidthFull();

        passwordField = new PasswordField("Password");
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setWidthFull();
        passwordField.setHelperText("At least 8 characters");

        confirmPasswordField = new PasswordField("Confirm Password");
        confirmPasswordField.setRequiredIndicatorVisible(true);
        confirmPasswordField.setWidthFull();

        firstNameField = new TextField("First Name");
        firstNameField.setRequiredIndicatorVisible(true);
        firstNameField.setWidthFull();

        lastNameField = new TextField("Last Name");
        lastNameField.setRequiredIndicatorVisible(true);
        lastNameField.setWidthFull();

        ageField = new IntegerField("Age");
        ageField.setRequiredIndicatorVisible(true);
        ageField.setMin(18);
        ageField.setMax(100);
        ageField.setWidthFull();

        genderField = new ComboBox<>("Gender");
        genderField.setItems("Male", "Female", "Other");
        genderField.setRequiredIndicatorVisible(true);
        genderField.setWidthFull();

        cityField = new TextField("City");
        cityField.setRequiredIndicatorVisible(true);
        cityField.setWidthFull();
        cityField.setPlaceholder("e.g., New York");

        // Add real-time password validation
        passwordField.addValueChangeListener(e -> {
            String password = e.getValue();
            if (password != null && password.length() > 0 && password.length() < 8) {
                passwordField.setInvalid(true);
                passwordField.setErrorMessage("Password must be at least 8 characters");
            } else {
                passwordField.setInvalid(false);
            }
        });

        // Add real-time confirm password validation
        confirmPasswordField.addValueChangeListener(e -> {
            String confirmValue = e.getValue();
            String passwordValue = passwordField.getValue();
            if (confirmValue != null && passwordValue != null && !confirmValue.equals(passwordValue)) {
                confirmPasswordField.setInvalid(true);
                confirmPasswordField.setErrorMessage("Passwords don't match");
            } else {
                confirmPasswordField.setInvalid(false);
            }
        });

        // Terms and conditions
        termsCheckbox = new Checkbox();
        Span termsText = new Span("I agree to the ");
        Anchor termsLink = new Anchor("#", "Terms of Service");
        termsLink.getStyle().set("color", "#667eea");
        Span andText = new Span(" and ");
        Anchor privacyLink = new Anchor("#", "Privacy Policy");
        privacyLink.getStyle().set("color", "#667eea");

        HorizontalLayout termsLayout = new HorizontalLayout(
            termsCheckbox, termsText, termsLink, andText, privacyLink);
        termsLayout.setAlignItems(Alignment.CENTER);
        termsLayout.setSpacing(false);
        termsLayout.getStyle().set("flex-wrap", "wrap");

        registerButton = new Button("Create Account", e -> handleRegister());
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.setWidthFull();

        backButton = new Button("Back to Login", e -> UI.getCurrent().navigate(LoginView.class));
        backButton.setWidthFull();

        formLayout.add(
            emailField, usernameField,
            passwordField, confirmPasswordField,
            firstNameField, lastNameField,
            ageField, genderField,
            cityField,
            termsLayout,
            registerButton, backButton
        );

        add(title, formLayout);
    }

    private void handleRegister() {
        // Validation
        if (!validateForm()) {
            return;
        }

        try {
            RegisterRequest request = RegisterRequest.builder()
                .email(emailField.getValue())
                .username(usernameField.getValue())
                .password(passwordField.getValue())
                .firstName(firstNameField.getValue())
                .lastName(lastNameField.getValue())
                .age(ageField.getValue())
                .gender(genderField.getValue())
                .city(cityField.getValue())
                .build();

            userService.register(request);

            Notification.show("Account created successfully!",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Navigate to main app
            UI.getCurrent().navigate(SwipeView.class);

        } catch (Exception ex) {
            log.error("Registration failed", ex);
            showError("Registration failed. Email might already be in use.");
        }
    }

    private boolean validateForm() {
        if (emailField.isEmpty() || usernameField.isEmpty() ||
            passwordField.isEmpty() || confirmPasswordField.isEmpty() ||
            firstNameField.isEmpty() || lastNameField.isEmpty() ||
            ageField.isEmpty() || genderField.isEmpty() || cityField.isEmpty()) {
            showError("Please fill in all fields");
            return false;
        }

        String email = emailField.getValue();
        String username = usernameField.getValue();
        String password = passwordField.getValue();
        String confirmPassword = confirmPasswordField.getValue();
        Integer age = ageField.getValue();

        // Validate email format
        if (email == null || !email.contains("@")) {
            showError("Please enter a valid email address");
            return false;
        }

        // Validate username length
        if (username == null || username.length() < 3) {
            showError("Username must be at least 3 characters");
            return false;
        }

        if (password == null || confirmPassword == null || !password.equals(confirmPassword)) {
            showError("Passwords don't match");
            return false;
        }

        if (password.length() < 8) {
            showError("Password must be at least 8 characters");
            return false;
        }

        if (age == null || age < 18) {
            showError("You must be at least 18 years old");
            return false;
        }

        if (!termsCheckbox.getValue()) {
            showError("You must accept the Terms of Service and Privacy Policy");
            return false;
        }

        return true;
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
