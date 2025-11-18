package com.dating.ui.views;

import com.dating.ui.components.ProfileCard;
import com.dating.ui.dto.SwipeResponse;
import com.dating.ui.dto.SwipeType;
import com.dating.ui.dto.User;
import com.dating.ui.service.MatchService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

/**
 * Swipe view - Main discovery interface
 * Users can swipe on profiles (like, pass, super like)
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Discover | POC Dating")
@PermitAll
@Slf4j
public class SwipeView extends VerticalLayout {

    private final MatchService matchService;

    private User currentUser;
    private User previousUser;
    private ProfileCard profileCard;
    private Button passButton;
    private Button superLikeButton;
    private Button likeButton;
    private Button undoButton;

    public SwipeView(MatchService matchService) {
        this.matchService = matchService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setPadding(true);

        createUI();
        loadNextProfile();
    }

    private void createUI() {
        H2 title = new H2("Discover");

        // Profile card component
        profileCard = new ProfileCard();
        profileCard.setWidth("400px");
        profileCard.setHeight("600px");

        // Action buttons
        passButton = new Button("✖️ Pass", e -> handlePass());
        passButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_LARGE);

        superLikeButton = new Button("⭐ Super Like", e -> handleSuperLike());
        superLikeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);

        likeButton = new Button("❤️ Like", e -> handleLike());
        likeButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_LARGE);

        undoButton = new Button("↩️ Undo", e -> handleUndo());
        undoButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        undoButton.setEnabled(false);

        HorizontalLayout buttons = new HorizontalLayout(passButton, superLikeButton, likeButton);
        buttons.setSpacing(true);
        buttons.setJustifyContentMode(JustifyContentMode.CENTER);

        HorizontalLayout undoLayout = new HorizontalLayout(undoButton);
        undoLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        undoLayout.setWidthFull();

        // Keyboard shortcuts hint
        Span shortcutHint = new Span("Keyboard shortcuts: ← Pass | ↑ Super Like | → Like");
        shortcutHint.getStyle()
            .set("color", "#999")
            .set("font-size", "0.85rem")
            .set("margin-top", "1rem");

        // Add keyboard shortcuts
        passButton.addClickShortcut(Key.ARROW_LEFT);
        superLikeButton.addClickShortcut(Key.ARROW_UP);
        likeButton.addClickShortcut(Key.ARROW_RIGHT);

        add(title, profileCard, buttons, undoLayout, shortcutHint);
    }

    private void loadNextProfile() {
        try {
            currentUser = matchService.getNextProfile();

            if (currentUser != null) {
                profileCard.setUser(currentUser);
                log.debug("Loaded profile: {}", currentUser.getFirstName());
            } else {
                profileCard.showNoMoreProfiles();
                disableButtons();
            }

        } catch (Exception ex) {
            log.error("Failed to load profile", ex);
            Notification.show("Failed to load profiles", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void handleLike() {
        if (currentUser == null) return;

        // Disable buttons and show loading
        setSwipeButtonsEnabled(false);
        likeButton.setText("...");

        try {
            previousUser = currentUser;
            SwipeResponse response = matchService.recordSwipe(currentUser.getId(), SwipeType.LIKE);

            if (response.isMatch()) {
                showMatchNotification(currentUser);
            }

            undoButton.setEnabled(true);
            loadNextProfile();

        } catch (Exception ex) {
            log.error("Failed to record swipe", ex);
            showError("Failed to record swipe");
        } finally {
            // Re-enable buttons
            setSwipeButtonsEnabled(true);
            likeButton.setText("\u2764\uFE0F Like");
        }
    }

    private void handleSuperLike() {
        if (currentUser == null) return;

        // Disable buttons and show loading
        setSwipeButtonsEnabled(false);
        superLikeButton.setText("...");

        try {
            previousUser = currentUser;
            SwipeResponse response = matchService.recordSwipe(currentUser.getId(), SwipeType.SUPER_LIKE);

            if (response.isMatch()) {
                showMatchNotification(currentUser);
            }

            undoButton.setEnabled(true);
            loadNextProfile();

        } catch (Exception ex) {
            log.error("Failed to record swipe", ex);
            showError("Failed to record swipe");
        } finally {
            // Re-enable buttons
            setSwipeButtonsEnabled(true);
            superLikeButton.setText("\u2B50 Super Like");
        }
    }

    private void handlePass() {
        if (currentUser == null) return;

        // Disable buttons and show loading
        setSwipeButtonsEnabled(false);
        passButton.setText("...");

        try {
            previousUser = currentUser;
            matchService.recordSwipe(currentUser.getId(), SwipeType.PASS);
            undoButton.setEnabled(true);
            loadNextProfile();

        } catch (Exception ex) {
            log.error("Failed to record swipe", ex);
            showError("Failed to record swipe");
        } finally {
            // Re-enable buttons
            setSwipeButtonsEnabled(true);
            passButton.setText("\u2716\uFE0F Pass");
        }
    }

    private void handleUndo() {
        // Disable undo button and show loading
        undoButton.setEnabled(false);
        undoButton.setText("...");

        try {
            matchService.undoLastSwipe();

            // Restore previous user to card
            if (previousUser != null) {
                currentUser = previousUser;
                profileCard.setUser(currentUser);
                previousUser = null;
            }

            Notification.show("Last swipe undone",
                2000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (Exception ex) {
            log.error("Failed to undo swipe", ex);
            showError("Failed to undo swipe");
            // Re-enable if undo failed
            undoButton.setEnabled(true);
        } finally {
            // Reset button text
            undoButton.setText("\u21A9\uFE0F Undo");
        }
    }

    private void showMatchNotification(User user) {
        Notification notification = new Notification();
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.setDuration(5000);
        notification.setPosition(Notification.Position.MIDDLE);

        VerticalLayout content = new VerticalLayout();
        content.add(new H2("🎉 It's a Match!"));
        content.add(new Paragraph("You and " + user.getFirstName() + " liked each other!"));

        Button chatButton = new Button("Send Message", e -> {
            UI.getCurrent().navigate(MessagesView.class);
            notification.close();
        });
        chatButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        content.add(chatButton);
        notification.add(content);
        notification.open();
    }

    private void setSwipeButtonsEnabled(boolean enabled) {
        passButton.setEnabled(enabled);
        superLikeButton.setEnabled(enabled);
        likeButton.setEnabled(enabled);
    }

    private void disableButtons() {
        passButton.setEnabled(false);
        superLikeButton.setEnabled(false);
        likeButton.setEnabled(false);
    }

    private void showError(String message) {
        Notification.show(message, 3000, Notification.Position.TOP_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    public User getCurrentUser() {
        return currentUser;
    }
}
