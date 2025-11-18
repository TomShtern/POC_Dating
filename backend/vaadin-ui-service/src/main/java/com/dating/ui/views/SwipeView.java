package com.dating.ui.views;

import com.dating.ui.components.ProfileCard;
import com.dating.ui.dto.SwipeResponse;
import com.dating.ui.dto.SwipeType;
import com.dating.ui.dto.User;
import com.dating.ui.service.MatchService;
import com.dating.ui.service.PageViewMetricsService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
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
    private ProfileCard profileCard;
    private Button passButton;
    private Button superLikeButton;
    private Button likeButton;

    public SwipeView(MatchService matchService, PageViewMetricsService pageViewMetrics) {
        this.matchService = matchService;

        // Record page view metric
        pageViewMetrics.recordPageView("discover");

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

        HorizontalLayout buttons = new HorizontalLayout(passButton, superLikeButton, likeButton);
        buttons.setSpacing(true);
        buttons.setJustifyContentMode(JustifyContentMode.CENTER);

        add(title, profileCard, buttons);
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

        try {
            SwipeResponse response = matchService.recordSwipe(currentUser.getId(), SwipeType.LIKE);

            if (response.isMatch()) {
                showMatchNotification(currentUser);
            }

            loadNextProfile();

        } catch (Exception ex) {
            log.error("Failed to record swipe", ex);
            showError("Failed to record swipe");
        }
    }

    private void handleSuperLike() {
        if (currentUser == null) return;

        try {
            SwipeResponse response = matchService.recordSwipe(currentUser.getId(), SwipeType.SUPER_LIKE);

            if (response.isMatch()) {
                showMatchNotification(currentUser);
            }

            loadNextProfile();

        } catch (Exception ex) {
            log.error("Failed to record swipe", ex);
            showError("Failed to record swipe");
        }
    }

    private void handlePass() {
        if (currentUser == null) return;

        try {
            matchService.recordSwipe(currentUser.getId(), SwipeType.PASS);
            loadNextProfile();

        } catch (Exception ex) {
            log.error("Failed to record swipe", ex);
            showError("Failed to record swipe");
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
