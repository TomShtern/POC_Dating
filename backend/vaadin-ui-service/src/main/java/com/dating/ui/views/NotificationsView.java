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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

        notificationsList = new VerticalLayout();
        notificationsList.setWidth("600px");
        notificationsList.setPadding(false);
        notificationsList.setSpacing(true);

        add(title, notificationsList);
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
                notificationsList.removeAll();
                showEmptyState();
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
        card.getStyle()
            .set("background", "white")
            .set("border-radius", "8px")
            .set("padding", "1rem")
            .set("box-shadow", "0 1px 3px rgba(0,0,0,0.1)")
            .set("border-left", "4px solid #f093fb")
            .set("cursor", "pointer");

        card.addClickListener(e ->
            UI.getCurrent().navigate(UserDetailView.class, match.getId()));

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

        String name = match.getOtherUser().getFirstName();
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
