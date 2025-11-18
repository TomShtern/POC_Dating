package com.dating.ui.views;

import com.dating.ui.dto.Match;
import com.dating.ui.service.MatchService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * NotificationsView - View match and message notifications
 * Displays recent activity like new matches and messages
 */
@Route(value = "notifications", layout = MainLayout.class)
@PageTitle("Notifications | POC Dating")
@PermitAll
@Slf4j
public class NotificationsView extends VerticalLayout {

    private final MatchService matchService;
    private VerticalLayout notificationsList;

    // Track read notifications with size limit to prevent memory issues
    // Uses LinkedHashSet to maintain insertion order for LRU-style eviction
    private static final int MAX_READ_NOTIFICATIONS = 100;
    private final Set<String> readNotifications = new LinkedHashSet<>() {
        @Override
        public boolean add(String e) {
            if (size() >= MAX_READ_NOTIFICATIONS) {
                // Remove oldest entry when at capacity
                remove(iterator().next());
            }
            return super.add(e);
        }
    };

    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("MMM d, HH:mm");

    public NotificationsView(MatchService matchService) {
        this.matchService = matchService;

        setSizeFull();
        setPadding(true);
        setAlignItems(Alignment.CENTER);

        createUI();
        loadNotifications();
    }

    private void createUI() {
        H2 title = new H2("Notifications");

        // Toolbar with mark all as read
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setMaxWidth("600px");
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button markAllReadButton = new Button("Mark all as read", e -> {
            Button btn = (Button) e.getSource();
            btn.setEnabled(false);
            btn.setText("Marking...");

            try {
                markAllAsRead();
            } finally {
                btn.setEnabled(true);
                btn.setText("Mark all as read");
            }
        });
        markAllReadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        toolbar.add(markAllReadButton);

        notificationsList = new VerticalLayout();
        notificationsList.setWidth("600px");
        notificationsList.setPadding(false);
        notificationsList.setSpacing(true);

        add(title, toolbar, notificationsList);
    }

    private void markAllAsRead() {
        try {
            // TODO: Backend API needed - MatchService should have a method like:
            // matchService.markAllNotificationsAsRead()
            // This would call an endpoint like POST /api/matches/notifications/read-all
            // Currently only updating UI state locally

            // Mark all notifications as read in UI
            notificationsList.getChildren().forEach(component -> {
                if (component instanceof Div card) {
                    card.removeClassName("unread");
                    card.addClassName("read");
                    // Extract match ID from card if stored as data attribute
                    // and add to readNotifications set
                }
            });

            log.info("Marked all notifications as read (UI only - backend persistence not implemented)");

            Notification.show("All notifications marked as read",
                2000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            log.error("Failed to mark notifications as read", ex);
            Notification.show("Failed to mark notifications as read",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void loadNotifications() {
        try {
            // Load recent matches as notifications
            List<Match> matches = matchService.getMyMatches();

            if (matches.isEmpty()) {
                showEmptyState();
                return;
            }

            // Show recent matches as notifications
            for (Match match : matches) {
                if (match.getOtherUser() != null) {
                    notificationsList.add(createMatchNotification(match));
                }
            }

            // Add clear all button
            Button clearAllButton = new Button("Clear All", e -> {
                Button btn = (Button) e.getSource();
                btn.setEnabled(false);
                btn.setText("Clearing...");

                try {
                    // TODO: Backend API needed - MatchService should have a method like:
                    // matchService.clearAllNotifications()
                    // This would call an endpoint like DELETE /api/matches/notifications
                    // Currently only clearing UI state locally

                    notificationsList.removeAll();
                    readNotifications.clear();
                    showEmptyState();

                    log.info("Cleared all notifications (UI only - backend persistence not implemented)");

                    Notification.show("All notifications cleared",
                        2000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    log.error("Failed to clear notifications", ex);
                    Notification.show("Failed to clear notifications",
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                } finally {
                    btn.setEnabled(true);
                    btn.setText("Clear All");
                }
            });
            clearAllButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            add(clearAllButton);

        } catch (Exception ex) {
            log.error("Failed to load notifications", ex);
            notificationsList.add(new Paragraph("Failed to load notifications"));
        }
    }

    private Div createMatchNotification(Match match) {
        Div card = new Div();
        card.setWidthFull();
        card.addClassName("notification-card");

        // Set unread state if has unread messages
        if (match.isHasUnreadMessages() && !readNotifications.contains(match.getId())) {
            card.addClassName("unread");
        } else {
            card.addClassName("read");
        }

        card.getStyle()
            .set("border-radius", "8px")
            .set("padding", "1rem")
            .set("box-shadow", "0 1px 3px rgba(0,0,0,0.1)")
            .set("cursor", "pointer");

        card.addClickListener(e -> {
            // Mark as read
            card.removeClassName("unread");
            card.addClassName("read");
            readNotifications.add(match.getId());

            // Navigate to user detail
            UI.getCurrent().navigate(UserDetailView.class, match.getId());
        });

        HorizontalLayout content = new HorizontalLayout();
        content.setAlignItems(Alignment.CENTER);
        content.setWidthFull();

        // Icon
        Icon heartIcon = new Icon(VaadinIcon.HEART);
        heartIcon.setSize("24px");
        heartIcon.getStyle().set("color", "#f093fb");

        // Text content
        VerticalLayout textContent = new VerticalLayout();
        textContent.setPadding(false);
        textContent.setSpacing(false);

        String firstName = match.getOtherUser().getFirstName();
        String name = (firstName != null && !firstName.isEmpty()) ? firstName : "Someone";
        Span title = new Span("New match with " + name + "!");
        title.getStyle()
            .set("font-weight", "500")
            .set("color", "#333");

        Span subtitle = new Span("You and " + name + " liked each other");
        subtitle.getStyle()
            .set("font-size", "0.85rem")
            .set("color", "#666");

        textContent.add(title, subtitle);

        // Time
        Span time = new Span();
        if (match.getCreatedAt() != null) {
            time.setText(TIME_FORMATTER.format(match.getCreatedAt()));
        }
        time.getStyle()
            .set("font-size", "0.75rem")
            .set("color", "#999");

        content.add(heartIcon, textContent, time);
        content.setFlexGrow(1, textContent);

        // Unread indicator
        if (match.isHasUnreadMessages()) {
            Div badge = new Div();
            badge.setText("New");
            badge.getStyle()
                .set("background", "#ef4444")
                .set("color", "white")
                .set("border-radius", "4px")
                .set("padding", "2px 6px")
                .set("font-size", "0.7rem")
                .set("margin-left", "auto");
            content.add(badge);
        }

        card.add(content);
        return card;
    }

    private void showEmptyState() {
        VerticalLayout emptyState = new VerticalLayout();
        emptyState.setAlignItems(Alignment.CENTER);
        emptyState.setJustifyContentMode(JustifyContentMode.CENTER);
        emptyState.setPadding(true);

        Icon bellIcon = new Icon(VaadinIcon.BELL);
        bellIcon.setSize("48px");
        bellIcon.getStyle().set("color", "#d1d5db");

        H2 emptyTitle = new H2("No notifications");
        emptyTitle.getStyle().set("color", "#9ca3af");

        Paragraph emptyText = new Paragraph(
            "When you get new matches or messages, they'll appear here");
        emptyText.getStyle()
            .set("color", "#9ca3af")
            .set("text-align", "center");

        emptyState.add(bellIcon, emptyTitle, emptyText);
        notificationsList.add(emptyState);
    }
}
